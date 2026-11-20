package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpiryStatus
import com.example.data.local.InvoicePaymentStatus
import com.example.data.local.InvoiceWithItems
import com.example.data.local.PricingTier
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.FormScreenDialog
import com.example.ui.components.PerformedByPill
import com.example.ui.theme.ExpiredCrimson
import com.example.ui.theme.ExpiringSoonAmber
import com.example.ui.theme.ExpiringSoonBg
import com.example.ui.theme.MonoFigureLarge
import com.example.ui.theme.MonoFigureMedium
import com.example.ui.theme.SafeStockBg
import com.example.ui.theme.SafeStockGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.Formatters
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
                    Text("Complete Sale & Generate Invoice", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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
                            val rate = if (enabled && (draft.taxRateText.toDoubleOrNull() ?: 0.0) == 0.0) "18" else draft.taxRateText
                            onUpdateTax(enabled, rate)
                        },
                        modifier = Modifier.testTag("billing_tax_switch")
                    )
                }
                AnimatedVisibility(visible = draft.taxEnabled) {
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

@Composable
fun InvoiceShareDialog(
    event: GeneratedInvoiceEvent,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val inv = event.invoiceWithItems.invoice
    val biz = event.business
    val currency = biz.resolvedCurrency()
    val caption = "Invoice #${inv.invoiceNumber} from ${biz.name} — Total: ${Formatters.formatPkr(inv.grandTotal, currency)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = { Text("Sale Completed & Invoice Ready") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${biz.name} • #${inv.invoiceNumber}",
                    style = MaterialTheme.typography.titleMedium
                )
                PerformedByPill(name = inv.performedByName)
                Text(
                    text = buildString {
                        append("Customer: ${inv.customerName}\n")
                        append("Total: ${Formatters.formatPkr(inv.grandTotal, currency)} • ${inv.paymentMethod}\n")
                        if (inv.balanceDue > 0.01 && !inv.isReturned) {
                            append("Paid: ${Formatters.formatPkr(inv.amountPaid, currency)} | Credit Due: ${Formatters.formatPkr(inv.balanceDue, currency)}")
                        } else {
                            append("Status: ${InvoicePaymentStatus.fromName(inv.paymentStatus).label}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
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
                OutlinedButton(
                    onClick = {
                        ShareHelper.sharePdfViaEmail(
                            context = context,
                            pdfFile = event.pdfFile,
                            recipientEmail = inv.customerEmail,
                            subject = "Invoice #${inv.invoiceNumber} — ${biz.name}",
                            body = "Dear ${inv.customerName},\n\nPlease find attached your sales invoice #${inv.invoiceNumber} totaling ${Formatters.formatPkr(inv.grandTotal, currency)}.\n\nRegards,\n${biz.name}"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_email_button")
                ) {
                    Icon(Icons.Default.Email, contentDescription = stringResource(R.string.cd_share_email), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send via Email", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = {
                        ShareHelper.shareFileGeneric(
                            context = context,
                            file = event.pdfFile,
                            mimeType = "application/pdf",
                            chooserTitle = "Share Invoice PDF",
                            extraText = caption
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_pdf_generic_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.cd_share_pdf), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share / Print PDF", maxLines = 1, softWrap = false)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
