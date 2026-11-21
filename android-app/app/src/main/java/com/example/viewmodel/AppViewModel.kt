package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AuthSession
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpiryStatus
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.MonthlyTotalRow
import com.example.data.local.PaymentMethod
import com.example.data.local.PreferencesStore
import com.example.data.local.PricingTier
import com.example.data.local.ExpenseCategory
import com.example.data.local.ProductEntity
import com.example.data.local.ServerSyncConfig
import com.example.data.local.ThemeAppearanceMode
import com.example.data.remote.CustomServerClientFactory
import com.example.data.remote.TeamPermissionItemDto
import com.example.data.remote.TeamRoleDto
import com.example.data.remote.TeamUserDto
import com.example.data.repository.HassanAgroRepository
import com.example.util.CsvExporter
import com.example.util.DashboardPeriod
import com.example.util.Formatters
import com.example.util.InvoicePdfGenerator
import com.example.util.InvoicePdfStyle
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

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

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
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val saveCustomerToDirectory: Boolean = false,
    val pricingTier: PricingTier = PricingTier.RETAIL,
    val cartLines: List<BillingCartLine> = emptyList(),
    val discountText: String = "",
    val taxEnabled: Boolean = false,
    val taxRateText: String = "0",
    val paymentMethod: String = PaymentMethod.CASH.label,
    val isCreditSale: Boolean = false,
    val amountReceivedText: String = "",
    val notes: String = "",
    val proofImagePath: String? = null
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
    val pdfFile: File,
    val style: InvoicePdfStyle = InvoicePdfStyle.THERMAL_COUPON
)

