package com.example.data.local

import org.json.JSONArray
import org.json.JSONObject

/**
 * Granular RBAC permissions matching Laravel config/sync.php verbatim.
 */
enum class AppPermission(val key: String, val label: String) {
    VIEW_DASHBOARD("view_dashboard", "Dashboard & KPIs"),
    MANAGE_INVENTORY("manage_inventory", "Manage Inventory"),
    USE_POS("use_pos", "POS Billing"),
    COLLECT_CREDIT("collect_credit", "Collect Credit"),
    RETURN_SALES("return_sales", "Return Sales"),
    MANAGE_EXPENSES("manage_expenses", "Manage Expenses"),
    VIEW_REPORTS("view_reports", "View Reports"),
    EXPORT_DATA("export_data", "Export PDF / CSV"),
    MANAGE_BUSINESSES("manage_businesses", "Manage Businesses"),
    MANAGE_SETTINGS("manage_settings", "Manage Settings"),
    MANAGE_TEAM("manage_team", "Manage Team & Custom Roles"),
    MANAGE_ROLES("manage_roles", "Manage Roles (Admin)");

    companion object {
        val ALL_KEYS: Set<String> = entries.map { it.key }.toSet()
    }
}

/**
 * Industry presets that pre-fill categories, payment methods, tracking toggles & theme
 * when creating a new business — all 100% editable/deletable afterwards!
 */
enum class BusinessTypePreset(
    val key: String,
    val label: String,
    val tagline: String,
    val defaultThemeKey: String,
    val trackExpiry: Boolean,
    val trackBatch: Boolean,
    val batchFieldLabel: String,
    val packSizeLabel: String,
    val productCategories: List<String>,
    val expenseCategories: List<String>,
    val paymentMethods: List<String>,
    val pricingTierLabels: Map<String, String>
) {
    GENERAL(
        key = "GENERAL",
        label = "General Retail / Store",
        tagline = "Retail & Wholesale Management",
        defaultThemeKey = "OCEAN",
        trackExpiry = false,
        trackBatch = false,
        batchFieldLabel = "Batch / Lot",
        packSizeLabel = "Unit / Size",
        productCategories = listOf("General", "Fast Moving", "Accessories", "Packaged Goods", "Supplies", "Other"),
        expenseCategories = listOf("Stock Purchase", "Shop Rent", "Staff Salary", "Utilities", "Transport", "Marketing", "Maintenance", "Other"),
        paymentMethods = listOf("Cash", "Bank Transfer", "JazzCash / EasyPaisa", "Cheque"),
        pricingTierLabels = mapOf("MRP" to "MRP", "DISTRIBUTOR" to "Distributor", "WHOLESALER" to "Wholesaler", "RETAIL" to "Retail")
    ),
    PESTICIDES(
        key = "PESTICIDES",
        label = "Agri & Pesticides",
        tagline = "Crop Protection & Agri Inputs",
        defaultThemeKey = "EMERALD",
        trackExpiry = true,
        trackBatch = true,
        batchFieldLabel = "Batch No.",
        packSizeLabel = "Pack Size",
        productCategories = listOf("Insecticide", "Herbicide", "Fungicide", "Fertilizer", "Seed Treatment", "Adjuvant", "Other"),
        expenseCategories = listOf("Stock Purchase", "Shop Rent", "Transport", "Utilities", "Staff Salary", "Packaging", "Marketing", "Other"),
        paymentMethods = listOf("Cash", "Bank Transfer", "JazzCash / EasyPaisa", "Cheque"),
        pricingTierLabels = mapOf("MRP" to "MRP", "DISTRIBUTOR" to "Distributor", "WHOLESALER" to "Wholesaler", "RETAIL" to "Retail")
    ),
    PHARMACY(
        key = "PHARMACY",
        label = "Pharmacy & Medical",
        tagline = "Healthcare & Medicines",
        defaultThemeKey = "TEAL",
        trackExpiry = true,
        trackBatch = true,
        batchFieldLabel = "Batch / Lot",
        packSizeLabel = "Strip / Bottle",
        productCategories = listOf("Tablets", "Syrups", "Injections", "Capsules", "Surgical", "OTC & Care", "Other"),
        expenseCategories = listOf("Medicine Purchase", "Shop Rent", "Pharmacist Salary", "Cold Storage / Utilities", "Delivery", "License & Compliance", "Other"),
        paymentMethods = listOf("Cash", "Bank Transfer", "JazzCash / EasyPaisa", "Card"),
        pricingTierLabels = mapOf("MRP" to "MRP", "DISTRIBUTOR" to "Distributor", "WHOLESALER" to "Wholesaler", "RETAIL" to "Retail")
    ),
    ELECTRONICS(
        key = "ELECTRONICS",
        label = "Electronics & Mobile",
        tagline = "Devices, Parts & Accessories",
        defaultThemeKey = "INDIGO",
        trackExpiry = false,
        trackBatch = true,
        batchFieldLabel = "Serial / IMEI",
        packSizeLabel = "Model / Spec",
        productCategories = listOf("Smartphones", "Accessories", "Chargers & Cables", "Audio", "Laptops", "Spare Parts", "Other"),
        expenseCategories = listOf("Inventory Purchase", "Shop Rent", "Staff Salary", "Utilities", "Repairs & Tools", "Marketing", "Other"),
        paymentMethods = listOf("Cash", "Bank Transfer", "JazzCash / EasyPaisa", "Card"),
        pricingTierLabels = mapOf("MRP" to "List Price", "DISTRIBUTOR" to "Dealer", "WHOLESALER" to "Bulk", "RETAIL" to "Retail")
    ),
    GROCERY(
        key = "GROCERY",
        label = "Grocery & Super Mart",
        tagline = "Fresh & Daily Essentials",
        defaultThemeKey = "CRIMSON",
        trackExpiry = true,
        trackBatch = false,
        batchFieldLabel = "Lot No.",
        packSizeLabel = "Weight / Pack",
        productCategories = listOf("Staples & Grains", "Beverages", "Dairy", "Snacks", "Household", "Personal Care", "Other"),
        expenseCategories = listOf("Stock Purchase", "Shop Rent", "Staff Salary", "Electricity", "Transport", "Packaging", "Other"),
        paymentMethods = listOf("Cash", "Bank Transfer", "JazzCash / EasyPaisa", "Card"),
        pricingTierLabels = mapOf("MRP" to "MRP", "DISTRIBUTOR" to "Distributor", "WHOLESALER" to "Wholesaler", "RETAIL" to "Retail")
    ),
    HARDWARE(
        key = "HARDWARE",
        label = "Hardware & Building",
        tagline = "Tools, Paint & Construction",
        defaultThemeKey = "AMBER",
        trackExpiry = false,
        trackBatch = false,
        batchFieldLabel = "Lot / Shade",
        packSizeLabel = "Size / Gauge",
        productCategories = listOf("Power Tools", "Hand Tools", "Paints", "Plumbing", "Electrical", "Fasteners", "Other"),
        expenseCategories = listOf("Stock Purchase", "Godown Rent", "Freight & Loading", "Staff Salary", "Utilities", "Other"),
        paymentMethods = listOf("Cash", "Bank Transfer", "JazzCash / EasyPaisa", "Cheque"),
        pricingTierLabels = mapOf("MRP" to "MRP", "DISTRIBUTOR" to "Contractor", "WHOLESALER" to "Wholesaler", "RETAIL" to "Retail")
    );

    companion object {
        fun fromKey(key: String?): BusinessTypePreset =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: GENERAL
    }
}

