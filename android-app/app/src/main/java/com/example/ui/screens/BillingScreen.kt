package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpiryStatus
import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.PricingTier
import com.example.data.local.ProductEntity
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpiryBadge
import com.example.ui.components.PerformedByPill
import com.example.ui.components.ProofImageViewerDialog
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoCodeSmall
import com.example.ui.theme.MonoFigureMedium
import com.example.util.Formatters
import com.example.util.InvoicePdfStyle
import com.example.viewmodel.BillingDraftState
import com.example.viewmodel.GeneratedInvoiceEvent
import com.example.viewmodel.HeldCart
import java.io.File

private enum class SalesFilter(val label: String) {
    ALL("All"),
    PAID("Paid"),
    CREDIT("Credit / Partial"),
    RETURNED("Returned")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    activeBusiness: BusinessEntity,
    products: List<ProductEntity>,
    invoices: List<InvoiceWithItems>,
    customers: List<CustomerEntity>,
    draft: BillingDraftState,
    heldCarts: List<HeldCart>,
    generatedInvoice: GeneratedInvoiceEvent?,
    canCreateSale: Boolean,
    canCollectCredit: Boolean,
    canReturnSale: Boolean,
    onSelectSavedCustomer: (CustomerEntity?) -> Unit,
    onToggleSaveCustomer: (Boolean) -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onUpdateCustomer: (String, String, String) -> Unit,
    onSelectTier: (PricingTier) -> Unit,
    onUpdateTax: (Boolean, String) -> Unit,
    onUpdateDiscount: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdatePayment: (String, Boolean, String) -> Unit,
    onAddProductToCart: (ProductEntity) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onUpdateCartPrice: (String, Double) -> Unit,
    onAttachProofImage: (Uri) -> Unit,
    onRemoveProofImage: () -> Unit,
    onClearCart: () -> Unit,
    onHoldCart: () -> Unit,
    onResumeHeldCart: (String) -> Unit,
    onDiscardHeldCart: (String) -> Unit,
    onRequestScanToCart: () -> Unit,
    onFinalizeInvoice: () -> Unit,
    onOpenPastInvoice: (InvoiceWithItems) -> Unit,
    onSwitchInvoiceStyle: (InvoicePdfStyle) -> Unit,
    onRecordPayment: (String, Double) -> Unit,
    onReturnInvoice: (String) -> Unit,
    onGoToInventory: () -> Unit,
    onDismissGeneratedInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val currency = activeBusiness.resolvedCurrency()
    val customCategories = remember(activeBusiness.productCategoriesJson, activeBusiness.businessType) {
        activeBusiness.productCategories()
    }

