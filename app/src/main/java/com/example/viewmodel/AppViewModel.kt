package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpiryStatus
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.PaymentMethod
import com.example.data.local.PreferencesStore
import com.example.data.local.PricingTier
import com.example.data.local.ProductEntity
import com.example.data.local.ServerSyncConfig
import com.example.data.remote.CustomServerClientFactory
import com.example.data.repository.HassanAgroRepository
import com.example.util.CsvExporter
import com.example.util.Formatters
import com.example.util.InvoicePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.WebSocket
import java.io.File
import java.util.UUID

enum class InventoryFilter(val label: String) {
    ALL("All"),
    EXPIRING_SOON("Expiring ≤ 30d"),
    EXPIRED("Expired"),
    LOW_STOCK("Low Stock")
}

data class SyncUiState(
    val isSyncing: Boolean = false,
    val isTestingConnection: Boolean = false,
    val wsConnected: Boolean = false,
    val lastStatusText: String = "Offline Ready",
    val lastError: String? = null
)

data class BillingCartLine(
    val product: ProductEntity,
    val quantity: Int = 1,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
}

data class HeldCart(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val draft: BillingDraftState,
    val heldAtMillis: Long = System.currentTimeMillis()
)

data class BillingDraftState(
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val pricingTier: PricingTier = PricingTier.RETAIL,
    val cartLines: List<BillingCartLine> = emptyList(),
    val discountText: String = "",
    val taxEnabled: Boolean = false,
    val taxRateText: String = "0",
    val paymentMethod: String = PaymentMethod.CASH.label,
    val isCreditSale: Boolean = false,
    val amountReceivedText: String = "",
    val notes: String = ""
) {
    val subtotal: Double get() = cartLines.sumOf { it.lineTotal }
    val discountAmount: Double get() = (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val taxableBase: Double get() = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxRatePercent: Double get() = if (taxEnabled) (taxRateText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) else 0.0
    val taxAmount: Double get() = taxableBase * (taxRatePercent / 100.0)
    val grandTotal: Double get() = taxableBase + taxAmount
    val totalItemCount: Int get() = cartLines.sumOf { it.quantity }

    val tenderedAmount: Double
        get() = when {
            isCreditSale && amountReceivedText.isBlank() -> 0.0
            amountReceivedText.isBlank() -> grandTotal
            else -> (amountReceivedText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        }

    val appliedPaidAmount: Double get() = tenderedAmount.coerceAtMost(grandTotal)
    val changeToReturn: Double get() = (tenderedAmount - grandTotal).coerceAtLeast(0.0)
    val balanceDue: Double get() = (grandTotal - appliedPaidAmount).coerceAtLeast(0.0)

    val computedPaymentStatus: InvoicePaymentStatus
        get() = when {
            balanceDue <= 0.01 -> InvoicePaymentStatus.PAID
            appliedPaidAmount > 0.0 -> InvoicePaymentStatus.PARTIAL
            else -> InvoicePaymentStatus.CREDIT
        }
}

data class GeneratedInvoiceEvent(
    val business: BusinessEntity,
    val invoiceWithItems: InvoiceWithItems,
    val pdfFile: File
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = PreferencesStore(application)
    private val repository = HassanAgroRepository(application, db, prefs)

    private var realtimeSocket: WebSocket? = null
    private var periodicSyncJob: Job? = null

    private val defaultBusiness = BusinessEntity(
        id = AppDatabase.DEFAULT_BUSINESS_ID,
        name = "Hassan Agro",
        tagline = "Crop Protection & Agri Inputs",
        invoicePrefix = "HA",
        defaultTaxRate = 0.0
    )

    val businesses: StateFlow<List<BusinessEntity>> = repository.businessesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(defaultBusiness))

    val activeBusiness: StateFlow<BusinessEntity> = combine(
        repository.businessesFlow,
        repository.activeBusinessIdFlow
    ) { list, activeId ->
        list.firstOrNull { it.id == activeId } ?: list.firstOrNull() ?: defaultBusiness
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultBusiness)

    val products: StateFlow<List<ProductEntity>> = activeBusiness
        .flatMapLatest { repository.observeProducts(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<InvoiceWithItems>> = activeBusiness
        .flatMapLatest { repository.observeInvoices(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = activeBusiness
        .flatMapLatest { repository.observeExpenses(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProductsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<InvoiceWithItems>> = repository.allInvoicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpensesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val serverConfig: StateFlow<ServerSyncConfig> = repository.serverConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerSyncConfig())

    private val _syncUiState = MutableStateFlow(SyncUiState())
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    private val _inventoryFilter = MutableStateFlow(InventoryFilter.ALL)
    val inventoryFilter: StateFlow<InventoryFilter> = _inventoryFilter.asStateFlow()

    private val _billingDraft = MutableStateFlow(BillingDraftState())
    val billingDraft: StateFlow<BillingDraftState> = _billingDraft.asStateFlow()

    private val _heldCarts = MutableStateFlow<List<HeldCart>>(emptyList())
    val heldCarts: StateFlow<List<HeldCart>> = _heldCarts.asStateFlow()

    private val _generatedInvoice = MutableStateFlow<GeneratedInvoiceEvent?>(null)
    val generatedInvoice: StateFlow<GeneratedInvoiceEvent?> = _generatedInvoice.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
        // Sync default tax rate into empty billing draft whenever active business changes
        viewModelScope.launch {
            activeBusiness.collect { biz ->
                _billingDraft.update { draft ->
                    if (draft.cartLines.isEmpty()) {
                        draft.copy(
                            taxEnabled = biz.defaultTaxRate > 0.0,
                            taxRateText = if (biz.defaultTaxRate > 0.0) biz.defaultTaxRate.toString() else "0"
                        )
                    } else {
                        draft
                    }
                }
            }
        }
        // Manage optional realtime WebSocket listener + periodic REST polling (for Laravel/shared hosting)
        viewModelScope.launch {
            serverConfig.collect { cfg ->
                reconnectWebSocket(cfg)
                restartPeriodicSync(cfg)
            }
        }
    }

    private fun restartPeriodicSync(cfg: ServerSyncConfig) {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
        if (cfg.autoSyncEnabled && cfg.isConfigured) {
            periodicSyncJob = viewModelScope.launch {
                syncNow(silent = true)
                while (isActive) {
                    delay(30_000L)
                    syncNow(silent = true)
                }
            }
        }
    }

    private fun reconnectWebSocket(cfg: ServerSyncConfig) {
        realtimeSocket?.close(1000, "Reconfiguring")
        realtimeSocket = null
        _syncUiState.update { it.copy(wsConnected = false) }
        if (cfg.autoSyncEnabled && cfg.wsUrl.isNotBlank()) {
            realtimeSocket = CustomServerClientFactory.openRealtimeSyncSocket(
                wsUrl = cfg.wsUrl,
                apiToken = cfg.apiToken,
                deviceId = cfg.deviceId,
                onSyncSignal = { syncNow(silent = true) },
                onStatusChanged = { connected ->
                    _syncUiState.update { it.copy(wsConnected = connected) }
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        periodicSyncJob?.cancel()
        realtimeSocket?.close(1000, "ViewModel cleared")
    }

    fun emitMessage(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    fun setInventoryFilter(filter: InventoryFilter) {
        _inventoryFilter.value = filter
    }

    // --- Business Management ---
    fun switchBusiness(businessId: String) {
        viewModelScope.launch {
            repository.setActiveBusiness(businessId)
            _billingDraft.value = BillingDraftState()
            val name = businesses.value.firstOrNull { it.id == businessId }?.name ?: "Business"
            emitMessage("Switched to $name")
        }
    }

    fun saveBusiness(business: BusinessEntity, switchToIt: Boolean = false) {
        viewModelScope.launch {
            repository.saveBusiness(business)
            if (switchToIt) {
                repository.setActiveBusiness(business.id)
                _billingDraft.value = BillingDraftState()
            }
            emitMessage("Saved business: ${business.name}")
            triggerAutoSyncIfEnabled()
        }
    }

    fun deleteBusiness(businessId: String) {
        viewModelScope.launch {
            val deleted = repository.deleteBusiness(businessId)
            if (deleted) {
                emitMessage("Business removed")
                triggerAutoSyncIfEnabled()
            } else {
                emitMessage("Cannot delete your only business")
            }
        }
    }

    fun updateBusinessLogo(businessId: String, uri: Uri) {
        viewModelScope.launch {
            val savedPath = repository.saveBusinessLogoFromUri(businessId, uri)
            if (savedPath != null) {
                emitMessage("Brand logo updated for invoices")
            } else {
                emitMessage("Could not load selected logo image")
            }
        }
    }

    // --- Inventory Management ---
    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            val bizId = activeBusiness.value.id
            repository.saveProduct(product.copy(businessId = bizId))
            emitMessage("Saved ${product.name}")
            triggerAutoSyncIfEnabled()
        }
    }

    fun restockProduct(
        productId: String,
        addQty: Int,
        newBatchNumber: String,
        newExpiryMillis: Long
    ) {
        viewModelScope.launch {
            repository.restockProduct(productId, addQty, newBatchNumber, newExpiryMillis)
            emitMessage("Added $addQty units to stock")
            triggerAutoSyncIfEnabled()
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            emitMessage("Product removed from inventory")
            triggerAutoSyncIfEnabled()
        }
    }

    suspend fun findProductByBarcode(barcode: String): ProductEntity? {
        return repository.findProductByBarcode(activeBusiness.value.id, barcode)
    }

    // --- Billing & Invoicing ---
    fun updateBillingCustomer(name: String, phone: String, email: String) {
        _billingDraft.update { it.copy(customerName = name, customerPhone = phone, customerEmail = email) }
    }

    fun updateBillingTier(tier: PricingTier) {
        _billingDraft.update { draft ->
            val repriced = draft.cartLines.map { line ->
                line.copy(unitPrice = line.product.priceForTier(tier))
            }
            draft.copy(pricingTier = tier, cartLines = repriced)
        }
    }

    fun updateBillingTax(enabled: Boolean, rateText: String) {
        _billingDraft.update { it.copy(taxEnabled = enabled, taxRateText = rateText) }
    }

    fun updateBillingDiscount(discountText: String) {
        _billingDraft.update { it.copy(discountText = discountText) }
    }

    fun updateBillingNotes(notes: String) {
        _billingDraft.update { it.copy(notes = notes) }
    }

    fun updateBillingPayment(method: String, isCredit: Boolean, receivedText: String) {
        _billingDraft.update {
            it.copy(
                paymentMethod = method,
                isCreditSale = isCredit,
                amountReceivedText = receivedText
            )
        }
    }

    fun holdCurrentCart() {
        val current = _billingDraft.value
        if (current.cartLines.isEmpty()) return
        val label = current.customerName.trim().ifBlank { "Customer #${_heldCarts.value.size + 1}" }
        _heldCarts.update { it + HeldCart(label = label, draft = current) }
        clearBillingDraft()
        emitMessage("Cart parked for $label")
    }

    fun resumeHeldCart(heldId: String) {
        val target = _heldCarts.value.firstOrNull { it.id == heldId } ?: return
        _billingDraft.value = target.draft
        _heldCarts.update { list -> list.filterNot { it.id == heldId } }
        emitMessage("Resumed cart: ${target.label}")
    }

    fun discardHeldCart(heldId: String) {
        _heldCarts.update { list -> list.filterNot { it.id == heldId } }
    }

    fun addProductToCart(product: ProductEntity) {
        if (Formatters.expiryStatus(product.expiryDateMillis) == ExpiryStatus.EXPIRED) {
            emitMessage("Warning: ${product.name} (Batch ${product.batchNumber}) is EXPIRED!")
        }
        _billingDraft.update { draft ->
            val existingIdx = draft.cartLines.indexOfFirst { it.product.id == product.id }
            val updated = draft.cartLines.toMutableList()
            if (existingIdx >= 0) {
                val existing = updated[existingIdx]
                if (existing.quantity < product.stockQty || product.stockQty <= 0) {
                    updated[existingIdx] = existing.copy(quantity = existing.quantity + 1)
                } else {
                    emitMessage("Only ${product.stockQty} units available in stock")
                }
            } else {
                val unitPrice = product.priceForTier(draft.pricingTier)
                updated.add(BillingCartLine(product = product, quantity = 1, unitPrice = unitPrice))
            }
            draft.copy(cartLines = updated)
        }
    }

    fun updateCartLineQuantity(productId: String, newQty: Int) {
        _billingDraft.update { draft ->
            val updated = draft.cartLines.mapNotNull { line ->
                if (line.product.id == productId) {
                    if (newQty <= 0) null else line.copy(quantity = newQty)
                } else {
                    line
                }
            }
            draft.copy(cartLines = updated)
        }
    }

    fun updateCartLineUnitPrice(productId: String, newPrice: Double) {
        _billingDraft.update { draft ->
            val updated = draft.cartLines.map { line ->
                if (line.product.id == productId) line.copy(unitPrice = newPrice.coerceAtLeast(0.0))
                else line
            }
            draft.copy(cartLines = updated)
        }
    }

    fun clearBillingDraft() {
        val defaultTax = activeBusiness.value.defaultTaxRate
        _billingDraft.value = BillingDraftState(
            taxEnabled = defaultTax > 0.0,
            taxRateText = if (defaultTax > 0.0) defaultTax.toString() else "0"
        )
    }

    fun finalizeInvoiceAndGeneratePdf() {
        val draft = _billingDraft.value
        if (draft.cartLines.isEmpty()) {
            emitMessage("Add at least one pesticide product to complete a POS sale")
            return
        }
        viewModelScope.launch {
            val business = activeBusiness.value
            val invoiceNumber = repository.nextInvoiceNumber(business)
            val invoiceId = UUID.randomUUID().toString()
            val invoiceEntity = InvoiceEntity(
                id = invoiceId,
                businessId = business.id,
                invoiceNumber = invoiceNumber,
                customerName = draft.customerName.trim().ifBlank { "Walk-in Customer" },
                customerPhone = draft.customerPhone.trim(),
                customerEmail = draft.customerEmail.trim(),
                pricingTier = draft.pricingTier.name,
                subtotal = draft.subtotal,
                discountAmount = draft.discountAmount,
                taxEnabled = draft.taxEnabled,
                taxRatePercent = draft.taxRatePercent,
                taxAmount = draft.taxAmount,
                grandTotal = draft.grandTotal,
                paymentMethod = if (draft.isCreditSale) "Credit (Udhaar)" else draft.paymentMethod,
                paymentStatus = draft.computedPaymentStatus.name,
                amountPaid = draft.appliedPaidAmount,
                balanceDue = draft.balanceDue,
                isReturned = false,
                notes = draft.notes.trim()
            )
            val items = draft.cartLines.map { line ->
                InvoiceItemEntity(
                    invoiceId = invoiceId,
                    productId = line.product.id,
                    productName = line.product.name,
                    packSize = line.product.packSize,
                    batchNumber = line.product.batchNumber,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    lineTotal = line.lineTotal
                )
            }
            val saved = repository.createInvoiceAndDeductStock(invoiceEntity, items)
            val pdfFile = withContext(Dispatchers.IO) {
                InvoicePdfGenerator.generateInvoicePdf(getApplication(), business, saved)
            }
            clearBillingDraft()
            _generatedInvoice.value = GeneratedInvoiceEvent(business, saved, pdfFile)
            emitMessage("POS Sale $invoiceNumber completed & stock deducted")
            triggerAutoSyncIfEnabled()
        }
    }

    fun recordInvoicePayment(invoiceId: String, amount: Double) {
        viewModelScope.launch {
            val updated = repository.recordInvoicePayment(invoiceId, amount)
            if (updated != null) {
                emitMessage("Collected ${Formatters.formatPkr(amount)} on ${updated.invoice.invoiceNumber}")
                triggerAutoSyncIfEnabled()
            }
        }
    }

    fun returnInvoiceAndRestock(invoiceId: String) {
        viewModelScope.launch {
            val ok = repository.returnInvoiceAndRestock(invoiceId)
            if (ok) {
                emitMessage("Sale returned & inventory stock restored")
                triggerAutoSyncIfEnabled()
            }
        }
    }

    fun openExistingInvoicePdf(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val biz = businesses.value.firstOrNull { it.id == invoiceWithItems.invoice.businessId }
                ?: activeBusiness.value
            val pdfFile = withContext(Dispatchers.IO) {
                InvoicePdfGenerator.generateInvoicePdf(getApplication(), biz, invoiceWithItems)
            }
            _generatedInvoice.value = GeneratedInvoiceEvent(biz, invoiceWithItems, pdfFile)
        }
    }

    fun dismissGeneratedInvoice() {
        _generatedInvoice.value = null
    }

    // --- Expenses ---
    fun saveExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            val bizId = activeBusiness.value.id
            repository.saveExpense(expense.copy(businessId = bizId))
            emitMessage("Expense saved")
            triggerAutoSyncIfEnabled()
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            emitMessage("Expense deleted")
            triggerAutoSyncIfEnabled()
        }
    }

    // --- Reports & Exports ---
    suspend fun generateMonthlyReportPdf(
        year: Int,
        monthZeroBased: Int,
        allBusinessesScope: Boolean
    ): File = withContext(Dispatchers.IO) {
        val (startMs, endMs) = Formatters.monthBoundsMillis(year, monthZeroBased)
        val biz = activeBusiness.value
        val scopeInvoices = (if (allBusinessesScope) allInvoices.value else invoices.value)
            .filter { !it.invoice.isReturned && it.invoice.createdAt in startMs until endMs }
        val scopeExpenses = (if (allBusinessesScope) allExpenses.value else expenses.value)
            .filter { it.expenseDateMillis in startMs until endMs }
        val scopeProducts = if (allBusinessesScope) allProducts.value else products.value

        val grossSales = scopeInvoices.sumOf { it.invoice.grandTotal }
        val totalExpenses = scopeExpenses.sumOf { it.amount }
        val taxCollected = scopeInvoices.sumOf { it.invoice.taxAmount }
        val stockValue = scopeProducts.sumOf { it.stockQty * it.purchasePrice }
        val expiringCount = scopeProducts.count { Formatters.expiryStatus(it.expiryDateMillis) == ExpiryStatus.EXPIRING_SOON }
        val expiredCount = scopeProducts.count { Formatters.expiryStatus(it.expiryDateMillis) == ExpiryStatus.EXPIRED }

        val scopeTitle = if (allBusinessesScope) "${biz.name} (All Businesses Consolidated)" else biz.name
        InvoicePdfGenerator.generateMonthlyReportPdf(
            context = getApplication(),
            business = biz,
            scopeTitle = scopeTitle,
            monthLabel = Formatters.formatMonthYear(year, monthZeroBased),
            grossSales = grossSales,
            totalExpenses = totalExpenses,
            netProfit = grossSales - totalExpenses,
            taxCollected = taxCollected,
            stockValuation = stockValue,
            invoiceCount = scopeInvoices.size,
            expiringCount = expiringCount,
            expiredCount = expiredCount,
            expenses = scopeExpenses
        )
    }

    suspend fun exportCsvToCache(
        type: String,
        year: Int,
        monthZeroBased: Int,
        allBusinessesScope: Boolean
    ): Pair<File, String> = withContext(Dispatchers.IO) {
        val (startMs, endMs) = Formatters.monthBoundsMillis(year, monthZeroBased)
        val bizTag = if (allBusinessesScope) "AllBusinesses" else activeBusiness.value.name.replace(Regex("[^A-Za-z0-9]"), "_")
        val monthTag = Formatters.formatMonthYear(year, monthZeroBased).replace(" ", "_")

        val (fileName, csv) = when (type) {
            "inventory" -> {
                val list = if (allBusinessesScope) allProducts.value else products.value
                "${bizTag}_Inventory.csv" to CsvExporter.buildInventoryCsv(list)
            }
            "expenses" -> {
                val list = (if (allBusinessesScope) allExpenses.value else expenses.value)
                    .filter { it.expenseDateMillis in startMs until endMs }
                "${bizTag}_Expenses_${monthTag}.csv" to CsvExporter.buildExpensesCsv(list)
            }
            else -> {
                val list = (if (allBusinessesScope) allInvoices.value else invoices.value)
                    .filter { it.invoice.createdAt in startMs until endMs }
                "${bizTag}_Sales_${monthTag}.csv" to CsvExporter.buildSalesCsv(list)
            }
        }
        val file = CsvExporter.writeCsvToCache(getApplication(), fileName, csv)
        file to csv
    }

    // --- Custom Server Sync ---
    fun saveServerConfig(
        baseUrl: String,
        apiToken: String,
        wsUrl: String,
        autoSyncEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.saveServerConfig(baseUrl, apiToken, wsUrl, autoSyncEnabled)
            emitMessage("Server settings saved")
        }
    }

    fun testServerConnection(baseUrl: String, apiToken: String) {
        viewModelScope.launch {
            _syncUiState.update { it.copy(isTestingConnection = true, lastError = null) }
            val result = repository.testServerConnection(baseUrl, apiToken)
            result.onSuccess { msg ->
                _syncUiState.update { it.copy(isTestingConnection = false, lastStatusText = msg) }
                emitMessage("Server reachable: $msg")
            }.onFailure { err ->
                val errMsg = err.localizedMessage ?: "Connection failed"
                _syncUiState.update { it.copy(isTestingConnection = false, lastError = errMsg) }
                emitMessage("Connection error: $errMsg")
            }
        }
    }

    fun syncNow(silent: Boolean = false) {
        if (_syncUiState.value.isSyncing) return
        viewModelScope.launch {
            _syncUiState.update { it.copy(isSyncing = true, lastError = null) }
            val result = repository.syncWithCustomServer()
            _syncUiState.update {
                it.copy(
                    isSyncing = false,
                    lastStatusText = if (result.success) "Synced" else "Offline Ready",
                    lastError = if (result.success) null else result.message
                )
            }
            if (!silent) {
                emitMessage(result.message)
            }
        }
    }

    private fun triggerAutoSyncIfEnabled() {
        val cfg = serverConfig.value
        if (cfg.autoSyncEnabled && cfg.isConfigured) {
            syncNow(silent = true)
        }
    }
}
