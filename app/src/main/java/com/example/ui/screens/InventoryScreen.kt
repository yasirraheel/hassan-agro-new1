package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ExpiryStatus
import com.example.data.local.PesticideCategory
import com.example.data.local.ProductEntity
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpiryBadge
import com.example.ui.components.FormScreenDialog
import com.example.ui.theme.ExpiredCrimson
import com.example.ui.theme.MonoCodeSmall
import com.example.ui.theme.MonoFigureMedium
import com.example.util.Formatters
import com.example.viewmodel.InventoryFilter
import java.util.concurrent.TimeUnit

@Composable
fun InventoryScreen(
    businessId: String,
    businessName: String,
    products: List<ProductEntity>,
    activeFilter: InventoryFilter,
    onFilterChange: (InventoryFilter) -> Unit,
    onRequestScan: () -> Unit,
    pendingNewProductBarcode: String?,
    pendingRestockProduct: ProductEntity?,
    onClearScanPrompts: () -> Unit,
    onSaveProduct: (ProductEntity) -> Unit,
    onRestockProduct: (String, Int, String, Long) -> Unit,
    onDeleteProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddProductSheet by remember { mutableStateOf(false) }
    var quickRestockTarget by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(products, searchQuery, activeFilter) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.brand.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true) ||
                p.batchNumber.contains(searchQuery, ignoreCase = true) ||
                p.category.contains(searchQuery, ignoreCase = true)
            val status = Formatters.expiryStatus(p.expiryDateMillis)
            val matchesFilter = when (activeFilter) {
                InventoryFilter.ALL -> true
                InventoryFilter.EXPIRING_SOON -> status == ExpiryStatus.EXPIRING_SOON
                InventoryFilter.EXPIRED -> status == ExpiryStatus.EXPIRED
                InventoryFilter.LOW_STOCK -> p.stockQty <= p.lowStockThreshold
            }
            matchesQuery && matchesFilter
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Compact Search + Matched Scan Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search product, batch, SKU...",
                    testTagName = "inventory_search_input",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRequestScan,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("inventory_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.cd_scan_barcode),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Scan", maxLines = 1, softWrap = false)
                }
            }

            // Clean Agri-Green Single-Line Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InventoryFilter.entries.forEach { filter ->
                    AgriFilterChip(
                        selected = activeFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = filter.label,
                        modifier = Modifier.testTag("inventory_filter_${filter.name.lowercase()}")
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Default.Inventory2,
                        title = if (products.isEmpty()) "No pesticide stock yet" else "No matching products",
                        subtitle = if (products.isEmpty())
                            "Scan any pesticide carton or tap Add Product to track batch numbers, expiry dates, and 4 pricing tiers for $businessName."
                        else
                            "Try clearing your search or switching expiry/stock filter chips.",
                        actionLabel = if (products.isEmpty()) "Add Pesticide Product" else null,
                        onAction = if (products.isEmpty()) { { showAddProductSheet = true } } else null,
                        actionTestTag = "inventory_empty_add_button"
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductStockCard(
                            product = product,
                            onEdit = { editingProduct = product },
                            onRestock = { quickRestockTarget = product },
                            onDelete = { onDeleteProduct(product.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddProductSheet = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("inventory_add_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_item))
        }
    }

    if (showAddProductSheet || editingProduct != null || pendingNewProductBarcode != null) {
        ProductEditorDialog(
            businessId = businessId,
            existing = editingProduct,
            prefilledBarcode = pendingNewProductBarcode,
            onSave = { saved ->
                onSaveProduct(saved)
                showAddProductSheet = false
                editingProduct = null
                onClearScanPrompts()
            },
            onDismiss = {
                showAddProductSheet = false
                editingProduct = null
                onClearScanPrompts()
            }
        )
    }

    val restockItem = quickRestockTarget ?: pendingRestockProduct
    if (restockItem != null) {
        QuickRestockDialog(
            product = restockItem,
            onConfirm = { qty, batch, expiry ->
                onRestockProduct(restockItem.id, qty, batch, expiry)
                quickRestockTarget = null
                onClearScanPrompts()
            },
            onDismiss = {
                quickRestockTarget = null
                onClearScanPrompts()
            }
        )
    }
}

@Composable
private fun ProductStockCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stockQty <= product.lowStockThreshold
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            append(product.name)
                            if (product.packSize.isNotBlank()) append(" (${product.packSize})")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(product.brand, product.category)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                ExpiryBadge(expiryMillis = product.expiryDateMillis)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (product.batchNumber.isNotBlank()) {
                        Text(
                            text = "Batch: ${product.batchNumber}",
                            style = MonoCodeSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "SKU: ${product.barcode}",
                            style = MonoCodeSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isLowStock) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = "Stock: ${product.stockQty}",
                        style = MonoFigureMedium,
                        color = if (isLowStock) ExpiredCrimson else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            // 4-Tier Pricing Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TierPricePill("MRP", product.mrpPrice, Modifier.weight(1f))
                TierPricePill("Dist", product.distributorPrice, Modifier.weight(1f))
                TierPricePill("Whls", product.wholesalerPrice, Modifier.weight(1f))
                TierPricePill("Retail", product.retailPrice, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRestock,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restock", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_item),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete_item),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TierPricePill(label: String, price: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = Formatters.formatPkr(price).removePrefix("Rs. "),
                style = MonoCodeSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProductEditorDialog(
    businessId: String,
    existing: ProductEntity?,
    prefilledBarcode: String?,
    onSave: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultExpiry = remember { System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365) }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var brand by remember { mutableStateOf(existing?.brand ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: PesticideCategory.INSECTICIDE.label) }
    var packSize by remember { mutableStateOf(existing?.packSize ?: "") }
    var barcode by remember { mutableStateOf(existing?.barcode ?: prefilledBarcode ?: "") }
    var batchNumber by remember { mutableStateOf(existing?.batchNumber ?: "") }
    var expiryMillis by remember { mutableLongStateOf(existing?.expiryDateMillis ?: defaultExpiry) }
    var stockQty by remember { mutableStateOf(existing?.stockQty?.toString() ?: "0") }
    var lowStock by remember { mutableStateOf(existing?.lowStockThreshold?.toString() ?: "10") }
    var purchasePrice by remember { mutableStateOf(existing?.purchasePrice?.takeIf { it > 0 }?.toString() ?: "") }
    var mrpPrice by remember { mutableStateOf(existing?.mrpPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var distPrice by remember { mutableStateOf(existing?.distributorPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var whlsPrice by remember { mutableStateOf(existing?.wholesalerPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var retailPrice by remember { mutableStateOf(existing?.retailPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    FormScreenDialog(
        title = if (existing == null) "Add Pesticide Product" else "Edit Product",
        subtitle = "Scroll freely — closes only when you tap X or Save",
        onDismiss = onDismiss,
        bottomBar = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Please enter the product name"
                        return@Button
                    }
                    val mrp = mrpPrice.toDoubleOrNull() ?: 0.0
                    val retail = retailPrice.toDoubleOrNull() ?: mrp
                    val whls = whlsPrice.toDoubleOrNull() ?: retail
                    val dist = distPrice.toDoubleOrNull() ?: whls
                    val entity = (existing ?: ProductEntity(
                        businessId = businessId,
                        name = name.trim(),
                        expiryDateMillis = expiryMillis
                    )).copy(
                        businessId = businessId,
                        name = name.trim(),
                        brand = brand.trim(),
                        category = category,
                        packSize = packSize.trim(),
                        barcode = barcode.trim(),
                        batchNumber = batchNumber.trim(),
                        expiryDateMillis = expiryMillis,
                        stockQty = stockQty.toIntOrNull() ?: 0,
                        lowStockThreshold = lowStock.toIntOrNull() ?: 10,
                        purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                        mrpPrice = mrp,
                        distributorPrice = dist,
                        wholesalerPrice = whls,
                        retailPrice = retail
                    )
                    onSave(entity)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_product_button")
            ) {
                Text(
                    text = if (existing == null) "Save to Inventory" else "Update Product",
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; validationError = null },
            label = { Text("Product Name *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("e.g. Lambda Cyhalothrin 2.5% EC", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_name_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = packSize,
                onValueChange = { packSize = it },
                label = { Text("Pack Size", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("500ml / 1L", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Text("Formulation Category", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PesticideCategory.entries.forEach { cat ->
                AgriFilterChip(
                    selected = category == cat.label,
                    onClick = { category = cat.label },
                    label = cat.label
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Barcode / SKU", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = batchNumber,
                onValueChange = { batchNumber = it },
                label = { Text("Batch No.", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Expiry Date Picker Row
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("product_expiry_date_button")
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Expiry Date: ${Formatters.formatDate(expiryMillis)}",
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Clean 2-column stock & cost rows (never cramped)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = stockQty,
                onValueChange = { stockQty = it.filter { ch -> ch.isDigit() } },
                label = { Text("Stock Qty", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lowStock,
                onValueChange = { lowStock = it.filter { ch -> ch.isDigit() } },
                label = { Text("Low Stock Alert", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = purchasePrice,
            onValueChange = { purchasePrice = it },
            label = { Text("Purchase / Cost Price (Rs.)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "4-Tier Selling Prices (PKR Rs.)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = mrpPrice,
                onValueChange = { mrpPrice = it },
                label = { Text("MRP Price", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("price_mrp_input")
            )
            OutlinedTextField(
                value = distPrice,
                onValueChange = { distPrice = it },
                label = { Text("Distributor", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("price_distributor_input")
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = whlsPrice,
                onValueChange = { whlsPrice = it },
                label = { Text("Wholesaler", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("price_wholesaler_input")
            )
            OutlinedTextField(
                value = retailPrice,
                onValueChange = { retailPrice = it },
                label = { Text("Retail Price", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("price_retail_input")
            )
        }

        if (validationError != null) {
            Text(
                text = validationError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialMillis = expiryMillis,
            onDateSelected = { expiryMillis = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun QuickRestockDialog(
    product: ProductEntity,
    onConfirm: (Int, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var addQty by remember { mutableStateOf("10") }
    var batch by remember { mutableStateOf(product.batchNumber) }
    var expiryMillis by remember { mutableLongStateOf(product.expiryDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    FormScreenDialog(
        title = "Quick Stock-In",
        subtitle = "${product.name} • Current stock: ${product.stockQty}",
        onDismiss = onDismiss,
        bottomBar = {
            Button(
                onClick = {
                    val qty = addQty.toIntOrNull() ?: 0
                    if (qty > 0) onConfirm(qty, batch.trim(), expiryMillis)
                },
                enabled = (addQty.toIntOrNull() ?: 0) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_restock_button")
            ) {
                Text("Add Stock", maxLines = 1, softWrap = false)
            }
        }
    ) {
        OutlinedTextField(
            value = addQty,
            onValueChange = { addQty = it.filter { c -> c.isDigit() } },
            label = { Text("Quantity to Add *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("restock_qty_input")
        )
        OutlinedTextField(
            value = batch,
            onValueChange = { batch = it },
            label = { Text("Batch Number", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Batch Expiry: ${Formatters.formatDate(expiryMillis)}",
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialMillis = expiryMillis,
            onDateSelected = { expiryMillis = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