    var selectedTab by remember(canCreateSale) { mutableIntStateOf(if (canCreateSale) 0 else 1) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var salesFilter by remember { mutableStateOf(SalesFilter.ALL) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showHeldCartsSheet by remember { mutableStateOf(false) }
    var showPosCustomerPicker by remember { mutableStateOf(false) }
    var quickCreateCustomerPrefill by remember { mutableStateOf<Pair<String, String>?>(null) }
    var paymentTargetInvoice by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var returnTargetInvoice by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var viewingProofInvoice by remember { mutableStateOf<InvoiceWithItems?>(null) }

    val selectedPosCustomer = remember(draft.customerId, customers) {
        customers.firstOrNull { it.id == draft.customerId }
    }

    val cartQtyByProductId = remember(draft.cartLines) {
        draft.cartLines.associate { it.product.id to it.quantity }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.brand.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || p.category.equals(selectedCategory, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }

    val todayStart = remember { Formatters.todayStartMillis() }
    val todaySales = remember(invoices) {
        invoices.filter { !it.invoice.isReturned && it.invoice.createdAt >= todayStart }
            .sumOf { it.invoice.grandTotal }
    }
    val todayCollected = remember(invoices) {
        invoices.filter { !it.invoice.isReturned && it.invoice.createdAt >= todayStart }
            .sumOf { it.invoice.amountPaid }
    }
    val pendingCreditTotal = remember(invoices) {
        invoices.filter { !it.invoice.isReturned }.sumOf { it.invoice.balanceDue }
    }

    val filteredInvoices = remember(invoices, salesFilter) {
        invoices.filter { entry ->
            val status = InvoicePaymentStatus.fromName(entry.invoice.paymentStatus)
            when (salesFilter) {
                SalesFilter.ALL -> true
                SalesFilter.PAID -> !entry.invoice.isReturned && status == InvoicePaymentStatus.PAID
                SalesFilter.CREDIT -> !entry.invoice.isReturned && (status == InvoicePaymentStatus.CREDIT || status == InvoicePaymentStatus.PARTIAL)
                SalesFilter.RETURNED -> entry.invoice.isReturned || status == InvoicePaymentStatus.RETURNED
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
        ) {
            if (canCreateSale) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(0, 3),
                        modifier = Modifier.testTag("billing_tab_new")
                    ) { Text("POS", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 3),
                        modifier = Modifier.testTag("billing_tab_history")
                    ) { Text("Sales (${invoices.size})", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    SegmentedButton(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(2, 3),
                        modifier = Modifier.testTag("billing_tab_customers")
                    ) { Text("Customers (${customers.size})", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                }
            } else {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("billing_tab_history")
                    ) { Text("Sales (${invoices.size})", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    SegmentedButton(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("billing_tab_customers")
                    ) { Text("Customers (${customers.size})", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                }
            }

            if (selectedTab == 0 && canCreateSale) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactSearchBar(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search or scan product...",
                            testTagName = "pos_search_input",
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onRequestScanToCart,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("billing_scan_product_button")
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(R.string.cd_scan_barcode),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("Scan", maxLines = 1, softWrap = false)
                        }
                    }

                    // Active POS Customer Strip: Pick saved customer OR create a new customer on the fly
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (selectedPosCustomer != null) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selectedPosCustomer != null) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPosCustomerPicker = true }
                                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                                .testTag("pos_counter_customer_bar"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedPosCustomer?.name
                                        ?: draft.customerName.trim().ifBlank { "Walk-in Customer" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (selectedPosCustomer != null) {
                                        listOf(
                                            selectedPosCustomer.phone.ifBlank { "Saved customer" },
                                            activeBusiness.tierLabel(PricingTier.fromName(selectedPosCustomer.pricingTier))
                                        ).joinToString(" • ")
                                    } else {
                                        "Tap to select customer (${customers.size}) or add new"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (selectedPosCustomer != null) {
                                IconButton(
                                    onClick = { onSelectSavedCustomer(null) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("pos_counter_clear_customer_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear selected customer",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { quickCreateCustomerPrefill = "" to "" },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("pos_counter_new_customer_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    // Active 4-Tier Pricing Switcher + Parked Carts Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PricingTier.entries.forEach { tier ->
                            AgriFilterChip(
                                selected = draft.pricingTier == tier,
                                onClick = { onSelectTier(tier) },
                                label = activeBusiness.tierLabel(tier),
                                modifier = Modifier.testTag("tier_chip_${tier.name.lowercase()}")
                            )
                        }
                        if (heldCarts.isNotEmpty()) {
                            AgriFilterChip(
                                selected = true,
                                onClick = { showHeldCartsSheet = true },
                                label = "Held (${heldCarts.size})",
                                leadingIcon = {
                                    Icon(Icons.Default.PauseCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("pos_held_carts_chip")
                            )
                        }
                    }

                    // Dynamic Per-Business Category Filter Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AgriFilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = "All Items"
                        )
                        customCategories.forEach { cat ->
                            AgriFilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = cat
                            )
                        }
                    }

                    if (products.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Default.PointOfSale,
                                title = "Add products to start selling on POS",
                                subtitle = "Add or scan stock for ${activeBusiness.name} first. Products appear here as 1-tap POS tiles with multi-tier pricing.",
                                actionLabel = "Open Stock & Add Products",
                                onAction = onGoToInventory,
                                actionTestTag = "pos_empty_go_stock_btn"
                            )
                        }
                    } else if (filteredProducts.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Default.Search,
                                title = "No matching items",
                                subtitle = "Try a different product name, barcode, or category."
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 158.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                val qtyInCart = cartQtyByProductId[product.id] ?: 0
                                PosProductTile(
                                    business = activeBusiness,
                                    product = product,
                                    pricingTier = draft.pricingTier,
                                    qtyInCart = qtyInCart,
                                    onTapAdd = { onAddProductToCart(product) },
                                    onQtyChange = { onUpdateCartQty(product.id, it) }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = draft.cartLines.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onClearCart,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("pos_clear_cart_button")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear cart", tint = Color.White)
                                }
                                IconButton(
                                    onClick = onHoldCart,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("pos_hold_cart_button")
                                ) {
                                    Icon(Icons.Default.PauseCircleOutline, contentDescription = "Hold cart", tint = Color.White)
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${draft.totalItemCount} items • ${activeBusiness.tierLabel(draft.pricingTier)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = Formatters.formatPkr(draft.grandTotal, currency),
                                        style = MonoFigureMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = { showCheckoutSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("pos_checkout_bar_button")
                                ) {
                                    Text("Checkout", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SalesMetricChip("Today Sales", Formatters.formatCompactPkr(todaySales, currency), semantic.positiveFg, Modifier.weight(1f))
                        SalesMetricChip("Collected", Formatters.formatCompactPkr(todayCollected, currency), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                        SalesMetricChip("Credit Due", Formatters.formatCompactPkr(pendingCreditTotal, currency), semantic.warningFg, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SalesFilter.entries.forEach { filter ->
                            AgriFilterChip(
                                selected = salesFilter == filter,
                                onClick = { salesFilter = filter },
                                label = filter.label
                            )
                        }
                    }

                    if (filteredInvoices.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Default.ReceiptLong,
                                title = "No sales bills found",
                                subtitle = "Tap items in the POS Counter tab to complete a sale, auto-deduct stock, and generate branded invoices.",
                                actionLabel = if (canCreateSale) "Open POS Counter" else null,
                                onAction = if (canCreateSale) { { selectedTab = 0 } } else null,
                                actionTestTag = "history_empty_new_invoice_btn"
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredInvoices, key = { it.invoice.id }) { entry ->
                                SalesInvoiceCard(
                                    business = activeBusiness,
                                    entry = entry,
                                    canCollectCredit = canCollectCredit,
                                    canReturnSale = canReturnSale,
                                    onSharePdf = { onOpenPastInvoice(entry) },
                                    onViewProof = { viewingProofInvoice = entry },
                                    onCollectPayment = { paymentTargetInvoice = entry },
                                    onReturnRestock = { returnTargetInvoice = entry }
                                )
                            }
                        }
                    }
                }
            } else {
                CustomersTabContent(
                    activeBusiness = activeBusiness,
                    customers = customers,
                    invoices = invoices,
                    canCreateSale = canCreateSale,
                    onBillCustomer = { customer ->
                        onSelectSavedCustomer(customer)
                        if (canCreateSale) selectedTab = 0
                    },
                    onSaveCustomer = onSaveCustomer,
                    onDeleteCustomer = onDeleteCustomer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showCheckoutSheet) {
        PosCheckoutSheet(
            business = activeBusiness,
            draft = draft,
            customers = customers,
            invoices = invoices,
            onSelectSavedCustomer = onSelectSavedCustomer,
            onSaveCustomer = onSaveCustomer,
            onToggleSaveCustomer = onToggleSaveCustomer,
            onUpdateCustomer = onUpdateCustomer,
            onSelectTier = onSelectTier,
            onUpdateTax = onUpdateTax,
            onUpdateDiscount = onUpdateDiscount,
            onUpdateNotes = onUpdateNotes,
            onUpdatePayment = onUpdatePayment,
            onUpdateCartQty = onUpdateCartQty,
            onUpdateCartPrice = onUpdateCartPrice,
            onAttachProofImage = onAttachProofImage,
            onRemoveProofImage = onRemoveProofImage,
            onHoldCart = onHoldCart,
            onCompleteSale = onFinalizeInvoice,
            onDismiss = { showCheckoutSheet = false }
        )
    }

    if (showPosCustomerPicker) {
        CustomerPickerBottomSheet(
            business = activeBusiness,
            customers = customers,
            invoices = invoices,
            selectedCustomerId = draft.customerId,
            onPick = { customer ->
                onSelectSavedCustomer(customer)
                showPosCustomerPicker = false
            },
            onSelectWalkIn = {
                onSelectSavedCustomer(null)
                showPosCustomerPicker = false
            },
            onCreateNewCustomer = { prefillName, prefillPhone ->
                showPosCustomerPicker = false
                quickCreateCustomerPrefill = prefillName to prefillPhone
            },
            onDismiss = { showPosCustomerPicker = false }
        )
    }

    quickCreateCustomerPrefill?.let { (prefillName, prefillPhone) ->
        CustomerEditorDialog(
            business = activeBusiness,
            existing = null,
            initialName = prefillName,
            initialPhone = prefillPhone,
            initialTier = draft.pricingTier,
            onSave = { newCustomer ->
                onSaveCustomer(newCustomer)
                onSelectSavedCustomer(newCustomer)
                quickCreateCustomerPrefill = null
            },
            onDismiss = { quickCreateCustomerPrefill = null }
        )
    }

    if (showHeldCartsSheet) {
        HeldCartsSheet(
            currencySymbol = currency,
            heldCarts = heldCarts,
            onResume = onResumeHeldCart,
            onDiscard = onDiscardHeldCart,
            onDismiss = { showHeldCartsSheet = false }
        )
    }

    paymentTargetInvoice?.let { target ->
        ReceivePaymentDialog(
            currencySymbol = currency,
            invoiceWithItems = target,
            onConfirm = { amt -> onRecordPayment(target.invoice.id, amt) },
            onDismiss = { paymentTargetInvoice = null }
        )
    }

    returnTargetInvoice?.let { target ->
        ReturnSaleConfirmDialog(
            currencySymbol = currency,
            invoiceWithItems = target,
            onConfirm = { onReturnInvoice(target.invoice.id) },
            onDismiss = { returnTargetInvoice = null }
        )
    }

    viewingProofInvoice?.let { entry ->
        val path = entry.invoice.proofImagePath
        if (!path.isNullOrBlank() && File(path).exists()) {
            ProofImageViewerDialog(
                title = "Payment Proof • #${entry.invoice.invoiceNumber}",
                imagePath = path,
                onDismiss = { viewingProofInvoice = null }
            )
        } else {
            viewingProofInvoice = null
        }
    }

    if (generatedInvoice != null) {
        InvoiceShareDialog(
            event = generatedInvoice,
            onSwitchStyle = onSwitchInvoiceStyle,
            onDismiss = onDismissGeneratedInvoice
        )
    }
}

@Composable
private fun PosProductTile(
    business: BusinessEntity,
    product: ProductEntity,
    pricingTier: PricingTier,
    qtyInCart: Int,
    onTapAdd: () -> Unit,
    onQtyChange: (Int) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val isExpired = business.trackExpiry && Formatters.expiryStatus(product.expiryDateMillis) == ExpiryStatus.EXPIRED
    val outOfStock = product.stockQty <= 0
    val tierPrice = product.priceForTier(pricingTier)
    val currency = business.resolvedCurrency()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_tile_${product.id}")
            .clickable(enabled = !outOfStock, onClick = onTapAdd),
        shape = MaterialTheme.shapes.large,
        color = if (qtyInCart > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (qtyInCart > 0) 1.5.dp else 1.dp,
            color = when {
                isExpired -> semantic.negativeFg.copy(alpha = 0.7f)
                qtyInCart > 0 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (outOfStock) semantic.negativeBg else semantic.positiveBg
                ) {
                    Text(
                        text = if (outOfStock) "Out of Stock" else "${product.stockQty} left",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (outOfStock) semantic.negativeFg else semantic.positiveFg,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (qtyInCart > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "×$qtyInCart",
                            style = MonoCodeSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Text(
                text = listOf(product.packSize, product.category).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (business.trackExpiry) {
                ExpiryBadge(expiryMillis = product.expiryDateMillis)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Formatters.formatPkr(tierPrice, currency),
                    style = MonoFigureMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (qtyInCart > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onQtyChange(qtyInCart - 1) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Remove one", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onTapAdd() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add one", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesMetricChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Text(value, style = MonoCodeSmall, color = accent, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SalesInvoiceCard(
    business: BusinessEntity,
    entry: InvoiceWithItems,
    canCollectCredit: Boolean,
    canReturnSale: Boolean,
    onSharePdf: () -> Unit,
    onViewProof: () -> Unit,
    onCollectPayment: () -> Unit,
    onReturnRestock: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val inv = entry.invoice
    val currency = business.resolvedCurrency()
    val status = InvoicePaymentStatus.fromName(inv.paymentStatus)
    val hasProofImage = remember(inv.proofImagePath) {
        !inv.proofImagePath.isNullOrBlank() && File(inv.proofImagePath).exists()
    }
    val (statusBg, statusFg) = when {
        inv.isReturned || status == InvoicePaymentStatus.RETURNED -> semantic.negativeBg to semantic.negativeFg
        status == InvoicePaymentStatus.PAID -> semantic.positiveBg to semantic.positiveFg
        else -> semantic.warningBg to semantic.warningFg
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(inv.invoiceNumber, style = MonoCodeSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1)
                        Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusFg,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                text = business.tierLabel(PricingTier.fromName(inv.pricingTier)),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(inv.customerName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${entry.items.sumOf { it.quantity }} units • ${inv.paymentMethod} • ${Formatters.formatDateTime(inv.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    PerformedByPill(name = inv.performedByName)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatPkr(inv.grandTotal, currency),
                        style = MonoFigureMedium,
                        color = if (inv.isReturned) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (!inv.isReturned && inv.balanceDue > 0.01) {
                        Text(
                            text = "Due: ${Formatters.formatPkr(inv.balanceDue, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.warningFg,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSharePdf,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF / Share", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }
                if (hasProofImage) {
                    OutlinedButton(
                        onClick = onViewProof,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("invoice_view_proof_${inv.id}")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Proof", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }
                }
                if (canCollectCredit && !inv.isReturned && inv.balanceDue > 0.01) {
                    Button(
                        onClick = onCollectPayment,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Collect", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }
                }
                if (canReturnSale && !inv.isReturned) {
                    OutlinedButton(
                        onClick = onReturnRestock,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Return", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
