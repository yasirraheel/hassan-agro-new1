package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpiryStatus
import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.PricingTier
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.FormScreenDialog
import com.example.ui.components.PerformedByPill
import com.example.ui.theme.ExpiredCrimson
import com.example.ui.theme.ExpiringSoonAmber
import com.example.ui.theme.ExpiringSoonBg
import com.example.ui.theme.MonoCodeSmall
import com.example.ui.theme.MonoFigureLarge
import com.example.ui.theme.MonoFigureMedium
import com.example.ui.theme.SafeStockBg
import com.example.ui.theme.SafeStockGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.Formatters
import com.example.util.InvoicePdfStyle
import com.example.util.ShareHelper
import com.example.viewmodel.BillingCartLine
import com.example.viewmodel.BillingDraftState
import com.example.viewmodel.GeneratedInvoiceEvent
import com.example.viewmodel.HeldCart
import kotlin.math.ceil

@Composable
fun PosCheckoutSheet(
    business: BusinessEntity,
    draft: BillingDraftState,
    customers: List<CustomerEntity>,
    invoices: List<InvoiceWithItems>,
    onSelectSavedCustomer: (CustomerEntity?) -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onToggleSaveCustomer: (Boolean) -> Unit,
    onUpdateCustomer: (String, String, String) -> Unit,
    onSelectTier: (PricingTier) -> Unit,
    onUpdateTax: (Boolean, String) -> Unit,
    onUpdateDiscount: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdatePayment: (String, Boolean, String) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onUpdateCartPrice: (String, Double) -> Unit,
    onHoldCart: () -> Unit,
    onCompleteSale: () -> Unit,
    onDismiss: () -> Unit
) {
    val currency = business.resolvedCurrency()
    val paymentMethods = remember(business.paymentMethodsJson, business.businessType) {
        business.paymentMethods()
    }
    val taxPresets = remember(business.taxPresetsJson) {
        business.taxPresets()
    }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var quickCreatePrefill by remember { mutableStateOf<Pair<String, String>?>(null) }

    val selectedCustomer = remember(draft.customerId, customers) {
        customers.firstOrNull { it.id == draft.customerId }
    }

    FormScreenDialog(
        title = "POS Checkout",
        subtitle = "${draft.totalItemCount} units • ${business.tierLabel(draft.pricingTier)} pricing",
        onDismiss = onDismiss,
        topActions = {
            OutlinedButton(
                onClick = {
                    onHoldCart()
                    onDismiss()
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(36.dp)
                    .padding(end = 6.dp)
                    .testTag("checkout_hold_button")
            ) {
                Icon(Icons.Default.PauseCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Hold", maxLines = 1, softWrap = false)
            }
        },
        bottomBar = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Grand Total", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = Formatters.formatPkr(draft.grandTotal, currency),
                        style = MonoFigureLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = {
                        onCompleteSale()
                        onDismiss()
                    },
                    enabled = draft.cartLines.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_invoice_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Complete Sale & Generate Receipt", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    ) {
        draft.cartLines.forEach { line ->
            PosCartLineRow(
                business = business,
                line = line,
                onQtyChange = { onUpdateCartQty(line.product.id, it) },
                onUnitPriceChange = { onUpdateCartPrice(line.product.id, it) }
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PricingTier.entries.forEach { tier ->
                AgriFilterChip(
                    selected = draft.pricingTier == tier,
                    onClick = { onSelectTier(tier) },
                    label = business.tierLabel(tier)
                )
            }
        }

        // Saved Customer Picker Bar
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomerPicker = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("pos_select_saved_customer_btn"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (selectedCustomer != null) "Saved Customer: ${selectedCustomer.name}" else "Select Saved Customer",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedCustomer != null)
                                "${selectedCustomer.phone.ifBlank { "No phone" }} • ${business.tierLabel(PricingTier.fromName(selectedCustomer.pricingTier))} tier"
                            else
                                "${customers.size} saved customers • auto-fills phone & tier",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (selectedCustomer != null) {
                    IconButton(
                        onClick = { onSelectSavedCustomer(null) },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("pos_checkout_unlink_customer_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Unlink customer", modifier = Modifier.size(18.dp))
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Pick",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = {
                                quickCreatePrefill = draft.customerName.trim() to draft.customerPhone.trim()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("pos_checkout_new_customer_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = draft.customerName,
            onValueChange = { onUpdateCustomer(it, draft.customerPhone, draft.customerEmail) },
            label = { Text("Customer / Dealer Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("Walk-in Customer", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("billing_customer_name_input")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = draft.customerPhone,
                onValueChange = { onUpdateCustomer(draft.customerName, it, draft.customerEmail) },
                label = { Text("Phone / WhatsApp", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = draft.customerEmail,
                onValueChange = { onUpdateCustomer(draft.customerName, draft.customerPhone, it) },
                label = { Text("Email (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        if (draft.customerId.isBlank() && draft.customerName.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSaveCustomer(!draft.saveCustomerToDirectory) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = draft.saveCustomerToDirectory,
                    onCheckedChange = onToggleSaveCustomer,
                    modifier = Modifier.testTag("pos_save_new_customer_checkbox")
                )
                Text(
                    text = "Save \"${draft.customerName.trim()}\" to Customer Directory",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Apply Sales Tax / GST", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = draft.taxEnabled,
                        onCheckedChange = { enabled ->
                            val firstNonZero = taxPresets.firstOrNull { it.rate > 0.0 }?.rate ?: 18.0
                            val rate = if (enabled && (draft.taxRateText.toDoubleOrNull() ?: 0.0) == 0.0) {
                                Formatters.formatDecimal(firstNonZero)
                            } else draft.taxRateText
                            onUpdateTax(enabled, rate)
                        },
                        modifier = Modifier.testTag("billing_tax_switch")
                    )
                }
                AnimatedVisibility(visible = draft.taxEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (taxPresets.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                taxPresets.filter { it.rate > 0.0 }.forEach { preset ->
                                    val presetRateStr = Formatters.formatDecimal(preset.rate)
                                    AgriFilterChip(
                                        selected = (draft.taxRateText.toDoubleOrNull() ?: -1.0) == preset.rate,
                                        onClick = { onUpdateTax(true, presetRateStr) },
                                        label = preset.displayLabel
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = draft.taxRateText,
                            onValueChange = { onUpdateTax(true, it) },
                            label = { Text("Tax Rate (%)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("billing_tax_rate_input")
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draft.discountText,
                        onValueChange = onUpdateDiscount,
                        label = { Text("Discount ($currency)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = onUpdateNotes,
                        label = { Text("Bill Note", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Payment Method", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    paymentMethods.forEach { method ->
                        AgriFilterChip(
                            selected = !draft.isCreditSale && draft.paymentMethod == method,
                            onClick = { onUpdatePayment(method, false, draft.amountReceivedText) },
                            label = method
                        )
                    }
                    AgriFilterChip(
                        selected = draft.isCreditSale,
                        onClick = { onUpdatePayment("Credit (Udhaar)", true, "0") },
                        label = "Credit (Udhaar)",
                        modifier = Modifier.testTag("pos_pay_credit")
                    )
                }

                OutlinedTextField(
                    value = draft.amountReceivedText,
                    onValueChange = { onUpdatePayment(draft.paymentMethod, draft.isCreditSale, it) },
                    label = {
                        Text(
                            text = if (draft.isCreditSale) "Advance Paid ($currency)" else "Cash Received ($currency)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = { Text(Formatters.formatPkr(draft.grandTotal, currency).removePrefix("$currency "), maxLines = 1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_amount_received_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val exactVal = ceil(draft.grandTotal).toInt()
                    val presets = listOf(
                        "Exact" to exactVal.toString(),
                        "$currency 1,000" to "1000",
                        "$currency 5,000" to "5000",
                        "$currency 10,000" to "10000",
                        "0 Credit" to "0"
                    )
                    presets.forEach { (label, value) ->
                        OutlinedButton(
                            onClick = {
                                val isCred = value == "0"
                                onUpdatePayment(
                                    if (isCred) "Credit (Udhaar)" else draft.paymentMethod,
                                    isCred,
                                    value
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (draft.changeToReturn > 0.01) {
                    Surface(shape = RoundedCornerShape(8.dp), color = SafeStockBg) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Change to Return", style = MaterialTheme.typography.labelLarge, color = SafeStockGreen)
                            Text(Formatters.formatPkr(draft.changeToReturn, currency), style = MonoFigureMedium, color = SafeStockGreen)
                        }
                    }
                } else if (draft.balanceDue > 0.01) {
                    Surface(shape = RoundedCornerShape(8.dp), color = ExpiringSoonBg) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Credit", style = MaterialTheme.typography.labelLarge, color = ExpiringSoonAmber)
                            Text(Formatters.formatPkr(draft.balanceDue, currency), style = MonoFigureMedium, color = ExpiringSoonAmber)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CheckoutSummaryRow("Subtotal", Formatters.formatPkr(draft.subtotal, currency))
        if (draft.discountAmount > 0.0) {
            CheckoutSummaryRow("Discount", "-${Formatters.formatPkr(draft.discountAmount, currency)}")
        }
        if (draft.taxEnabled && draft.taxRatePercent > 0.0) {
            CheckoutSummaryRow("Tax (${draft.taxRatePercent}%)", Formatters.formatPkr(draft.taxAmount, currency))
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showCustomerPicker) {
        CustomerPickerBottomSheet(
            business = business,
            customers = customers,
            invoices = invoices,
            selectedCustomerId = draft.customerId,
            onPick = { customer ->
                onSelectSavedCustomer(customer)
                showCustomerPicker = false
            },
            onSelectWalkIn = {
                onSelectSavedCustomer(null)
                showCustomerPicker = false
            },
            onCreateNewCustomer = { prefillName, prefillPhone ->
                showCustomerPicker = false
                quickCreatePrefill = prefillName to prefillPhone
            },
            onDismiss = { showCustomerPicker = false }
        )
    }

    quickCreatePrefill?.let { (prefillName, prefillPhone) ->
        CustomerEditorDialog(
            business = business,
            existing = null,
            initialName = prefillName,
            initialPhone = prefillPhone,
            initialTier = draft.pricingTier,
            onSave = { newCustomer ->
                onSaveCustomer(newCustomer)
                onSelectSavedCustomer(newCustomer)
                quickCreatePrefill = null
            },
            onDismiss = { quickCreatePrefill = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerBottomSheet(
    business: BusinessEntity,
    customers: List<CustomerEntity>,
    invoices: List<InvoiceWithItems>,
    selectedCustomerId: String,
    onPick: (CustomerEntity) -> Unit,
    onCreateNewCustomer: (initialName: String, initialPhone: String) -> Unit,
    onSelectWalkIn: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currency = business.resolvedCurrency()
    var query by remember { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val queryLooksLikePhone = remember(trimmedQuery) {
        trimmedQuery.isNotEmpty() &&
            trimmedQuery.any { it.isDigit() } &&
            trimmedQuery.all { it.isDigit() || it in "+- ()" }
    }
    val filtered = remember(customers, query) {
        customers.filter { c ->
            query.isBlank() ||
                c.name.contains(query, ignoreCase = true) ||
                c.phone.contains(query, ignoreCase = true)
        }
    }
    val hasExactMatch = remember(customers, trimmedQuery) {
        trimmedQuery.isNotEmpty() && customers.any {
            it.name.equals(trimmedQuery, ignoreCase = true) ||
                it.phone.equals(trimmedQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Select Customer",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (queryLooksLikePhone) onCreateNewCustomer("", trimmedQuery)
                        else onCreateNewCustomer(trimmedQuery, "")
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("customer_picker_add_new_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("New Customer", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
            }

            CompactSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search name or phone...",
                testTagName = "customer_picker_search"
            )

            // Instant inline "Create from search" card whenever typed query doesn't match an existing customer
            if (trimmedQuery.isNotEmpty() && !hasExactMatch) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (queryLooksLikePhone) onCreateNewCustomer("", trimmedQuery)
                            else onCreateNewCustomer(trimmedQuery, "")
                        }
                        .testTag("customer_picker_create_from_query_btn"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create \"$trimmedQuery\"",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Save to directory & select for this POS sale",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (selectedCustomerId.isNotBlank() && onSelectWalkIn != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectWalkIn)
                        .testTag("customer_picker_walk_in_btn"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Switch back to Walk-in Customer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear selected customer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (customers.isEmpty()) "No saved customers yet. Tap \"New Customer\" above to create and select one right now without leaving your POS sale."
                        else "No saved customers match \"$trimmedQuery\". Tap the card above to create them now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { customer ->
                        val (_, due) = remember(customer, invoices) { computeCustomerStats(customer, invoices) }
                        val isSelected = customer.id == selectedCustomerId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(customer) }
                                .testTag("customer_picker_item_${customer.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = listOf(
                                            customer.phone.ifBlank { "No phone" },
                                            business.tierLabel(PricingTier.fromName(customer.pricingTier))
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (due > 0.01) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = ExpiringSoonBg) {
                                        Text(
                                            text = "Due ${Formatters.formatCompactPkr(due, currency)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ExpiringSoonAmber,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosCartLineRow(
    business: BusinessEntity,
    line: BillingCartLine,
    onQtyChange: (Int) -> Unit,
    onUnitPriceChange: (Double) -> Unit
) {
    val isExpired = business.trackExpiry && Formatters.expiryStatus(line.product.expiryDateMillis) == ExpiryStatus.EXPIRED
    val currency = business.resolvedCurrency()
    var rateText by remember(line.product.id, line.unitPrice) { mutableStateOf(line.unitPrice.toString()) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isExpired) ExpiredCrimson else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.product.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append("Stock: ${line.product.stockQty}")
                            if (business.trackBatch && line.product.batchNumber.isNotBlank()) {
                                append(" • ${business.batchFieldLabel}: ${line.product.batchNumber}")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExpired) ExpiredCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { onQtyChange(0) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onQtyChange(line.quantity - 1) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(18.dp))
                    }
                    Text("${line.quantity}", style = MonoFigureMedium, modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(onClick = { onQtyChange(line.quantity + 1) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase quantity", modifier = Modifier.size(18.dp))
                    }
                }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it
                        it.toDoubleOrNull()?.let(onUnitPriceChange)
                    },
                    label = { Text("Rate", maxLines = 1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.width(105.dp)
                )
                Text(
                    text = Formatters.formatPkr(line.lineTotal, currency),
                    style = MonoFigureMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun CheckoutSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MonoFigureMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeldCartsSheet(
    currencySymbol: String,
    heldCarts: List<HeldCart>,
    onResume: (String) -> Unit,
    onDiscard: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Held / Parked Carts", style = MaterialTheme.typography.titleLarge)
            if (heldCarts.isEmpty()) {
                Text(
                    text = "No parked carts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                heldCarts.forEach { held ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(held.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${held.draft.totalItemCount} items • ${Formatters.formatPkr(held.draft.grandTotal, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Button(
                                onClick = {
                                    onResume(held.id)
                                    onDismiss()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Resume", maxLines = 1, softWrap = false)
                            }
                            IconButton(onClick = { onDiscard(held.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Discard cart", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceivePaymentDialog(
    currencySymbol: String,
    invoiceWithItems: InvoiceWithItems,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val inv = invoiceWithItems.invoice
    var amountText by remember { mutableStateOf(inv.balanceDue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collect Credit Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${inv.invoiceNumber} • ${inv.customerName}\nRemaining Balance: ${Formatters.formatPkr(inv.balanceDue, currencySymbol)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Received ($currencySymbol)", maxLines = 1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collect_payment_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onConfirm(amt)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_collect_payment_btn")
            ) { Text("Record Payment", maxLines = 1, softWrap = false) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReturnSaleConfirmDialog(
    currencySymbol: String,
    invoiceWithItems: InvoiceWithItems,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val inv = invoiceWithItems.invoice
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return Sale & Restock?") },
        text = {
            Text(
                text = "Mark invoice #${inv.invoiceNumber} (${Formatters.formatPkr(inv.grandTotal, currencySymbol)}) as Returned and restore all ${invoiceWithItems.items.sumOf { it.quantity }} sold units back into inventory stock?"
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpiredCrimson)
            ) { Text("Return & Restock", maxLines = 1, softWrap = false) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceShareDialog(
    event: GeneratedInvoiceEvent,
    onSwitchStyle: (InvoicePdfStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val inv = event.invoiceWithItems.invoice
    val biz = event.business
    val currency = biz.resolvedCurrency()
    val caption = "Invoice #${inv.invoiceNumber} from ${biz.name} — Total: ${Formatters.formatPkr(inv.grandTotal, currency)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (event.style == InvoicePdfStyle.THERMAL_COUPON) Icons.Default.Receipt else Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Receipt & Invoice Ready", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Style Toggle: 80mm Thermal Coupon vs Ultra-Minimal Compact A4 PDF
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = event.style == InvoicePdfStyle.THERMAL_COUPON,
                        onClick = { onSwitchStyle(InvoicePdfStyle.THERMAL_COUPON) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("invoice_style_thermal")
                    ) {
                        Text("Thermal Receipt", maxLines = 1, softWrap = false, fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = event.style == InvoicePdfStyle.MINIMAL_A4,
                        onClick = { onSwitchStyle(InvoicePdfStyle.MINIMAL_A4) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("invoice_style_minimal_a4")
                    ) {
                        Text("Minimal PDF", maxLines = 1, softWrap = false, fontSize = 12.sp)
                    }
                }

                if (event.style == InvoicePdfStyle.THERMAL_COUPON) {
                    ThermalReceiptPreviewCard(
                        business = biz,
                        invoiceWithItems = event.invoiceWithItems
                    )
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "${biz.name} • #${inv.invoiceNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            PerformedByPill(name = inv.performedByName)
                            Text(
                                text = buildString {
                                    append("Customer: ${inv.customerName}\n")
                                    append("Items: ${event.invoiceWithItems.items.sumOf { it.quantity }} units • ${inv.paymentMethod}\n")
                                    append("Total: ${Formatters.formatPkr(inv.grandTotal, currency)}")
                                    if (inv.balanceDue > 0.01 && !inv.isReturned) {
                                        append("\nPaid: ${Formatters.formatPkr(inv.amountPaid, currency)} | Due: ${Formatters.formatPkr(inv.balanceDue, currency)}")
                                    } else {
                                        append(" (${InvoicePaymentStatus.fromName(inv.paymentStatus).label})")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sleek compact A4 PDF with clean hairline dividers.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Button(
                    onClick = { ShareHelper.sharePdfOnWhatsApp(context, event.pdfFile, caption) },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_whatsapp_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share_whatsapp), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send via WhatsApp", maxLines = 1, softWrap = false)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ShareHelper.sharePdfViaEmail(
                                context = context,
                                pdfFile = event.pdfFile,
                                recipientEmail = inv.customerEmail,
                                subject = "Invoice #${inv.invoiceNumber} — ${biz.name}",
                                body = "Dear ${inv.customerName},\n\nPlease find attached your receipt #${inv.invoiceNumber} totaling ${Formatters.formatPkr(inv.grandTotal, currency)}.\n\nRegards,\n${biz.name}"
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_email_button")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = stringResource(R.string.cd_share_email), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Email", maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = {
                            ShareHelper.shareFileGeneric(
                                context = context,
                                file = event.pdfFile,
                                mimeType = "application/pdf",
                                chooserTitle = "Share / Print Receipt PDF",
                                extraText = caption
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("share_pdf_generic_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.cd_share_pdf), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Print / Share", maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ThermalReceiptPreviewCard(
    business: BusinessEntity,
    invoiceWithItems: InvoiceWithItems
) {
    val inv = invoiceWithItems.invoice
    val currency = business.resolvedCurrency()
    val paperBg = Color(0xFFFFFDF8)
    val inkColor = Color(0xFF1A1C1E)
    val mutedInk = Color(0xFF55595E)
    val borderColor = Color(0xFFD5D7DB)

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(paperBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = business.name.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = inkColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (business.phone.isNotBlank()) {
                Text(
                    text = "Tel: ${business.phone}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = mutedInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = "- - - - - - - - - - - - - - - - - -",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = mutedInk,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            ThermalPreviewLine("BILL #", inv.invoiceNumber, inkColor, bold = true)
            ThermalPreviewLine("DATE", Formatters.formatDateTime(inv.createdAt), mutedInk)
            ThermalPreviewLine("CUST", inv.customerName, inkColor)
            ThermalPreviewLine("BY", inv.performedByName.ifBlank { "Admin" }, mutedInk)
            Text(
                text = "- - - - - - - - - - - - - - - - - -",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = mutedInk,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            invoiceWithItems.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.productName}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = inkColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Formatters.formatDecimal(item.lineTotal),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = inkColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Text(
                text = "= = = = = = = = = = = = = = = = = =",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = inkColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            ThermalPreviewLine("TOTAL", Formatters.formatPkr(inv.grandTotal, currency), inkColor, bold = true)
            ThermalPreviewLine("PAID (${inv.paymentMethod})", Formatters.formatPkr(inv.amountPaid, currency), mutedInk)
            if (!inv.isReturned && inv.balanceDue > 0.01) {
                ThermalPreviewLine("CREDIT DUE", Formatters.formatPkr(inv.balanceDue, currency), ExpiredCrimson, bold = true)
            }
            Text(
                text = business.invoiceFooter.ifBlank { "Thank you for your business!" },
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                color = mutedInk,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
        // Serrated / Zig-Zag Coupon Torn Bottom Edge
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            val toothW = 14f
            val toothH = size.height
            val fillPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                var x = size.width
                while (x > 0f) {
                    val mid = (x - toothW / 2f).coerceAtLeast(0f)
                    val next = (x - toothW).coerceAtLeast(0f)
                    lineTo(mid, toothH)
                    lineTo(next, 0f)
                    x = next
                }
                close()
            }
            drawPath(path = fillPath, color = paperBg)
            val edgePath = Path().apply {
                moveTo(0f, 0f)
                var x = 0f
                while (x < size.width) {
                    val mid = (x + toothW / 2f).coerceAtMost(size.width)
                    val next = (x + toothW).coerceAtMost(size.width)
                    lineTo(mid, toothH)
                    lineTo(next, 0f)
                    x = next
                }
            }
            drawPath(path = edgePath, color = borderColor, style = Stroke(width = 1.5f))
        }
    }
}

@Composable
private fun ThermalPreviewLine(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp,
            color = color,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
