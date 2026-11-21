package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

enum class PricingTier(val label: String, val shortLabel: String) {
    RETAIL("Retail", "Ret"),
    WHOLESALER("Wholesaler", "Whls"),
    DISTRIBUTOR("Distributor", "Dist"),
    MRP("MRP", "MRP");

    companion object {
        fun fromName(name: String): PricingTier =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: RETAIL
    }
}

enum class SyncStatus {
    PENDING,
    SYNCED
}

enum class PesticideCategory(val label: String) {
    INSECTICIDE("Insecticide"),
    HERBICIDE("Herbicide"),
    FUNGICIDE("Fungicide"),
    FERTILIZER("Fertilizer"),
    SEED_TREATMENT("Seed Treatment"),
    ADJUVANT("Adjuvant"),
    OTHER("Other");

    companion object {
        fun fromLabel(label: String): PesticideCategory =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: OTHER
    }
}

enum class ExpenseCategory(val label: String) {
    STOCK_PURCHASE("Stock Purchase"),
    SHOP_RENT("Shop Rent"),
    TRANSPORT("Transport"),
    UTILITIES("Utilities"),
    STAFF_SALARY("Staff Salary"),
    PACKAGING("Packaging"),
    MARKETING("Marketing"),
    OTHER("Other");

    companion object {
        fun fromLabel(label: String): ExpenseCategory =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: OTHER
    }
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    BANK("Bank Transfer"),
    MOBILE_WALLET("JazzCash / EasyPaisa"),
    CHEQUE("Cheque");

    companion object {
        fun fromLabel(label: String): PaymentMethod =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: CASH
    }
}

enum class ExpiryStatus(val label: String) {
    EXPIRED("Expired"),
    EXPIRING_SOON("Expiring Soon"),
    WATCH("Expiring ≤ 90d"),
    SAFE("Valid")
}

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tagline: String = "Retail & Wholesale Management",
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
    val invoiceFooter: String = "Thank you for your business!",
    val logoFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = SyncStatus.PENDING.name
) {
    fun productCategories(): List<String> = CustomizationJson.decodeList(
        productCategoriesJson,
        BusinessTypePreset.fromKey(businessType).productCategories
    )

    fun expenseCategories(): List<String> = CustomizationJson.decodeList(
        expenseCategoriesJson,
        BusinessTypePreset.fromKey(businessType).expenseCategories
    )

    fun paymentMethods(): List<String> = CustomizationJson.decodeList(
        paymentMethodsJson,
        BusinessTypePreset.fromKey(businessType).paymentMethods
    )

    fun tierLabel(tier: PricingTier): String {
        val map = CustomizationJson.decodeTierLabels(
            pricingTierLabelsJson,
            BusinessTypePreset.fromKey(businessType).pricingTierLabels
        )
        return map[tier.name] ?: tier.label
    }

    fun taxPresets(): List<TaxPreset> = CustomizationJson.decodeTaxPresets(taxPresetsJson)

    fun pricingRules(): PricingMarginRules = CustomizationJson.decodePricingRules(pricingRulesJson)

    fun resolvedCurrency(): String = currencySymbol.trim().ifBlank { "Rs." }
}

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["businessId", "phone"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val pricingTier: String = PricingTier.RETAIL.name,
    val creditLimit: Double = 0.0,
    val openingBalance: Double = 0.0,
    val notes: String = "",
    val performedById: String = "",
    val performedByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["businessId", "barcode"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = SyncStatus.PENDING.name
) {
    fun priceForTier(tier: PricingTier): Double = when (tier) {
        PricingTier.MRP -> mrpPrice
        PricingTier.DISTRIBUTOR -> distributorPrice
        PricingTier.WHOLESALER -> wholesalerPrice
        PricingTier.RETAIL -> retailPrice
    }
}

enum class InvoicePaymentStatus(val label: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    CREDIT("Credit"),
    RETURNED("Returned");

    companion object {
        fun fromName(name: String): InvoicePaymentStatus =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PAID
    }
}

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["businessId"]), Index(value = ["createdAt"])]
)
data class InvoiceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val invoiceNumber: String,
    val customerId: String = "",
    val customerName: String,
    val customerPhone: String = "",
    val customerEmail: String = "",
    val pricingTier: String = PricingTier.RETAIL.name,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxEnabled: Boolean = false,
    val taxRatePercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val paymentMethod: String = PaymentMethod.CASH.label,
    val paymentStatus: String = InvoicePaymentStatus.PAID.name,
    val amountPaid: Double = grandTotal,
    val balanceDue: Double = 0.0,
    val isReturned: Boolean = false,
    val notes: String = "",
    val proofImagePath: String? = null,
    val performedById: String = "",
    val performedByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "invoice_items",
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceId: String,
    val productId: String,
    val productName: String,
    val packSize: String = "",
    val batchNumber: String = "",
    val quantity: Int,
    val unitPrice: Double,
    val costPrice: Double = 0.0,
    val lineTotal: Double
)

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
)

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["businessId"]), Index(value = ["expenseDateMillis"])]
)
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val title: String,
    val category: String = ExpenseCategory.OTHER.label,
    val amount: Double,
    val paymentMethod: String = PaymentMethod.CASH.label,
    val payee: String = "",
    val notes: String = "",
    val proofImagePath: String? = null,
    val performedById: String = "",
    val performedByName: String = "",
    val expenseDateMillis: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = SyncStatus.PENDING.name
)
