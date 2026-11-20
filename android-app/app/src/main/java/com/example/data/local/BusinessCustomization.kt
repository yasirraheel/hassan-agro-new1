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
}
