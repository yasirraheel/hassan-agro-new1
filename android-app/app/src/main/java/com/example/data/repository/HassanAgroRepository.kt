package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.AuthSession
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.MonthlyTotalRow
import com.example.data.local.PreferencesStore
import com.example.data.local.ProductEntity
import com.example.data.local.ServerSyncConfig
import com.example.data.local.SyncStatus
import com.example.data.local.ThemeAppearanceMode
import com.example.data.remote.CustomServerClientFactory
import com.example.data.remote.LoginRequest
import com.example.data.remote.RegisterRequest
import com.example.data.remote.SaveTeamRoleRequest
import com.example.data.remote.SaveTeamUserRequest
import com.example.data.remote.SyncPushRequest
import com.example.data.remote.TeamOverviewResponse
import com.example.data.remote.ToggleTeamUserRequest
import com.example.data.remote.toDto
import com.example.data.remote.toEntity
import com.example.data.remote.toEntityPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream

data class SyncResult(
    val success: Boolean,
    val message: String,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val unauthorized: Boolean = false
)

data class AuthActionResult(
    val success: Boolean,
    val message: String,
    val otpRequired: Boolean = false,
    val otpEmail: String = "",
    val otpBusinessName: String = ""
)

class HassanAgroRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: PreferencesStore
) {
    private val businessDao = db.businessDao()
    private val customerDao = db.customerDao()
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()
    private val expenseDao = db.expenseDao()

    val businessesFlow: Flow<List<BusinessEntity>> = businessDao.observeBusinesses()
    val activeBusinessIdFlow: Flow<String> = prefs.activeBusinessIdFlow
    val appearanceModeFlow: Flow<ThemeAppearanceMode> = prefs.appearanceModeFlow
    val autoIncludeTaxMapFlow: Flow<Map<String, Boolean>> = prefs.autoIncludeTaxMapFlow
    val serverConfigFlow: Flow<ServerSyncConfig> = prefs.serverConfigFlow
    val authSessionFlow: Flow<AuthSession> = prefs.authSessionFlow

    suspend fun setAppearanceMode(mode: ThemeAppearanceMode) {
        prefs.setAppearanceMode(mode)
    }

    suspend fun setAutoIncludeTaxForBusiness(businessId: String, enabled: Boolean) {
        prefs.setAutoIncludeTaxForBusiness(businessId, enabled)
    }

    val allProductsFlow: Flow<List<ProductEntity>> = productDao.observeAllActive()
    val allInvoicesFlow: Flow<List<InvoiceWithItems>> = invoiceDao.observeAllActive()
    val allExpensesFlow: Flow<List<ExpenseEntity>> = expenseDao.observeAllActive()

    val pendingSyncCountFlow: Flow<Int> = combine(
        businessDao.observePendingCount(),
        customerDao.observePendingCount(),
        productDao.observePendingCount(),
        invoiceDao.observePendingCount(),
        expenseDao.observePendingCount()
    ) { b, c, p, i, e -> b + c + p + i + e }

    fun observeCustomers(businessId: String): Flow<List<CustomerEntity>> =
        customerDao.observeByBusiness(businessId)

    fun observeProducts(businessId: String): Flow<List<ProductEntity>> =
        productDao.observeByBusiness(businessId)

    fun observeInvoices(businessId: String): Flow<List<InvoiceWithItems>> =
        invoiceDao.observeByBusiness(businessId)

    fun observeExpenses(businessId: String): Flow<List<ExpenseEntity>> =
        expenseDao.observeByBusiness(businessId)

    fun observeMonthlySalesTotals(businessId: String, sinceMillis: Long): Flow<List<MonthlyTotalRow>> =
        invoiceDao.observeMonthlySalesTotals(businessId, sinceMillis)

    fun observeMonthlyExpenseTotals(businessId: String, sinceMillis: Long): Flow<List<MonthlyTotalRow>> =
        expenseDao.observeMonthlyExpenseTotals(businessId, sinceMillis)

    fun observeAllMonthlySalesTotals(allowedBusinessIds: Set<String>, sinceMillis: Long): Flow<List<MonthlyTotalRow>> =
        if (allowedBusinessIds.isEmpty()) {
            invoiceDao.observeAllMonthlySalesTotals(sinceMillis)
        } else {
            invoiceDao.observeMonthlySalesTotalsForBusinesses(allowedBusinessIds.toList(), sinceMillis)
        }

    fun observeAllMonthlyExpenseTotals(allowedBusinessIds: Set<String>, sinceMillis: Long): Flow<List<MonthlyTotalRow>> =
        if (allowedBusinessIds.isEmpty()) {
            expenseDao.observeAllMonthlyExpenseTotals(sinceMillis)
        } else {
            expenseDao.observeMonthlyExpenseTotalsForBusinesses(allowedBusinessIds.toList(), sinceMillis)
        }

    suspend fun ensureInitialized() {
        prefs.ensureDeviceId()
    }

    private fun extractErrorMessage(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(errorBody)
            json.optString("message").ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    private suspend fun clearLocalCache() {
        db.withTransaction {
            invoiceDao.deleteAllItems()
            invoiceDao.deleteAll()
            expenseDao.deleteAll()
            productDao.deleteAll()
            customerDao.deleteAll()
            businessDao.deleteAll()
        }
    }

    suspend fun login(
        serverBaseUrl: String,
        email: String,
        password: String,
        otpCode: String? = null
    ): AuthActionResult =
        withContext(Dispatchers.IO) {
            val cleanUrl = serverBaseUrl.trim().ifBlank { PreferencesStore.DEFAULT_SERVER_URL }
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                return@withContext AuthActionResult(false, "Server URL must start with http:// or https://")
            }
            try {
                prefs.saveServerUrlOnly(cleanUrl)
                val deviceId = prefs.ensureDeviceId()
                val api = CustomServerClientFactory.createApi(cleanUrl, "", deviceId)
                val resp = api.login(
                    LoginRequest(
                        email = email.trim(),
                        password = password,
                        deviceId = deviceId,
                        otpCode = otpCode?.trim()?.ifBlank { null }
                    )
                )
                val body = resp.body()
                if (resp.isSuccessful && body != null && body.otpRequired) {
                    return@withContext AuthActionResult(
                        success = true,
                        message = body.message ?: "Your account is not verified yet. Enter the verification code sent to ${email.trim()}.",
                        otpRequired = true,
                        otpEmail = body.email ?: email.trim(),
                        otpBusinessName = body.businessName.orEmpty()
                    )
                }
                if (!resp.isSuccessful || body == null || !body.success || body.token.isNullOrBlank() || body.user == null) {
                    val err = body?.message ?: extractErrorMessage(resp.errorBody()?.string(), "Invalid email or password")
                    return@withContext AuthActionResult(false, err)
                }

                val previousUserId = prefs.authSessionFlow.first().userId
                if (previousUserId != body.user.id) {
                    clearLocalCache()
                }

                prefs.saveAuthSession(
                    token = body.token,
                    userId = body.user.id,
                    userName = body.user.name,
                    userEmail = body.user.email,
                    roleId = body.user.roleId,
                    roleName = body.user.roleName,
                    permissions = body.user.permissions,
                    allowedBusinessIds = body.user.allowedBusinessIds
                )

                // Immediately pull businesses, products, invoices & expenses from server
                syncWithCustomServer()

                val availableBusinesses = businessDao.observeBusinesses().first()
                    .filter { body.user.allowedBusinessIds.isEmpty() || body.user.allowedBusinessIds.contains(it.id) }
                if (availableBusinesses.isNotEmpty()) {
                    prefs.setActiveBusinessId(availableBusinesses.first().id)
                }

                AuthActionResult(true, body.message ?: "Welcome back, ${body.user.name}!")
            } catch (e: Exception) {
                AuthActionResult(false, "Unable to reach server: ${e.localizedMessage ?: "Check internet or Server URL"}")
            }
        }

    suspend fun register(
        serverBaseUrl: String,
        name: String,
        email: String,
        password: String,
        businessName: String,
        businessType: String,
        themeKey: String,
        currencySymbol: String,
        otpCode: String? = null
    ): AuthActionResult = withContext(Dispatchers.IO) {
        val cleanUrl = serverBaseUrl.trim().ifBlank { PreferencesStore.DEFAULT_SERVER_URL }
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return@withContext AuthActionResult(false, "Server URL must start with http:// or https://")
        }
        try {
            prefs.saveServerUrlOnly(cleanUrl)
            val deviceId = prefs.ensureDeviceId()
            val api = CustomServerClientFactory.createApi(cleanUrl, "", deviceId)
            val resp = api.register(
                RegisterRequest(
                    name = name.trim(),
                    email = email.trim(),
                    password = password,
                    businessName = businessName.trim(),
                    businessType = businessType,
                    themeKey = themeKey,
                    currencySymbol = currencySymbol.trim().ifBlank { "Rs." },
                    deviceId = deviceId,
                    otpCode = otpCode?.trim()?.ifBlank { null }
                )
            )
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.otpRequired) {
                return@withContext AuthActionResult(
                    success = true,
                    message = body.message ?: "Verification code sent to ${email.trim()}",
                    otpRequired = true,
                    otpEmail = body.email ?: email.trim(),
                    otpBusinessName = body.businessName ?: businessName.trim()
                )
            }
            if (!resp.isSuccessful || body == null || !body.success || body.token.isNullOrBlank() || body.user == null) {
                val err = body?.message ?: extractErrorMessage(resp.errorBody()?.string(), "Could not create account")
                return@withContext AuthActionResult(false, err)
            }

            clearLocalCache()

            prefs.saveAuthSession(
                token = body.token,
                userId = body.user.id,
                userName = body.user.name,
                userEmail = body.user.email,
                roleId = body.user.roleId,
                roleName = body.user.roleName,
                permissions = body.user.permissions,
                allowedBusinessIds = body.user.allowedBusinessIds
            )

            syncWithCustomServer()

            val targetBizId = body.businessId ?: body.user.allowedBusinessIds.firstOrNull()
            if (!targetBizId.isNullOrBlank()) {
                prefs.setActiveBusinessId(targetBizId)
            }

            AuthActionResult(true, body.message ?: "Welcome to Business Manager!")
        } catch (e: Exception) {
            AuthActionResult(false, "Unable to reach server: ${e.localizedMessage ?: "Check internet or Server URL"}")
        }
    }

    suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
        val session = prefs.authSessionFlow.first()
        val config = prefs.serverConfigFlow.first()
        if (!session.isLoggedIn || !config.isConfigured) return@withContext session.isLoggedIn
        try {
            val deviceId = prefs.ensureDeviceId()
            val api = CustomServerClientFactory.createApi(config.serverBaseUrl, session.authToken, deviceId)
            val resp = api.me()
            if (resp.code() == 401 || resp.code() == 403) {
                prefs.clearAuthSession()
                return@withContext false
            }
            val user = resp.body()?.user
            if (resp.isSuccessful && user != null) {
                prefs.updateAuthUser(
                    userId = user.id,
                    userName = user.name,
                    userEmail = user.email,
                    roleId = user.roleId,
                    roleName = user.roleName,
                    permissions = user.permissions,
                    allowedBusinessIds = user.allowedBusinessIds
                )
            }
            true
        } catch (_: Exception) {
            // Keep cached session active while offline
            true
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val session = prefs.authSessionFlow.first()
        val config = prefs.serverConfigFlow.first()
        if (session.isLoggedIn && config.isConfigured) {
            try {
                val deviceId = prefs.ensureDeviceId()
                val api = CustomServerClientFactory.createApi(config.serverBaseUrl, session.authToken, deviceId)
                api.logout()
            } catch (_: Exception) {
                // Best effort server logout
            }
        }
        prefs.clearAuthSession()
        clearLocalCache()
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
        prefs.addAllowedBusinessIdLocally(business.id)
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

    suspend fun saveProofImageFromUri(prefix: String, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val proofsDir = File(context.filesDir, "proofs").apply { mkdirs() }
                val safePrefix = prefix.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "proof" }
                val targetFile = File(proofsDir, "${safePrefix}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
                targetFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }

    suspend fun saveCustomer(customer: CustomerEntity, performedById: String, performedByName: String) {
        customerDao.upsert(
            customer.copy(
                performedById = performedById,
                performedByName = performedByName,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name
            )
        )
    }

    suspend fun deleteCustomer(customerId: String) {
        customerDao.softDelete(customerId)
    }

    suspend fun findProductByBarcode(businessId: String, barcode: String): ProductEntity? =
        productDao.findByBarcode(businessId, barcode.trim())

    suspend fun getProductById(productId: String): ProductEntity? = productDao.getById(productId)

    suspend fun saveProduct(product: ProductEntity, performedById: String, performedByName: String) {
        productDao.upsert(
            product.copy(
                performedById = performedById,
                performedByName = performedByName,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name
            )
        )
    }

    suspend fun restockProduct(
        productId: String,
        addQty: Int,
        newBatchNumber: String,
        newExpiryMillis: Long,
        performedById: String,
        performedByName: String,
        updatedPurchasePrice: Double? = null
    ): ProductEntity? {
        val existing = productDao.getById(productId) ?: return null
        val updated = existing.copy(
            stockQty = existing.stockQty + addQty,
            purchasePrice = updatedPurchasePrice?.takeIf { it > 0.0 } ?: existing.purchasePrice,
            batchNumber = newBatchNumber.ifBlank { existing.batchNumber },
            expiryDateMillis = newExpiryMillis,
            performedById = performedById,
            performedByName = performedByName,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        productDao.upsert(updated)
        return updated
    }

    suspend fun deleteProduct(productId: String) {
        productDao.softDelete(productId)
    }

    suspend fun nextInvoiceNumber(business: BusinessEntity): String {
        val nextIndex = invoiceDao.countForBusiness(business.id) + 1
        val prefix = business.invoicePrefix.ifBlank { "BM" }.uppercase()
        return "$prefix-${nextIndex.toString().padStart(4, '0')}"
    }

    suspend fun createInvoiceAndDeductStock(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        performedById: String,
        performedByName: String
    ): InvoiceWithItems {
        val now = System.currentTimeMillis()
        val stampedInvoice = invoice.copy(
            performedById = performedById,
            performedByName = performedByName,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING.name
        )
        db.withTransaction {
            invoiceDao.upsertInvoiceWithItems(stampedInvoice, items)
            items.forEach { item ->
                productDao.adjustStock(item.productId, -item.quantity, now, performedById, performedByName)
            }
        }
        return InvoiceWithItems(stampedInvoice, items)
    }

    suspend fun recordInvoicePayment(
        invoiceId: String,
        additionalAmount: Double,
        performedById: String,
        performedByName: String
    ): InvoiceWithItems? {
        if (additionalAmount <= 0.0) return null
        val existing = invoiceDao.getWithItems(invoiceId) ?: return null
        val inv = existing.invoice
        if (inv.isReturned) return existing
        val newPaid = (inv.amountPaid + additionalAmount).coerceAtMost(inv.grandTotal)
        val newBalance = (inv.grandTotal - newPaid).coerceAtLeast(0.0)
        val newStatus = when {
            newBalance <= 0.01 -> InvoicePaymentStatus.PAID.name
            newPaid > 0.0 -> InvoicePaymentStatus.PARTIAL.name
            else -> InvoicePaymentStatus.CREDIT.name
        }
        val updated = inv.copy(
            amountPaid = newPaid,
            balanceDue = newBalance,
            paymentStatus = newStatus,
            performedById = performedById.ifBlank { inv.performedById },
            performedByName = performedByName.ifBlank { inv.performedByName },
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        invoiceDao.upsertInvoice(updated)
        return existing.copy(invoice = updated)
    }

    suspend fun returnInvoiceAndRestock(
        invoiceId: String,
        performedById: String,
        performedByName: String
    ): Boolean {
        val existing = invoiceDao.getWithItems(invoiceId) ?: return false
        if (existing.invoice.isReturned) return false
        val now = System.currentTimeMillis()
        val returnedInvoice = existing.invoice.copy(
            isReturned = true,
            paymentStatus = InvoicePaymentStatus.RETURNED.name,
            balanceDue = 0.0,
            performedById = performedById.ifBlank { existing.invoice.performedById },
            performedByName = performedByName.ifBlank { existing.invoice.performedByName },
            updatedAt = now,
            syncStatus = SyncStatus.PENDING.name
        )
        db.withTransaction {
            invoiceDao.upsertInvoice(returnedInvoice)
            existing.items.forEach { item ->
                productDao.adjustStock(item.productId, item.quantity, now, performedById, performedByName)
            }
        }
        return true
    }

    suspend fun saveExpense(expense: ExpenseEntity, performedById: String, performedByName: String) {
        expenseDao.upsert(
            expense.copy(
                performedById = performedById,
                performedByName = performedByName,
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
                val session = prefs.authSessionFlow.first()
                val tokenToUse = apiToken.trim().ifBlank { session.authToken }
                val api = CustomServerClientFactory.createApi(baseUrl, tokenToUse, deviceId)
                val response = api.health()
                Result.success(response.message ?: "Connected (${response.status})")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun syncWithCustomServer(): SyncResult = withContext(Dispatchers.IO) {
        val config = prefs.serverConfigFlow.first()
        val session = prefs.authSessionFlow.first()
        if (!config.isConfigured) {
            return@withContext SyncResult(
                success = false,
                message = "No server configured. Running in Offline Mode."
            )
        }
        val activeToken = session.authToken.ifBlank { config.apiToken }
        val deviceId = prefs.ensureDeviceId()
        try {
            val api = CustomServerClientFactory.createApi(
                baseUrl = config.serverBaseUrl,
                apiToken = activeToken,
                deviceId = deviceId
            )

            // 1. Push pending local changes
            val pendingBusinesses = businessDao.getPendingSync()
            val pendingCustomers = customerDao.getPendingSync()
            val pendingProducts = productDao.getPendingSync()
            val pendingInvoices = invoiceDao.getPendingSync()
            val pendingExpenses = expenseDao.getPendingSync()

            val totalToPush = pendingBusinesses.size + pendingCustomers.size + pendingProducts.size +
                pendingInvoices.size + pendingExpenses.size

            if (totalToPush > 0) {
                val pushResp = api.pushSync(
                    SyncPushRequest(
                        deviceId = deviceId,
                        businesses = pendingBusinesses.map { it.toDto() },
                        customers = pendingCustomers.map { it.toDto() },
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
                if (pendingCustomers.isNotEmpty()) customerDao.markSynced(pendingCustomers.map { it.id })
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
                pullResp.customers.forEach { dto ->
                    val local = customerDao.getById(dto.id)
                    if (local == null || dto.updatedAt >= local.updatedAt) {
                        customerDao.upsert(dto.toEntity())
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

            // Apply live role/permission/business scope updates pushed from server
            pullResp.user?.let { refreshedUser ->
                prefs.updateAuthUser(
                    userId = refreshedUser.id,
                    userName = refreshedUser.name,
                    userEmail = refreshedUser.email,
                    roleId = refreshedUser.roleId,
                    roleName = refreshedUser.roleName,
                    permissions = refreshedUser.permissions,
                    allowedBusinessIds = refreshedUser.allowedBusinessIds
                )
            }

            prefs.updateLastSyncTimestamp(pullResp.serverTime)
            SyncResult(
                success = true,
                message = "Synced ($totalToPush pushed, $pulled pulled)",
                pushedCount = totalToPush,
                pulledCount = pulled
            )
        } catch (http: HttpException) {
            if (http.code() == 401) {
                prefs.clearAuthSession()
                return@withContext SyncResult(
                    success = false,
                    message = "Session expired. Please sign in again.",
                    unauthorized = true
                )
            }
            val msg = extractErrorMessage(http.response()?.errorBody()?.string(), "HTTP ${http.code()} error")
            SyncResult(success = false, message = "Sync failed: $msg")
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Sync failed (offline data safe): ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    // ---------- Business Owner In-App Team & Custom Roles API ----------

    private suspend fun createAuthedApi() = run {
        val config = prefs.serverConfigFlow.first()
        val session = prefs.authSessionFlow.first()
        if (!config.isConfigured || session.authToken.isBlank()) {
            throw IllegalStateException("Please sign in to a connected server to manage team & roles.")
        }
        val deviceId = prefs.ensureDeviceId()
        CustomServerClientFactory.createApi(config.serverBaseUrl, session.authToken, deviceId)
    }

    suspend fun fetchTeamOverview(businessId: String): Result<TeamOverviewResponse> = withContext(Dispatchers.IO) {
        try {
            val api = createAuthedApi()
            val resp = api.getTeamOverview(businessId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.ok) {
                Result.success(body)
            } else {
                val msg = body?.message ?: extractErrorMessage(resp.errorBody()?.string(), "Unable to load team (${resp.code()})")
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTeamRole(
        roleId: String?,
        businessId: String,
        name: String,
        description: String,
        permissions: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = createAuthedApi()
            val req = SaveTeamRoleRequest(
                businessId = businessId,
                name = name.trim(),
                description = description.trim(),
                permissions = permissions
            )
            val resp = if (roleId.isNullOrBlank()) api.createTeamRole(req) else api.updateTeamRole(roleId, req)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.ok) {
                Result.success(body.message ?: "Role saved")
            } else {
                val msg = body?.message ?: extractErrorMessage(resp.errorBody()?.string(), "Failed to save role")
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTeamRole(roleId: String, businessId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = createAuthedApi()
            val resp = api.deleteTeamRole(roleId, businessId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.ok) {
                Result.success(body.message ?: "Role deleted")
            } else {
                val msg = body?.message ?: extractErrorMessage(resp.errorBody()?.string(), "Failed to delete role")
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTeamUser(
        userId: String?,
        businessId: String,
        name: String,
        email: String,
        password: String,
        roleId: String,
        customRoleName: String = "",
        customPermissions: List<String>? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = createAuthedApi()
            val req = SaveTeamUserRequest(
                businessId = businessId,
                name = name.trim(),
                email = email.trim(),
                password = password,
                roleId = roleId,
                customRoleName = customRoleName.trim(),
                customPermissions = customPermissions
            )
            val resp = if (userId.isNullOrBlank()) api.createTeamUser(req) else api.updateTeamUser(userId, req)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.ok) {
                Result.success(body.message ?: "Team member saved")
            } else {
                val msg = body?.message ?: extractErrorMessage(resp.errorBody()?.string(), "Failed to save team member")
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTeamUser(userId: String, businessId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = createAuthedApi()
            val resp = api.toggleTeamUser(userId, ToggleTeamUserRequest(businessId))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.ok) {
                Result.success(body.message ?: "Status updated")
            } else {
                val msg = body?.message ?: extractErrorMessage(resp.errorBody()?.string(), "Failed to toggle user")
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTeamUser(userId: String, businessId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = createAuthedApi()
            val resp = api.deleteTeamUser(userId, businessId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.ok) {
                Result.success(body.message ?: "User removed")
            } else {
                val msg = body?.message ?: extractErrorMessage(resp.errorBody()?.string(), "Failed to remove user")
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
