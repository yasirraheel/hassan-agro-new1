package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.local.PricingTier
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FormScreenDialog
import com.example.ui.components.PerformedByPill
import com.example.ui.theme.ExpiredCrimson
import com.example.ui.theme.ExpiringSoonAmber
import com.example.ui.theme.ExpiringSoonBg
import com.example.ui.theme.MonoCodeSmall
import com.example.ui.theme.MonoFigureMedium
import com.example.ui.theme.SafeStockBg
import com.example.ui.theme.SafeStockGreen
import com.example.util.Formatters

private enum class CustomerFilter(val label: String) {
    ALL("All"),
    WITH_CREDIT("Credit Due")
}

internal fun computeCustomerStats(
    customer: CustomerEntity,
    invoices: List<InvoiceWithItems>
): Pair<Double, Double> {
    val matching = invoices.filter { entry ->
        val inv = entry.invoice
        !inv.isReturned && (
            (inv.customerId.isNotBlank() && inv.customerId == customer.id) ||
                (inv.customerId.isBlank() && inv.customerName.equals(customer.name, ignoreCase = true) &&
                    (customer.phone.isBlank() || inv.customerPhone == customer.phone))
            )
    }
    val totalBilled = matching.sumOf { it.invoice.grandTotal }
    val creditDue = customer.openingBalance + matching.sumOf { it.invoice.balanceDue }
    return totalBilled to creditDue
}