data class TeamUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val businessId: String = "",
    val businessName: String = "",
    val roles: List<TeamRoleDto> = emptyList(),
    val users: List<TeamUserDto> = emptyList(),
    val permissionsCatalog: List<TeamPermissionItemDto> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = PreferencesStore(application)
    private val repository = HassanAgroRepository(application, db, prefs)

    private var realtimeSocket: WebSocket? = null
    private var periodicSyncJob: Job? = null

    private val fallbackBusiness = BusinessEntity(
        id = AppDatabase.DEFAULT_BUSINESS_ID,
        name = "My Business",
        tagline = "Retail & Wholesale Management",
        invoicePrefix = "BM",
        themeKey = "OCEAN",
        businessType = "GENERAL",
        defaultTaxRate = 0.0
    )

    // Nullable until DataStore emits the real persisted session — prevents Login screen flash on startup!
    val authSessionLoaded: StateFlow<AuthSession?> = repository.authSessionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val authSession: StateFlow<AuthSession> = repository.authSessionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthSession())

    val appearanceMode: StateFlow<ThemeAppearanceMode> = repository.appearanceModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeAppearanceMode.SYSTEM)

    private val _dashboardPeriod = MutableStateFlow(DashboardPeriod.THIS_MONTH)
    val dashboardPeriod: StateFlow<DashboardPeriod> = _dashboardPeriod.asStateFlow()

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    fun setAppearanceMode(mode: ThemeAppearanceMode) {
        viewModelScope.launch {
            repository.setAppearanceMode(mode)
        }
    }

    fun setDashboardPeriod(period: DashboardPeriod) {
        _dashboardPeriod.value = period
    }

    suspend fun saveProofImage(prefix: String, uri: Uri): String? {
        return repository.saveProofImageFromUri(prefix, uri)
    }

    // Strictly scope visible businesses to the authenticated admin's allowedBusinessIds
    val businesses: StateFlow<List<BusinessEntity>> = combine(
        repository.businessesFlow,
        authSession
    ) { all, session ->
        if (!session.isLoggedIn) emptyList()
        else all.filter { session.canAccessBusiness(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBusiness: StateFlow<BusinessEntity> = combine(
        businesses,
        repository.activeBusinessIdFlow
    ) { list, activeId ->
        list.firstOrNull { it.id == activeId } ?: list.firstOrNull() ?: fallbackBusiness
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), fallbackBusiness)

    val customers: StateFlow<List<CustomerEntity>> = activeBusiness
        .flatMapLatest { repository.observeCustomers(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = activeBusiness
        .flatMapLatest { repository.observeProducts(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<InvoiceWithItems>> = activeBusiness
        .flatMapLatest { repository.observeInvoices(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = activeBusiness
        .flatMapLatest { repository.observeExpenses(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = combine(
        repository.allProductsFlow,
        authSession
    ) { list, session -> list.filter { session.canAccessBusiness(it.businessId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<InvoiceWithItems>> = combine(
        repository.allInvoicesFlow,
        authSession
    ) { list, session -> list.filter { session.canAccessBusiness(it.invoice.businessId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = combine(
        repository.allExpensesFlow,
        authSession
    ) { list, session -> list.filter { session.canAccessBusiness(it.businessId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Room SQL-aggregated monthly income & expense series for the Reports growth charts
    private val reportSinceMillis: Long = Formatters.sixMonthsAgoMillis()

    val activeMonthlySalesTotals: StateFlow<List<MonthlyTotalRow>> = activeBusiness
        .flatMapLatest { repository.observeMonthlySalesTotals(it.id, reportSinceMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMonthlyExpenseTotals: StateFlow<List<MonthlyTotalRow>> = activeBusiness
        .flatMapLatest { repository.observeMonthlyExpenseTotals(it.id, reportSinceMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMonthlySalesTotals: StateFlow<List<MonthlyTotalRow>> = authSession
        .flatMapLatest { session -> repository.observeAllMonthlySalesTotals(session.allowedBusinessIds, reportSinceMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMonthlyExpenseTotals: StateFlow<List<MonthlyTotalRow>> = authSession
        .flatMapLatest { session -> repository.observeAllMonthlyExpenseTotals(session.allowedBusinessIds, reportSinceMillis) }
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

    private val _teamState = MutableStateFlow(TeamUiState())
    val teamState: StateFlow<TeamUiState> = _teamState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            repository.refreshSession()
        }
        viewModelScope.launch {
            activeBusiness.collect { biz ->
                val defaultMethod = biz.paymentMethods().firstOrNull() ?: PaymentMethod.CASH.label
                _billingDraft.update { draft ->
                    if (draft.cartLines.isEmpty()) {
                        draft.copy(
                            taxEnabled = biz.defaultTaxRate > 0.0,
                            taxRateText = if (biz.defaultTaxRate > 0.0) biz.defaultTaxRate.toString() else "0",
                            paymentMethod = defaultMethod
                        )
                    } else {
                        draft
                    }
                }
                if (!biz.trackExpiry && (_inventoryFilter.value == InventoryFilter.EXPIRED || _inventoryFilter.value == InventoryFilter.EXPIRING_SOON)) {
                    _inventoryFilter.value = InventoryFilter.ALL
                }
            }
        }
        viewModelScope.launch {
            combine(serverConfig, authSession) { cfg, session -> cfg to session }
                .collect { (cfg, session) ->
                    if (session.isLoggedIn) {
                        reconnectWebSocket(cfg, session.authToken)
                        restartPeriodicSync(cfg)
                    } else {
                        periodicSyncJob?.cancel()
                        periodicSyncJob = null
                        realtimeSocket?.close(1000, "Logged out")
                        realtimeSocket = null
                    }
                }
        }
    }

    // --- Authentication ---
    fun clearAuthError() {
        _authUiState.update { it.copy(errorMessage = null) }
    }

    fun login(serverUrl: String, email: String, password: String) {
        if (_authUiState.value.isLoading) return
        viewModelScope.launch {
            _authUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.login(serverUrl, email, password)
            _authUiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = if (result.success) null else result.message
                )
            }
            if (result.success) {
                emitMessage(result.message)
            }
        }
    }

    fun register(
        serverUrl: String,
        name: String,
        email: String,
        password: String,
        businessName: String,
        businessType: String,
        themeKey: String,
        currencySymbol: String
    ) {
        if (_authUiState.value.isLoading) return
        viewModelScope.launch {
            _authUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.register(
                serverBaseUrl = serverUrl,
                name = name,
                email = email,
                password = password,
                businessName = businessName,
                businessType = businessType,
                themeKey = themeKey,
                currencySymbol = currencySymbol
            )
            _authUiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = if (result.success) null else result.message
                )
            }
            if (result.success) {
                emitMessage(result.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _billingDraft.value = BillingDraftState()
            _heldCarts.value = emptyList()
            _generatedInvoice.value = null
            _authUiState.value = AuthUiState()
        }
    }

    private fun restartPeriodicSync(cfg: ServerSyncConfig) {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
        if (cfg.autoSyncEnabled && cfg.isConfigured && authSession.value.isLoggedIn) {
            periodicSyncJob = viewModelScope.launch {
                syncNow(silent = true)
                while (isActive) {
                    delay(30_000L)
                    syncNow(silent = true)
                }
            }
        }
    }

    private fun reconnectWebSocket(cfg: ServerSyncConfig, authToken: String) {
        realtimeSocket?.close(1000, "Reconfiguring")
        realtimeSocket = null
        _syncUiState.update { it.copy(wsConnected = false) }
        if (cfg.autoSyncEnabled && cfg.wsUrl.isNotBlank()) {
            realtimeSocket = CustomServerClientFactory.openRealtimeSyncSocket(
                wsUrl = cfg.wsUrl,
                apiToken = authToken.ifBlank { cfg.apiToken },
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
    fun saveProduct(
        product: ProductEntity,
        recordStockPurchaseExpense: Boolean = false,
        purchasePaymentMethod: String = PaymentMethod.CASH.label
    ) {
        viewModelScope.launch {
            val business = activeBusiness.value
            val actor = authSession.value
            val previousProduct = repository.getProductById(product.id)
            val addedQty = if (previousProduct == null) {
                product.stockQty.coerceAtLeast(0)
            } else {
                (product.stockQty - previousProduct.stockQty).coerceAtLeast(0)
            }
            val enriched = if (product.purchasePrice > 0.0 && (
                    product.distributorPrice <= 0.0 ||
                    product.wholesalerPrice <= 0.0 ||
                    product.retailPrice <= 0.0 ||
                    product.mrpPrice <= 0.0
                )
            ) {
                val rules = business.pricingRules()
                val suggested = com.example.data.local.calculateSuggestedTierPrices(
                    purchasePrice = product.purchasePrice,
                    taxRatePercent = 0.0,
                    taxIncludedInCost = true,
                    marginMode = rules.ownerMarginMode,
                    marginValue = rules.ownerMarginValue,
                    rules = rules
                )
                product.copy(
                    businessId = business.id,
                    distributorPrice = if (product.distributorPrice > 0.0) product.distributorPrice else suggested.distributorPrice,
                    wholesalerPrice = if (product.wholesalerPrice > 0.0) product.wholesalerPrice else suggested.wholesalerPrice,
                    retailPrice = if (product.retailPrice > 0.0) product.retailPrice else suggested.retailPrice,
                    mrpPrice = if (product.mrpPrice > 0.0) product.mrpPrice else suggested.mrpPrice
                )
            } else {
                product.copy(businessId = business.id)
            }
            repository.saveProduct(
                product = enriched,
                performedById = actor.userId,
                performedByName = actor.userName
            )
            val totalPurchaseCost = addedQty * enriched.purchasePrice
            if (recordStockPurchaseExpense && totalPurchaseCost > 0.0) {
                val currency = business.resolvedCurrency()
                val stockExpense = ExpenseEntity(
                    businessId = business.id,
                    title = "Stock Purchase: ${enriched.name}",
                    category = ExpenseCategory.STOCK_PURCHASE.label,
                    amount = totalPurchaseCost,
                    paymentMethod = purchasePaymentMethod.ifBlank { PaymentMethod.CASH.label },
                    payee = enriched.brand.trim(),
                    notes = "$addedQty units × ${Formatters.formatPkr(enriched.purchasePrice, currency)}",
                    expenseDateMillis = System.currentTimeMillis()
                )
                repository.saveExpense(stockExpense, actor.userId, actor.userName)
                emitMessage("Saved ${enriched.name} & logged ${Formatters.formatPkr(totalPurchaseCost, currency)} stock purchase")
            } else {
                emitMessage("Saved ${enriched.name}")
            }
            triggerAutoSyncIfEnabled()
        }
    }

    fun restockProduct(
        productId: String,
        addQty: Int,
        newBatchNumber: String,
        newExpiryMillis: Long,
        updatedPurchasePrice: Double? = null,
        recordStockPurchaseExpense: Boolean = false,
        purchasePaymentMethod: String = PaymentMethod.CASH.label
    ) {
        viewModelScope.launch {
            val business = activeBusiness.value
            val actor = authSession.value
            val updated = repository.restockProduct(
                productId = productId,
                addQty = addQty,
                newBatchNumber = newBatchNumber,
                newExpiryMillis = newExpiryMillis,
                performedById = actor.userId,
                performedByName = actor.userName,
                updatedPurchasePrice = updatedPurchasePrice
            )
            val unitCost = updatedPurchasePrice?.takeIf { it > 0.0 } ?: updated?.purchasePrice ?: 0.0
            val totalPurchaseCost = addQty.coerceAtLeast(0) * unitCost
            if (recordStockPurchaseExpense && updated != null && totalPurchaseCost > 0.0) {
                val currency = business.resolvedCurrency()
                val stockExpense = ExpenseEntity(
                    businessId = business.id,
                    title = "Restock: ${updated.name}",
                    category = ExpenseCategory.STOCK_PURCHASE.label,
                    amount = totalPurchaseCost,
                    paymentMethod = purchasePaymentMethod.ifBlank { PaymentMethod.CASH.label },
                    payee = updated.brand.trim(),
                    notes = "+$addQty units × ${Formatters.formatPkr(unitCost, currency)}",
                    expenseDateMillis = System.currentTimeMillis()
                )
                repository.saveExpense(stockExpense, actor.userId, actor.userName)
                emitMessage("Restocked +$addQty units & logged ${Formatters.formatPkr(totalPurchaseCost, currency)}")
            } else {
                emitMessage("Added $addQty units to stock")
            }
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

    // --- Customers Directory & Credit Ledger ---
    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            val bizId = activeBusiness.value.id
            val actor = authSession.value
            repository.saveCustomer(
                customer = customer.copy(businessId = bizId),
                performedById = actor.userId,
                performedByName = actor.userName
            )
            emitMessage("Saved customer: ${customer.name}")
            triggerAutoSyncIfEnabled()
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            emitMessage("Customer removed")
            triggerAutoSyncIfEnabled()
        }
    }

    // --- Billing & Invoicing ---
    fun selectBillingCustomer(customer: CustomerEntity?) {
        if (customer == null) {
            _billingDraft.update {
                it.copy(
                    customerId = "",
                    customerName = "",
                    customerPhone = "",
                    customerEmail = "",
                    saveCustomerToDirectory = false
                )
            }
            return
        }
        val preferredTier = PricingTier.fromName(customer.pricingTier)
        _billingDraft.update { draft ->
            val repriced = draft.cartLines.map { line ->
                line.copy(unitPrice = line.product.priceForTier(preferredTier))
            }
            draft.copy(
                customerId = customer.id,
                customerName = customer.name,
                customerPhone = customer.phone,
                customerEmail = customer.email,
                saveCustomerToDirectory = false,
                pricingTier = preferredTier,
                cartLines = repriced
            )
        }
    }

    fun updateBillingSaveCustomer(save: Boolean) {
        _billingDraft.update { it.copy(saveCustomerToDirectory = save) }
    }

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
        if (activeBusiness.value.trackExpiry && Formatters.expiryStatus(product.expiryDateMillis) == ExpiryStatus.EXPIRED) {
            emitMessage("Warning: ${product.name} is EXPIRED!")
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
        val biz = activeBusiness.value
        val defaultTax = biz.defaultTaxRate
        val defaultMethod = biz.paymentMethods().firstOrNull() ?: PaymentMethod.CASH.label
        _billingDraft.value = BillingDraftState(
            taxEnabled = defaultTax > 0.0,
            taxRateText = if (defaultTax > 0.0) defaultTax.toString() else "0",
            paymentMethod = defaultMethod
        )
    }

    fun attachBillingProofImage(uri: Uri) {
        viewModelScope.launch {
            val savedPath = repository.saveProofImageFromUri("pos_proof", uri)
            if (savedPath != null) {
                _billingDraft.update { it.copy(proofImagePath = savedPath) }
                emitMessage("Payment proof image attached")
            } else {
                emitMessage("Could not attach image")
            }
        }
    }

    fun clearBillingProofImage() {
        _billingDraft.update { it.copy(proofImagePath = null) }
    }

    fun finalizeInvoiceAndGeneratePdf() {
        val draft = _billingDraft.value
        if (draft.cartLines.isEmpty()) {
            emitMessage("Add at least one product to complete a POS sale")
            return
        }
        viewModelScope.launch {
            val business = activeBusiness.value
            val actor = authSession.value
            val cleanName = draft.customerName.trim()
            var resolvedCustomerId = draft.customerId

            // Auto-save walk-in customer to directory when toggled on
            if (resolvedCustomerId.isBlank() && draft.saveCustomerToDirectory && cleanName.isNotBlank()) {
                val newCustomer = CustomerEntity(
                    businessId = business.id,
                    name = cleanName,
                    phone = draft.customerPhone.trim(),
                    email = draft.customerEmail.trim(),
                    pricingTier = draft.pricingTier.name
                )
                repository.saveCustomer(newCustomer, actor.userId, actor.userName)
                resolvedCustomerId = newCustomer.id
            }

            val invoiceNumber = repository.nextInvoiceNumber(business)
            val invoiceId = UUID.randomUUID().toString()
            val invoiceEntity = InvoiceEntity(
                id = invoiceId,
                businessId = business.id,
                invoiceNumber = invoiceNumber,
                customerId = resolvedCustomerId,
                customerName = cleanName.ifBlank { "Walk-in Customer" },
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
                notes = draft.notes.trim(),
                proofImagePath = draft.proofImagePath,
                performedById = actor.userId,
                performedByName = actor.userName
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
                    costPrice = line.product.purchasePrice,
                    lineTotal = line.lineTotal
                )
            }
            val saved = repository.createInvoiceAndDeductStock(
                invoice = invoiceEntity,
                items = items,
                performedById = actor.userId,
                performedByName = actor.userName
            )
            val defaultStyle = InvoicePdfStyle.THERMAL_COUPON
            val pdfFile = withContext(Dispatchers.IO) {
                InvoicePdfGenerator.generateInvoicePdf(getApplication(), business, saved, defaultStyle)
            }
            clearBillingDraft()
            _generatedInvoice.value = GeneratedInvoiceEvent(business, saved, pdfFile, defaultStyle)
            emitMessage("POS Sale $invoiceNumber completed by ${actor.userName.ifBlank { "Admin" }}")
            triggerAutoSyncIfEnabled()
        }
    }

    fun recordInvoicePayment(invoiceId: String, amount: Double) {
        viewModelScope.launch {
            val actor = authSession.value
            val currency = activeBusiness.value.resolvedCurrency()
            val updated = repository.recordInvoicePayment(invoiceId, amount, actor.userId, actor.userName)
            if (updated != null) {
                emitMessage("Collected ${Formatters.formatPkr(amount, currency)} on ${updated.invoice.invoiceNumber}")
                triggerAutoSyncIfEnabled()
            }
        }
    }

    fun returnInvoiceAndRestock(invoiceId: String) {
        viewModelScope.launch {
            val actor = authSession.value
            val ok = repository.returnInvoiceAndRestock(invoiceId, actor.userId, actor.userName)
            if (ok) {
                emitMessage("Sale returned & inventory stock restored")
                triggerAutoSyncIfEnabled()
            }
        }
    }

    fun openExistingInvoicePdf(
        invoiceWithItems: InvoiceWithItems,
        style: InvoicePdfStyle = InvoicePdfStyle.THERMAL_COUPON
    ) {
        viewModelScope.launch {
            val biz = businesses.value.firstOrNull { it.id == invoiceWithItems.invoice.businessId }
                ?: activeBusiness.value
            val pdfFile = withContext(Dispatchers.IO) {
                InvoicePdfGenerator.generateInvoicePdf(getApplication(), biz, invoiceWithItems, style)
            }
            _generatedInvoice.value = GeneratedInvoiceEvent(biz, invoiceWithItems, pdfFile, style)
        }
    }

    fun switchGeneratedInvoiceStyle(style: InvoicePdfStyle) {
        val current = _generatedInvoice.value ?: return
        if (current.style == style) return
        viewModelScope.launch {
            val pdfFile = withContext(Dispatchers.IO) {
                InvoicePdfGenerator.generateInvoicePdf(getApplication(), current.business, current.invoiceWithItems, style)
            }
            _generatedInvoice.value = current.copy(pdfFile = pdfFile, style = style)
        }
    }

    fun dismissGeneratedInvoice() {
        _generatedInvoice.value = null
    }

    // --- Business Team & Custom Roles (Business Owner In-App Management) ---
    fun loadTeamOverview() {
        val bizId = activeBusiness.value.id
        if (bizId.isBlank()) return
        viewModelScope.launch {
            _teamState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.fetchTeamOverview(bizId).onSuccess { resp ->
                _teamState.value = TeamUiState(
                    isLoading = false,
                    businessId = resp.businessId,
                    businessName = resp.businessName,
                    roles = resp.roles,
                    users = resp.users,
                    permissionsCatalog = resp.permissionsCatalog
                )
            }.onFailure { err ->
                _teamState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage ?: "Unable to load team") }
            }
        }
    }

    fun saveTeamRole(
        roleId: String?,
        name: String,
        description: String,
        permissions: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        val bizId = activeBusiness.value.id
        viewModelScope.launch {
            _teamState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.saveTeamRole(roleId, bizId, name, description, permissions).onSuccess { msg ->
                _teamState.update { it.copy(isSaving = false) }
                emitMessage(msg)
                onSuccess()
                loadTeamOverview()
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Could not save role"
                _teamState.update { it.copy(isSaving = false, errorMessage = msg) }
                emitMessage(msg)
            }
        }
    }

    fun deleteTeamRole(roleId: String) {
        val bizId = activeBusiness.value.id
        viewModelScope.launch {
            repository.deleteTeamRole(roleId, bizId).onSuccess { msg ->
                emitMessage(msg)
                loadTeamOverview()
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Could not delete role")
            }
        }
    }

    fun saveTeamUser(
        userId: String?,
        name: String,
        email: String,
        password: String,
        roleId: String,
        customRoleName: String = "",
        customPermissions: List<String>? = null,
        onSuccess: () -> Unit = {}
    ) {
        val bizId = activeBusiness.value.id
        viewModelScope.launch {
            _teamState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.saveTeamUser(
                userId = userId,
                businessId = bizId,
                name = name,
                email = email,
                password = password,
                roleId = roleId,
                customRoleName = customRoleName,
                customPermissions = customPermissions
            ).onSuccess { msg ->
                _teamState.update { it.copy(isSaving = false) }
                emitMessage(msg)
                onSuccess()
                loadTeamOverview()
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Could not save team member"
                _teamState.update { it.copy(isSaving = false, errorMessage = msg) }
                emitMessage(msg)
            }
        }
    }

    fun toggleTeamUser(userId: String) {
        val bizId = activeBusiness.value.id
        viewModelScope.launch {
            repository.toggleTeamUser(userId, bizId).onSuccess { msg ->
                emitMessage(msg)
                loadTeamOverview()
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to update user status")
            }
        }
    }

    fun deleteTeamUser(userId: String) {
        val bizId = activeBusiness.value.id
        viewModelScope.launch {
            repository.deleteTeamUser(userId, bizId).onSuccess { msg ->
                emitMessage(msg)
                loadTeamOverview()
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to remove user")
            }
        }
    }

    // --- Expenses ---
    fun saveExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            val bizId = activeBusiness.value.id
            val actor = authSession.value
            repository.saveExpense(
                expense = expense.copy(businessId = bizId),
                performedById = actor.userId,
                performedByName = actor.userName
            )
            emitMessage("Expense saved by ${actor.userName.ifBlank { "Admin" }}")
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

        val pl = com.example.util.ProfitLossCalculator.calculate(
            products = scopeProducts,
            invoices = scopeInvoices,
            expenses = scopeExpenses
        )
        val taxCollected = scopeInvoices.sumOf { it.invoice.taxAmount }
        val expiringCount = scopeProducts.count { Formatters.expiryStatus(it.expiryDateMillis) == ExpiryStatus.EXPIRING_SOON }
        val expiredCount = scopeProducts.count { Formatters.expiryStatus(it.expiryDateMillis) == ExpiryStatus.EXPIRED }

        val scopeTitle = if (allBusinessesScope) "${biz.name} (All Businesses Consolidated)" else biz.name
        InvoicePdfGenerator.generateMonthlyReportPdf(
            context = getApplication(),
            business = biz,
            scopeTitle = scopeTitle,
            monthLabel = Formatters.formatMonthYear(year, monthZeroBased),
            grossSales = pl.totalSales,
            totalExpenses = pl.operatingExpenses,
            netProfit = pl.netProfit,
            taxCollected = taxCollected,
            stockValuation = pl.inventoryCostValuation,
            invoiceCount = scopeInvoices.size,
            expiringCount = expiringCount,
            expiredCount = expiredCount,
            expenses = scopeExpenses,
            costOfGoodsSold = pl.costOfGoodsSold,
            grossProfit = pl.grossProfit,
            stockPurchaseSpend = pl.stockPurchaseSpend,
            inventoryRetailValue = pl.inventoryRetailValuation
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

    fun saveAndSyncServerConfig(
        baseUrl: String,
        apiToken: String,
        wsUrl: String,
        autoSyncEnabled: Boolean
    ) {
        if (_syncUiState.value.isSyncing) return
        viewModelScope.launch {
            repository.saveServerConfig(baseUrl, apiToken, wsUrl, autoSyncEnabled)
            syncNow(silent = false)
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
            if (!silent || result.unauthorized) {
                emitMessage(result.message)
            }
        }
    }

    private fun triggerAutoSyncIfEnabled() {
        val cfg = serverConfig.value
        if (cfg.autoSyncEnabled && cfg.isConfigured && authSession.value.isLoggedIn) {
            syncNow(silent = true)
        }
    }
}
