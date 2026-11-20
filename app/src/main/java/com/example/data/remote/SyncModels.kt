package com.example.data.remote

import com.example.data.local.BusinessEntity
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
data class BusinessDto(
    val id: String,
    val name: String,
    val tagline: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val taxId: String = "",
    val invoicePrefix: String = "HA",
    val defaultTaxRate: Double = 0.0,
    val invoiceFooter: String = "",
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
    val category: String = "Insecticide",
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
    val expenseDateMillis: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SyncPushRequest(
    val deviceId: String,
    val pushedAt: Long = System.currentTimeMillis(),
    val businesses: List<BusinessDto> = emptyList(),
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
    val businesses: List<BusinessDto> = emptyList(),
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
    defaultTaxRate = defaultTaxRate,
    invoiceFooter = invoiceFooter,
    logoFilePath = existingLogoPath,
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
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    syncStatus = SyncStatus.SYNCED.name
)

fun InvoiceWithItems.toDto() = InvoiceDto(
    id = invoice.id,
    businessId = invoice.businessId,
    invoiceNumber = invoice.invoiceNumber,
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
    expenseDateMillis = expenseDateMillis,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    syncStatus = SyncStatus.SYNCED.name
)