@Composable
fun CustomersTabContent(
    activeBusiness: BusinessEntity,
    customers: List<CustomerEntity>,
    invoices: List<InvoiceWithItems>,
    canCreateSale: Boolean,
    onBillCustomer: (CustomerEntity) -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = activeBusiness.resolvedCurrency()
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CustomerFilter.ALL) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showAddCustomer by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CustomerEntity?>(null) }

    val statsByCustomerId = remember(customers, invoices) {
        customers.associate { cust -> cust.id to computeCustomerStats(cust, invoices) }
    }

    val totalDirectoryCredit = remember(statsByCustomerId) {
        statsByCustomerId.values.sumOf { it.second.coerceAtLeast(0.0) }
    }

    val filteredCustomers = remember(customers, searchQuery, filter, statsByCustomerId) {
        customers.filter { cust ->
            val matchesQuery = searchQuery.isBlank() ||
                cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery, ignoreCase = true) ||
                cust.email.contains(searchQuery, ignoreCase = true) ||
                cust.address.contains(searchQuery, ignoreCase = true)
            val creditDue = statsByCustomerId[cust.id]?.second ?: cust.openingBalance
            val matchesFilter = when (filter) {
                CustomerFilter.ALL -> true
                CustomerFilter.WITH_CREDIT -> creditDue > 0.01
            }
            matchesQuery && matchesFilter
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Summary strip + Add Customer button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomerMetricBox(
                label = "Customers",
                value = customers.size.toString(),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            CustomerMetricBox(
                label = "Total Udhaar",
                value = Formatters.formatCompactPkr(totalDirectoryCredit, currency),
                accent = if (totalDirectoryCredit > 0.01) ExpiringSoonAmber else SafeStockGreen,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { showAddCustomer = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_customer_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Add", maxLines = 1, softWrap = false)
            }
        }

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
                placeholder = "Search customer name or phone...",
                testTagName = "customer_search_input",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CustomerFilter.entries.forEach { item ->
                AgriFilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = item.label
                )
            }
        }

        if (filteredCustomers.isEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.People,
                    title = if (customers.isEmpty()) "No saved customers yet" else "No matching customers",
                    subtitle = if (customers.isEmpty())
                        "Save dealers, shops, and regular buyers with their phone, preferred pricing tier, and Udhaar / credit ledger."
                    else
                        "Try searching with a different name, phone number, or filter.",
                    actionLabel = if (customers.isEmpty()) "Add First Customer" else null,
                    onAction = if (customers.isEmpty()) { { showAddCustomer = true } } else null,
                    actionTestTag = "empty_add_customer_btn"
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val (totalBilled, creditDue) = statsByCustomerId[customer.id] ?: (0.0 to customer.openingBalance)
                    CustomerDirectoryCard(
                        business = activeBusiness,
                        customer = customer,
                        totalBilled = totalBilled,
                        creditDue = creditDue,
                        canCreateSale = canCreateSale,
                        onBillNow = { onBillCustomer(customer) },
                        onEdit = { editingCustomer = customer },
                        onDelete = { deleteTarget = customer }
                    )
                }
            }
        }
    }

    if (showAddCustomer || editingCustomer != null) {
        CustomerEditorDialog(
            business = activeBusiness,
            existing = editingCustomer,
            onSave = { entity ->
                onSaveCustomer(entity)
                showAddCustomer = false
                editingCustomer = null
            },
            onDismiss = {
                showAddCustomer = false
                editingCustomer = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Customer?") },
            text = { Text("Remove ${target.name} from your customer directory? Existing invoices will remain intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(target.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredCrimson)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CustomerMetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                style = MonoCodeSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerDirectoryCard(
    business: BusinessEntity,
    customer: CustomerEntity,
    totalBilled: Double,
    creditDue: Double,
    canCreateSale: Boolean,
    onBillNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currency = business.resolvedCurrency()
    val tier = PricingTier.fromName(customer.pricingTier)
    val hasCredit = creditDue > 0.01

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = business.tierLabel(tier),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (customer.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (customer.address.isNotBlank()) {
                        Text(
                            text = customer.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (customer.performedByName.isNotBlank()) {
                        PerformedByPill(name = customer.performedByName)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasCredit) ExpiringSoonBg else SafeStockBg
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (hasCredit) "Udhaar Due" else "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasCredit) ExpiringSoonAmber else SafeStockGreen,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = Formatters.formatPkr(creditDue.coerceAtLeast(0.0), currency),
                                style = MonoCodeSmall,
                                color = if (hasCredit) ExpiringSoonAmber else SafeStockGreen,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (totalBilled > 0.01) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Billed: ${Formatters.formatCompactPkr(totalBilled, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canCreateSale) {
                    Button(
                        onClick = onBillNow,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("bill_customer_${customer.id}")
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("New Bill", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                }
                Spacer(Modifier.width(6.dp))
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
fun CustomerEditorDialog(
    business: BusinessEntity,
    existing: CustomerEntity?,
    onSave: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val currency = business.resolvedCurrency()
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var selectedTier by remember { mutableStateOf(PricingTier.fromName(existing?.pricingTier ?: PricingTier.RETAIL.name)) }
    var openingBalance by remember {
        mutableStateOf(existing?.openingBalance?.takeIf { it > 0.0 }?.toString() ?: "")
    }
    var creditLimit by remember {
        mutableStateOf(existing?.creditLimit?.takeIf { it > 0.0 }?.toString() ?: "")
    }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    FormScreenDialog(
        title = if (existing == null) "Add Customer" else "Edit Customer",
        subtitle = business.name,
        onDismiss = onDismiss,
        bottomBar = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Customer name is required"
                        return@Button
                    }
                    val entity = (existing ?: CustomerEntity(
                        businessId = business.id,
                        name = name.trim()
                    )).copy(
                        businessId = business.id,
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        pricingTier = selectedTier.name,
                        openingBalance = openingBalance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        creditLimit = creditLimit.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        notes = notes.trim()
                    )
                    onSave(entity)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_customer_button")
            ) {
                Text(
                    if (existing == null) "Save Customer" else "Update Customer",
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMsg = null },
            label = { Text("Customer / Shop / Dealer Name *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("e.g. Ali Traders, Bilal Store") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_name_input")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone / WhatsApp", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("customer_phone_input")
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address / Market / Route", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Preferred Pricing Tier",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PricingTier.entries.forEach { tier ->
                AgriFilterChip(
                    selected = selectedTier == tier,
                    onClick = { selectedTier = tier },
                    label = business.tierLabel(tier)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = openingBalance,
                onValueChange = { openingBalance = it },
                label = { Text("Opening Udhaar ($currency)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = creditLimit,
                onValueChange = { creditLimit = it },
                label = { Text("Credit Limit ($currency)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMsg != null) {
            Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
    }
}