object CustomizationJson {
    fun encodeList(items: List<String>): String {
        val arr = JSONArray()
        items.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { arr.put(it) }
        return arr.toString()
    }

    fun decodeList(json: String?, fallback: List<String>): List<String> {
        if (json.isNullOrBlank()) return fallback
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim()
                if (item.isNotEmpty()) result.add(item)
            }
            if (result.isEmpty()) fallback else result
        } catch (_: Exception) {
            fallback
        }
    }

    fun encodeTierLabels(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    fun decodeTierLabels(json: String?, fallback: Map<String, String>): Map<String, String> {
        if (json.isNullOrBlank()) return fallback
        return try {
            val obj = JSONObject(json)
            val out = fallback.toMutableMap()
            obj.keys().forEach { key ->
                val v = obj.optString(key).trim()
                if (v.isNotEmpty()) out[key] = v
            }
            out
        } catch (_: Exception) {
            fallback
        }
    }

    fun encodeTaxPresets(items: List<TaxPreset>): String {
        val arr = JSONArray()
        items.filter { it.name.isNotBlank() }.forEach { preset ->
            arr.put(
                JSONObject().apply {
                    put("name", preset.name.trim())
                    put("rate", preset.rate.coerceIn(0.0, 100.0))
                }
            )
        }
        return arr.toString()
    }

    fun decodeTaxPresets(json: String?): List<TaxPreset> {
        if (json.isNullOrBlank()) return TaxPreset.DEFAULT_LIST
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<TaxPreset>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name").trim()
                val rate = obj.optDouble("rate", 0.0).coerceIn(0.0, 100.0)
                if (name.isNotEmpty()) list.add(TaxPreset(name, rate))
            }
            if (list.isEmpty()) TaxPreset.DEFAULT_LIST else list
        } catch (_: Exception) {
            TaxPreset.DEFAULT_LIST
        }
    }

    fun encodePricingRules(rules: PricingMarginRules): String {
        return JSONObject().apply {
            put("ownerMarginMode", rules.ownerMarginMode)
            put("ownerMarginValue", rules.ownerMarginValue)
            put("distributorStep", rules.distributorStep)
            put("wholesalerStep", rules.wholesalerStep)
            put("retailStep", rules.retailStep)
            put("mrpStep", rules.mrpStep)
        }.toString()
    }

    fun decodePricingRules(json: String?): PricingMarginRules {
        if (json.isNullOrBlank()) return PricingMarginRules()
        return try {
            val obj = JSONObject(json)
            PricingMarginRules(
                ownerMarginMode = obj.optString("ownerMarginMode", "PERCENT").ifBlank { "PERCENT" },
                ownerMarginValue = obj.optDouble("ownerMarginValue", 5.0).coerceAtLeast(0.0),
                distributorStep = obj.optDouble("distributorStep", 0.0).coerceAtLeast(0.0),
                wholesalerStep = obj.optDouble("wholesalerStep", 5.0).coerceAtLeast(0.0),
                retailStep = obj.optDouble("retailStep", 12.0).coerceAtLeast(0.0),
                mrpStep = obj.optDouble("mrpStep", 20.0).coerceAtLeast(0.0)
            )
        } catch (_: Exception) {
            PricingMarginRules()
        }
    }
}

