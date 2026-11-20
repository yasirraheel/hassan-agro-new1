package com.example.data.remote

import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.ProductEntity
import com.example.data.local.SyncStatus
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val status: String = "ok",
    val serverTime: Long? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthUserDto(
    val id: String,
    val name: String,
    val email: String,
    val roleId: String = "",
    val roleName: String = "",
    val permissions: List<String> = emptyList(),
    val allowedBusinessIds: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String = ""
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val businessName: String,
    val businessType: String = "GENERAL",
    val themeKey: String = "OCEAN",
    val currencySymbol: String = "Rs.",
    val deviceId: String = ""
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val success: Boolean = false,
    val message: String? = null,
    val token: String? = null,
    val user: AuthUserDto? = null,
    val businessId: String? = null,
    val serverTime: Long? = null
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    val success: Boolean = false,
    val message: String? = null,
    val user: AuthUserDto? = null,
    val serverTime: Long? = null
)

@JsonClass(generateAdapter = true)
data class LogoutResponse(
    val success: Boolean = true,
    val message: String? = null
)

// ---------- Team & Custom Roles Management DTOs ----------

@JsonClass(generateAdapter = true)
data class TeamPermissionItemDto(
    val key: String,
    val label: String
)

@JsonClass(generateAdapter = true)
data class TeamRoleDto(
    val id: String,
    val businessId: String = "",
    val name: String,
    val description: String = "",
    val permissions: List<String> = emptyList(),
    val isSystem: Boolean = false,
    val isEditable: Boolean = false,
    val userCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class TeamUserDto(
    val id: String,
    val name: String,
    val email: String,
    val roleId: String,
    val roleName: String = "",
    val permissions: List<String> = emptyList(),
    val isActive: Boolean = true,
    val isOwner: Boolean = false,
    val isPrimaryOwner: Boolean = false,
    val lastLoginMs: Long = 0L
)

@JsonClass(generateAdapter = true)
data class TeamOverviewResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val businessId: String = "",
    val businessName: String = "",
    val roles: List<TeamRoleDto> = emptyList(),
    val users: List<TeamUserDto> = emptyList(),
    val permissionsCatalog: List<TeamPermissionItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SaveTeamRoleRequest(
    val businessId: String,
    val name: String,
    val description: String = "",
    val permissions: List<String>
)

@JsonClass(generateAdapter = true)
data class SaveTeamUserRequest(
    val businessId: String,
    val name: String,
    val email: String,
    val password: String = "",
    val roleId: String = "",
    val customRoleName: String = "",
    val customPermissions: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ToggleTeamUserRequest(
    val businessId: String
)

@JsonClass(generateAdapter = true)
data class TeamActionResponse(
    val ok: Boolean = false,
    val id: String? = null,
    val isActive: Boolean? = null,
    val message: String? = null
)

// ---------- Core Sync DTOs ----------

@JsonClass(generateAdapter = true)
data class BusinessDto(
    val id: String,
    val name: String,
    val tagline: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val taxId: String = "",
    val invoicePrefix: String = "BM",
    val themeKey: String = "OCEAN",
    val businessType: String = "GENERAL",
    val currencySymbol: String = "Rs.",
    val trackExpiry: Boolean = false,
    val trackBatch: Boolean = false,
    val batchFieldLabel: String = "Batch / Lot",
    val packSizeLabel: String = "Unit / Size",
    val productCategoriesJson: String = "",
    val expenseCategoriesJson: String = "",
    val paymentMethodsJson: String = "",
    val pricingTierLabelsJson: String = "",
    val taxPresetsJson: String = "",
    val pricingRulesJson: String = "",
    val defaultTaxRate: Double = 0.0,
    val invoiceFooter: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CustomerDto(
    val id: String,
    val businessId: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val pricingTier: String = "RETAIL",
    val creditLimit: Double = 0.0,
    val openingBalance: Double = 0.0,
    val notes: String = "",
    val performedById: String = "",
    val performedByName: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    val id: String,
    val businessId: String,
    val name: String,
    val brand: String = "",
    val category: String = "General",
    val packSize: String = "",
    val barcode: String = "",
    val batchNumber: String = "",
    val expiryDateMillis: Long,
    val stockQty: Int = 0,
    val lowStockThreshold: Int = 10,
    val purchasePrice: Double = 0.0,
    val mrpPrice: Double = 0.0,
    val distributorPrice: Double = 0.0,
    val wholesalerPrice: Double = 0.0,
    val retailPrice: Double = 0.0,
    val performedById: String = "",
    val performedByName: String = "",
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class InvoiceItemDto(
    val id: String,
    val invoiceId: String,
    val productId: String,
    val productName: String,
    val packSize: String = "",
    val batchNumber: String = "",
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

@JsonClass(generateAdapter = true)
data class InvoiceDto(
    val id: String,
    val businessId: String,
    val invoiceNumber: String,
    val customerId: String = "",
    val customerName: String,
    val customerPhone: String = "",
    val customerEmail: String = "",
    val pricingTier: String = "RETAIL",
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxEnabled: Boolean = false,
    val taxRatePercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val paymentMethod: String = "Cash",
    val paymentStatus: String = "PAID",
    val amountPaid: Double = grandTotal,
    val balanceDue: Double = 0.0,
    val isReturned: Boolean = false,
    val notes: String = "",
    val performedById: String = "",
    val performedByName: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val items: List<InvoiceItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ExpenseDto(
    val id: String,
    val businessId: String,
    val title: String,
    val category: String = "Other",
    val amount: Double,
    val paymentMethod: String = "Cash",
    val payee: String = "",
    val notes: String = "",
    val performedById: String = "",
    val performedByName: String = "",
    val expenseDateMillis: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SyncPushRequest(
    val deviceId: String,
    val pushedAt: Long = System.currentTimeMillis(),
    val businesses: List<BusinessDto> = emptyList(),
    val customers: List<CustomerDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val invoices: List<InvoiceDto> = emptyList(),
    val expenses: List<ExpenseDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SyncPushResponse(
    val accepted: Boolean = true,
    val serverTime: Long = System.currentTimeMillis(),
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncPullResponse(
    val serverTime: Long = System.currentTimeMillis(),
    val user: AuthUserDto? = null,
    val businesses: List<BusinessDto> = emptyList(),
    val customers: List<CustomerDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val invoices: List<InvoiceDto> = emptyList(),
    val expenses: List<ExpenseDto> = emptyList()
)

// Entity <-> DTO mappers
fun BusinessEntity.toDto() = BusinessDto(
    id = id,
    name = name,
    tagline = tagline,
    phone = phone,
    email = email,
    address = address,
    taxId = taxId,
    invoicePrefix = invoicePrefix,
    themeKey = themeKey,
    businessType = businessType,
    currencySymbol = currencySymbol,
    trackExpiry = trackExpiry,
    trackBatch = trackBatch,
    batchFieldLabel = batchFieldLabel,
    packSizeLabel = packSizeLabel,
    productCategoriesJson = productCategoriesJson,
    expenseCategoriesJson = expenseCategoriesJson,
    paymentMethodsJson = paymentMethodsJson,
    pricingTierLabelsJson = pricingTierLabelsJson,
    taxPresetsJson = taxPresetsJson,
    pricingRulesJson = pricingRulesJson,
    defaultTaxRate = defaultTaxRate,
    invoiceFooter = invoiceFooter,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun BusinessDto.toEntity(existingLogoPath: String? = null) = BusinessEntity(
    id = id,
    name = name,
    tagline = tagline,
    phone = phone,
    email = email,
    address = address,
    taxId = taxId,
    invoicePrefix = invoicePrefix,
    themeKey = themeKey.ifBlank { "OCEAN" },
    businessType = businessType.ifBlank { "GENERAL" },
    currencySymbol = currencySymbol.ifBlank { "Rs." },
    trackExpiry = trackExpiry,
    trackBatch = trackBatch,
    batchFieldLabel = batchFieldLabel.ifBlank { "Batch / Lot" },
    packSizeLabel = packSizeLabel.ifBlank { "Unit / Size" },
    productCategoriesJson = productCategoriesJson,
    expenseCategoriesJson = expenseCategoriesJson,
    paymentMethodsJson = paymentMethodsJson,
    pricingTierLabelsJson = pricingTierLabelsJson,
    taxPresetsJson = taxPresetsJson,
    pricingRulesJson = pricingRulesJson,
    defaultTaxRate = defaultTaxRate,
    invoiceFooter = invoiceFooter,
    logoFilePath = existingLogoPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    syncStatus = SyncStatus.SYNCED.name
)

fun CustomerEntity.toDto() = CustomerDto(
    id = id,
    businessId = businessId,
    name = name,
    phone = phone,
    email = email,
    address = address,
    pricingTier = pricingTier,
    creditLimit = creditLimit,
    openingBalance = openingBalance,
    notes = notes,
    performedById = performedById,
    performedByName = performedByName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun CustomerDto.toEntity() = CustomerEntity(
    id = id,
    businessId = businessId,
    name = name,
    phone = phone,
    email = email,
    address = address,
    pricingTier = pricingTier,
    creditLimit = creditLimit,
    openingBalance = openingBalance,
    notes = notes,
    performedById = performedById,
    performedByName = performedByName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    syncStatus = SyncStatus.SYNCED.name
)

fun ProductEntity.toDto() = ProductDto(
    id = id,
    businessId = businessId,
    name = name,
    brand = brand,
    category = category,
    packSize = packSize,
    barcode = barcode,
    batchNumber = batchNumber,
    expiryDateMillis = expiryDateMillis,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    purchasePrice = purchasePrice,
    mrpPrice = mrpPrice,
    distributorPrice = distributorPrice,
    wholesalerPrice = wholesalerPrice,
    retailPrice = retailPrice,
    performedById = performedById,
    performedByName = performedByName,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun ProductDto.toEntity() = ProductEntity(
    id = id,
    businessId = businessId,
    name = name,
    brand = brand,
    category = category,
    packSize = packSize,
    barcode = barcode,
    batchNumber = batchNumber,
    expiryDateMillis = expiryDateMillis,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    purchasePrice = purchasePrice,
    mrpPrice = mrpPrice,
    distributorPrice = distributorPrice,
    wholesalerPrice = wholesalerPrice,
    retailPrice = retailPrice,
    performedById = performedById,
    performedByName = performedByName,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    syncStatus = SyncStatus.SYNCED.name
)

fun InvoiceWithItems.toDto() = InvoiceDto(
    id = invoice.id,
    businessId = invoice.businessId,
    invoiceNumber = invoice.invoiceNumber,
    customerId = invoice.customerId,
    customerName = invoice.customerName,
    customerPhone = invoice.customerPhone,
    customerEmail = invoice.customerEmail,
    pricingTier = invoice.pricingTier,
    subtotal = invoice.subtotal,
    discountAmount = invoice.discountAmount,
    taxEnabled = invoice.taxEnabled,
    taxRatePercent = invoice.taxRatePercent,
    taxAmount = invoice.taxAmount,
    grandTotal = invoice.grandTotal,
    paymentMethod = invoice.paymentMethod,
    paymentStatus = invoice.paymentStatus,
    amountPaid = invoice.amountPaid,
    balanceDue = invoice.balanceDue,
    isReturned = invoice.isReturned,
    notes = invoice.notes,
    performedById = invoice.performedById,
    performedByName = invoice.performedByName,
    createdAt = invoice.createdAt,
    updatedAt = invoice.updatedAt,
    isDeleted = invoice.isDeleted,
    items = items.map {
        InvoiceItemDto(
            id = it.id,
            invoiceId = it.invoiceId,
            productId = it.productId,
            productName = it.productName,
            packSize = it.packSize,
            batchNumber = it.batchNumber,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            lineTotal = it.lineTotal
        )
    }
)

fun InvoiceDto.toEntityPair(): Pair<InvoiceEntity, List<InvoiceItemEntity>> {
    val inv = InvoiceEntity(
        id = id,
        businessId = businessId,
        invoiceNumber = invoiceNumber,
        customerId = customerId,
        customerName = customerName,
        customerPhone = customerPhone,
        customerEmail = customerEmail,
        pricingTier = pricingTier,
        subtotal = subtotal,
        discountAmount = discountAmount,
        taxEnabled = taxEnabled,
        taxRatePercent = taxRatePercent,
        taxAmount = taxAmount,
        grandTotal = grandTotal,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        isReturned = isReturned,
        notes = notes,
        performedById = performedById,
        performedByName = performedByName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        syncStatus = SyncStatus.SYNCED.name
    )
    val itemEntities = items.map {
        InvoiceItemEntity(
            id = it.id,
            invoiceId = id,
            productId = it.productId,
            productName = it.productName,
            packSize = it.packSize,
            batchNumber = it.batchNumber,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            lineTotal = it.lineTotal
        )
    }
    return inv to itemEntities
}

fun ExpenseEntity.toDto() = ExpenseDto(
    id = id,
    businessId = businessId,
    title = title,
    category = category,
    amount = amount,
    paymentMethod = paymentMethod,
    payee = payee,
    notes = notes,
    performedById = performedById,
    performedByName = performedByName,
    expenseDateMillis = expenseDateMillis,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun ExpenseDto.toEntity() = ExpenseEntity(
    id = id,
    businessId = businessId,
    title = title,
    category = category,
    amount = amount,
    paymentMethod = paymentMethod,
    payee = payee,
    notes = notes,
    performedById = performedById,
    performedByName = performedByName,
    expenseDateMillis = expenseDateMillis,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    syncStatus = SyncStatus.SYNCED.name
)
