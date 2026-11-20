package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.PreferencesStore
import com.example.data.local.ProductEntity
import com.example.data.local.ServerSyncConfig
import com.example.data.local.SyncStatus
import com.example.data.remote.CustomServerClientFactory
import com.example.data.remote.SyncPushRequest
import com.example.data.remote.toDto
import com.example.data.remote.toEntity
import com.example.data.remote.toEntityPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SyncResult(
    val success: Boolean,
    val message: String,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0
)

class HassanAgroRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: PreferencesStore
) {
    private val businessDao = db.businessDao()
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()
    private val expenseDao = db.expenseDao()

    val businessesFlow: Flow<List<BusinessEntity>> = businessDao.observeBusinesses()
    val activeBusinessIdFlow: Flow<String> = prefs.activeBusinessIdFlow
    val serverConfigFlow: Flow<ServerSyncConfig> = prefs.serverConfigFlow

    val allProductsFlow: Flow<List<ProductEntity>> = productDao.observeAllActive()
    val allInvoicesFlow: Flow<List<InvoiceWithItems>> = invoiceDao.observeAllActive()
    val allExpensesFlow: Flow<List<ExpenseEntity>> = expenseDao.observeAllActive()

    val pendingSyncCountFlow: Flow<Int> = combine(
        businessDao.observePendingCount(),
        productDao.observePendingCount(),
        invoiceDao.observePendingCount(),
        expenseDao.observePendingCount()
    ) { b, p, i, e -> b + p + i + e }

    fun observeProducts(businessId: String): Flow<List<ProductEntity>> =
        productDao.observeByBusiness(businessId)

    fun observeInvoices(businessId: String): Flow<List<InvoiceWithItems>> =
        invoiceDao.observeByBusiness(businessId)

    fun observeExpenses(businessId: String): Flow<List<ExpenseEntity>> =
        expenseDao.observeByBusiness(businessId)

    suspend fun ensureInitialized() {
        prefs.ensureDeviceId()
        val count = businessDao.countActiveBusinesses()
        if (count == 0) {
            val primary = BusinessEntity(
                id = AppDatabase.DEFAULT_BUSINESS_ID,
                name = "Hassan Agro",
                tagline = "Crop Protection & Agri Inputs",
                invoicePrefix = "HA",
                defaultTaxRate = 0.0
            )
            businessDao.upsert(primary)
            prefs.setActiveBusinessId(primary.id)
        }
    }

    suspend fun setActiveBusiness(businessId: String) {
        prefs.setActiveBusinessId(businessId)
    }

    suspend fun saveBusiness(business: BusinessEntity) {
        val now = System.currentTimeMillis()
        businessDao.upsert(
            business.copy(
                updatedAt = now,
                syncStatus = SyncStatus.PENDING.name
            )
        )
    }

    suspend fun deleteBusiness(businessId: String): Boolean {
        val activeCount = businessDao.countActiveBusinesses()
        if (activeCount <= 1) return false
        businessDao.softDelete(businessId)
        val remaining = businessDao.observeBusinesses().first()
        remaining.firstOrNull()?.let { prefs.setActiveBusinessId(it.id) }
        return true
    }

    suspend fun saveBusinessLogoFromUri(businessId: String, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val logosDir = File(context.filesDir, "logos").apply { mkdirs() }
                val targetFile = File(logosDir, "logo_${businessId}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
                val existing = businessDao.getById(businessId) ?: return@withContext null
                saveBusiness(existing.copy(logoFilePath = targetFile.absolutePath))
                targetFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }

    suspend fun findProductByBarcode(businessId: String, barcode: String): ProductEntity? =
        productDao.findByBarcode(businessId, barcode.trim())

    suspend fun saveProduct(product: ProductEntity) {
        productDao.upsert(
            product.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name
            )
        )
    }

    suspend fun restockProduct(
        productId: String,
        addQty: Int,
        newBatchNumber: String,
        newExpiryMillis: Long
    ) {
        val existing = productDao.getById(productId) ?: return
        productDao.upsert(
            existing.copy(
                stockQty = existing.stockQty + addQty,
                batchNumber = newBatchNumber.ifBlank { existing.batchNumber },
                expiryDateMillis = newExpiryMillis,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name
            )
        )
    }

    suspend fun deleteProduct(productId: String) {
        productDao.softDelete(productId)
    }

    suspend fun nextInvoiceNumber(business: BusinessEntity): String {
        val nextIndex = invoiceDao.countForBusiness(business.id) + 1
        val prefix = business.invoicePrefix.ifBlank { "HA" }.uppercase()
        return "$prefix-${nextIndex.toString().padStart(4, '0')}"
    }

    suspend fun createInvoiceAndDeductStock(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): InvoiceWithItems {
        val now = System.currentTimeMillis()
        val stampedInvoice = invoice.copy(
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING.name
        )
        db.withTransaction {
            invoiceDao.upsertInvoiceWithItems(stampedInvoice, items)
            items.forEach { item ->
                productDao.adjustStock(item.productId, -item.quantity, now)
            }
        }
        return InvoiceWithItems(stampedInvoice, items)
    }

    suspend fun recordInvoicePayment(invoiceId: String, additionalAmount: Double): InvoiceWithItems? {
        if (additionalAmount <= 0.0) return null
        val existing = invoiceDao.getWithItems(invoiceId) ?: return null
        val inv = existing.invoice
        if (inv.isReturned) return existing
        val newPaid = (inv.amountPaid + additionalAmount).coerceAtMost(inv.grandTotal)
        val newBalance = (inv.grandTotal - newPaid).coerceAtLeast(0.0)
        val newStatus = when {
            newBalance <= 0.01 -> com.example.data.local.InvoicePaymentStatus.PAID.name
            newPaid > 0.0 -> com.example.data.local.InvoicePaymentStatus.PARTIAL.name
            else -> com.example.data.local.InvoicePaymentStatus.CREDIT.name
        }
        val updated = inv.copy(
            amountPaid = newPaid,
            balanceDue = newBalance,
            paymentStatus = newStatus,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        invoiceDao.upsertInvoice(updated)
        return existing.copy(invoice = updated)
    }

    suspend fun returnInvoiceAndRestock(invoiceId: String): Boolean {
        val existing = invoiceDao.getWithItems(invoiceId) ?: return false
        if (existing.invoice.isReturned) return false
        val now = System.currentTimeMillis()
        val returnedInvoice = existing.invoice.copy(
            isReturned = true,
            paymentStatus = com.example.data.local.InvoicePaymentStatus.RETURNED.name,
            balanceDue = 0.0,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING.name
        )
        db.withTransaction {
            invoiceDao.upsertInvoice(returnedInvoice)
            existing.items.forEach { item ->
                productDao.adjustStock(item.productId, item.quantity, now)
            }
        }
        return true
    }

    suspend fun saveExpense(expense: ExpenseEntity) {
        expenseDao.upsert(
            expense.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name
            )
        )
    }

    suspend fun deleteExpense(expenseId: String) {
        expenseDao.softDelete(expenseId)
    }

    suspend fun saveServerConfig(
        baseUrl: String,
        apiToken: String,
        wsUrl: String,
        autoSyncEnabled: Boolean
    ) {
        prefs.saveServerConfig(baseUrl, apiToken, wsUrl, autoSyncEnabled)
    }

    suspend fun testServerConnection(baseUrl: String, apiToken: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                if (!baseUrl.trim().startsWith("http://") && !baseUrl.trim().startsWith("https://")) {
                    return@withContext Result.failure(IllegalArgumentException("Server URL must start with http:// or https://"))
                }
                val deviceId = prefs.ensureDeviceId()
                val api = CustomServerClientFactory.createApi(baseUrl, apiToken, deviceId)
                val response = api.health()
                Result.success(response.message ?: "Connected (${response.status})")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun syncWithCustomServer(): SyncResult = withContext(Dispatchers.IO) {
        val config = prefs.serverConfigFlow.first()
        if (!config.isConfigured) {
            return@withContext SyncResult(
                success = false,
                message = "No server configured. Running in Offline Mode."
            )
        }
        val deviceId = prefs.ensureDeviceId()
        try {
            val api = CustomServerClientFactory.createApi(
                baseUrl = config.serverBaseUrl,
                apiToken = config.apiToken,
                deviceId = deviceId
            )

            // 1. Push pending local changes
            val pendingBusinesses = businessDao.getPendingSync()
            val pendingProducts = productDao.getPendingSync()
            val pendingInvoices = invoiceDao.getPendingSync()
            val pendingExpenses = expenseDao.getPendingSync()

            val totalToPush = pendingBusinesses.size + pendingProducts.size +
                pendingInvoices.size + pendingExpenses.size

            if (totalToPush > 0) {
                val pushResp = api.pushSync(
                    SyncPushRequest(
                        deviceId = deviceId,
                        businesses = pendingBusinesses.map { it.toDto() },
                        products = pendingProducts.map { it.toDto() },
                        invoices = pendingInvoices.map { it.toDto() },
                        expenses = pendingExpenses.map { it.toDto() }
                    )
                )
                if (!pushResp.accepted) {
                    return@withContext SyncResult(
                        success = false,
                        message = pushResp.message ?: "Server rejected sync payload."
                    )
                }
                if (pendingBusinesses.isNotEmpty()) businessDao.markSynced(pendingBusinesses.map { it.id })
                if (pendingProducts.isNotEmpty()) productDao.markSynced(pendingProducts.map { it.id })
                if (pendingInvoices.isNotEmpty()) invoiceDao.markSynced(pendingInvoices.map { it.invoice.id })
                if (pendingExpenses.isNotEmpty()) expenseDao.markSynced(pendingExpenses.map { it.id })
            }

            // 2. Pull remote changes since last sync
            val pullResp = api.pullSync(since = config.lastSyncTimestamp)
            var pulled = 0
            db.withTransaction {
                pullResp.businesses.forEach { dto ->
                    val local = businessDao.getById(dto.id)
                    if (local == null || dto.updatedAt >= local.updatedAt) {
                        businessDao.upsert(dto.toEntity(existingLogoPath = local?.logoFilePath))
                        pulled++
                    }
                }
                pullResp.products.forEach { dto ->
                    val local = productDao.getById(dto.id)
                    if (local == null || dto.updatedAt >= local.updatedAt) {
                        productDao.upsert(dto.toEntity())
                        pulled++
                    }
                }
                pullResp.invoices.forEach { dto ->
                    val local = invoiceDao.getWithItems(dto.id)?.invoice
                    if (local == null || dto.updatedAt >= local.updatedAt) {
                        val (inv, items) = dto.toEntityPair()
                        invoiceDao.upsertInvoiceWithItems(inv, items)
                        pulled++
                    }
                }
                pullResp.expenses.forEach { dto ->
                    val local = expenseDao.getById(dto.id)
                    if (local == null || dto.updatedAt >= local.updatedAt) {
                        expenseDao.upsert(dto.toEntity())
                        pulled++
                    }
                }
            }

            prefs.updateLastSyncTimestamp(pullResp.serverTime)
            SyncResult(
                success = true,
                message = "Synced ($totalToPush pushed, $pulled pulled)",
                pushedCount = totalToPush,
                pulledCount = pulled
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Sync failed (offline data safe): ${e.localizedMessage ?: "Network error"}"
            )
        }
    }
}