data class TaxPreset(
    val name: String,
    val rate: Double
) {
    val displayLabel: String
        get() = if (rate == 0.0) name else "$name (${if (rate % 1.0 == 0.0) rate.toInt() else rate}%)"

    companion object {
        val DEFAULT_LIST = listOf(
            TaxPreset("No Tax", 0.0),
            TaxPreset("GST", 18.0),
            TaxPreset("Sales Tax", 17.0),
            TaxPreset("WHT", 5.0)
        )
    }
}

data class PricingMarginRules(
    val ownerMarginMode: String = "PERCENT", // "PERCENT" or "FLAT"
    val ownerMarginValue: Double = 5.0,
    val distributorStep: Double = 0.0, // Extra % above owner's minimum floor price
    val wholesalerStep: Double = 5.0,
    val retailStep: Double = 12.0,
    val mrpStep: Double = 20.0
)

data class SuggestedTierPrices(
    val landedCost: Double,
    val baseFloorPrice: Double,
    val distributorPrice: Double,
    val wholesalerPrice: Double,
    val retailPrice: Double,
    val mrpPrice: Double
)

fun calculateSuggestedTierPrices(
    purchasePrice: Double,
    taxRatePercent: Double,
    taxIncludedInCost: Boolean,
    marginMode: String,
    marginValue: Double,
    rules: PricingMarginRules
): SuggestedTierPrices {
    val cleanCost = purchasePrice.coerceAtLeast(0.0)
    val taxRate = taxRatePercent.coerceIn(0.0, 100.0)
    val landed = if (taxIncludedInCost || taxRate <= 0.0) cleanCost else cleanCost * (1.0 + taxRate / 100.0)
    val baseFloor = if (marginMode.equals("FLAT", ignoreCase = true)) {
        landed + marginValue.coerceAtLeast(0.0)
    } else {
        landed * (1.0 + marginValue.coerceAtLeast(0.0) / 100.0)
    }
    fun round2(v: Double): Double = kotlin.math.round(v * 100.0) / 100.0

    val dist = round2(baseFloor * (1.0 + rules.distributorStep.coerceAtLeast(0.0) / 100.0))
    val whls = round2(baseFloor * (1.0 + rules.wholesalerStep.coerceAtLeast(0.0) / 100.0))
    val ret = round2(baseFloor * (1.0 + rules.retailStep.coerceAtLeast(0.0) / 100.0))
    val mrp = round2(baseFloor * (1.0 + rules.mrpStep.coerceAtLeast(0.0) / 100.0))

    return SuggestedTierPrices(
        landedCost = round2(landed),
        baseFloorPrice = round2(baseFloor),
        distributorPrice = dist,
        wholesalerPrice = whls,
        retailPrice = ret,
        mrpPrice = mrp
    )
}

