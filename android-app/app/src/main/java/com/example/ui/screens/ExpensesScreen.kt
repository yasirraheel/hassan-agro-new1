package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.AppConfirmDeleteDialog
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FormScreenDialog
import com.example.ui.components.PerformedByPill
import com.example.ui.components.ProofImagePickerCard
import com.example.ui.components.ProofImageViewerDialog
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoFigureLarge
import com.example.ui.theme.MonoFigureMedium
import com.example.util.Formatters
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@Composable
fun ExpensesScreen(
    activeBusiness: BusinessEntity,
    expenses: List<ExpenseEntity>,
    autoOpenAdd: Boolean,
    onConsumeAutoOpenAdd: () -> Unit,
    onSaveProofImage: suspend (Uri) -> String?,
    onSaveExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = activeBusiness.resolvedCurrency()
    val semantic = LocalSemanticColors.current
    val customCategories = remember(activeBusiness.expenseCategoriesJson, activeBusiness.businessType) {
        activeBusiness.expenseCategories()
    }

    val initCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH)) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var deleteTargetExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var previewProofPair by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(autoOpenAdd) {
        if (autoOpenAdd) {
            showAddSheet = true
            onConsumeAutoOpenAdd()
        }
    }

    val (startMs, endMs) = remember(selectedYear, selectedMonth) {
        Formatters.monthBoundsMillis(selectedYear, selectedMonth)
    }

    val monthlyExpenses = remember(expenses, startMs, endMs) {
        expenses.filter { it.expenseDateMillis in startMs until endMs }
    }
    val filteredExpenses = remember(monthlyExpenses, selectedCategory) {
        if (selectedCategory == null) monthlyExpenses
        else monthlyExpenses.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }
    val monthlyTotal = remember(monthlyExpenses) { monthlyExpenses.sumOf { it.amount } }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonth == 0) {
                                    selectedMonth = 11
                                    selectedYear -= 1
                                } else {
                                    selectedMonth -= 1
                                }
                            },
                            modifier = Modifier.testTag("expenses_prev_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_previous_month))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Formatters.formatMonthYear(selectedYear, selectedMonth),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Text(
                                text = "${monthlyExpenses.size} entries",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedMonth == 11) {
                                    selectedMonth = 0
                                    selectedYear += 1
                                } else {
                                    selectedMonth += 1
                                }
                            },
                            modifier = Modifier.testTag("expenses_next_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next_month))
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Monthly Spend",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = Formatters.formatCompactPkr(monthlyTotal, currency),
                            style = MonoFigureMedium,
                            fontWeight = FontWeight.Bold,
                            color = semantic.negativeFg,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Dynamic Per-Business Expense Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgriFilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = "All"
                )
                customCategories.forEach { cat ->
                    AgriFilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = cat
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Default.Payments,
                        title = "No expenses in ${Formatters.formatMonthYear(selectedYear, selectedMonth)}",
                        subtitle = "Log rent, stock purchases, utilities, transport, or staff salaries for ${activeBusiness.name} to track real monthly profit.",
                        actionLabel = "Add Expense",
                        onAction = { showAddSheet = true },
                        actionTestTag = "expenses_empty_add_btn"
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        val hasProof = !expense.proofImagePath.isNullOrBlank() && File(expense.proofImagePath).exists()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingExpense = expense },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = expense.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = expense.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${expense.paymentMethod} • ${Formatters.formatDate(expense.expenseDateMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (expense.payee.isNotBlank() || expense.notes.isNotBlank()) {
                                        Text(
                                            text = listOf(expense.payee, expense.notes).filter { it.isNotBlank() }.joinToString(" - "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PerformedByPill(name = expense.performedByName)
                                        if (hasProof) {
                                            Surface(
                                                modifier = Modifier
                                                    .clickable {
                                                        previewProofPair = expense.title to expense.proofImagePath!!
                                                    }
                                                    .testTag("expense_view_proof_${expense.id}"),
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Image,
                                                        contentDescription = "View receipt proof",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "Proof",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Formatters.formatPkr(expense.amount, currency),
                                        style = MonoFigureMedium,
                                        color = semantic.negativeFg,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    IconButton(
                                        onClick = { deleteTargetExpense = expense },
                                        modifier = Modifier.size(34.dp)
                                    ) {
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
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("expenses_add_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_item))
        }
    }

    if (showAddSheet || editingExpense != null) {
        ExpenseEditorDialog(
            business = activeBusiness,
            existing = editingExpense,
            onSaveProofImage = onSaveProofImage,
            onSave = {
                onSaveExpense(it)
                showAddSheet = false
                editingExpense = null
            },
            onDismiss = {
                showAddSheet = false
                editingExpense = null
            }
        )
    }

    deleteTargetExpense?.let { target ->
        AppConfirmDeleteDialog(
            title = "Delete Expense?",
            itemLabel = "${target.title} (${Formatters.formatPkr(target.amount, currency)})",
            message = "This expense entry will be removed from ${activeBusiness.name}'s monthly ledger and profit/loss totals.",
            confirmText = "Delete Expense",
            onConfirm = { onDeleteExpense(target.id) },
            onDismiss = { deleteTargetExpense = null }
        )
    }

    previewProofPair?.let { (title, path) ->
        ProofImageViewerDialog(
            title = "Expense Proof • $title",
            imagePath = path,
            onDismiss = { previewProofPair = null }
        )
    }
}

@Composable
private fun ExpenseEditorDialog(
    business: BusinessEntity,
    existing: ExpenseEntity?,
    onSaveProofImage: suspend (Uri) -> String?,
    onSave: (ExpenseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val currency = business.resolvedCurrency()
    val scope = rememberCoroutineScope()
    val categories = remember(business.expenseCategoriesJson, business.businessType) { business.expenseCategories() }
    val paymentMethods = remember(business.paymentMethodsJson, business.businessType) { business.paymentMethods() }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: categories.firstOrNull() ?: "Other") }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: paymentMethods.firstOrNull() ?: "Cash") }
    var payee by remember { mutableStateOf(existing?.payee ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var proofImagePath by remember { mutableStateOf(existing?.proofImagePath) }
    var dateMillis by remember { mutableLongStateOf(existing?.expenseDateMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    FormScreenDialog(
        title = if (existing == null) "Record Business Expense" else "Edit Expense",
        subtitle = business.name,
        onDismiss = onDismiss,
        bottomBar = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (title.isBlank() || amt == null || amt <= 0.0) {
                        errorMsg = "Enter a valid title and amount greater than 0"
                        return@Button
                    }
                    val entity = (existing ?: ExpenseEntity(
                        businessId = business.id,
                        title = title.trim(),
                        amount = amt
                    )).copy(
                        businessId = business.id,
                        title = title.trim(),
                        category = category,
                        amount = amt,
                        paymentMethod = paymentMethod,
                        payee = payee.trim(),
                        notes = notes.trim(),
                        proofImagePath = proofImagePath,
                        expenseDateMillis = dateMillis
                    )
                    onSave(entity)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_expense_button")
            ) {
                Text(if (existing == null) "Save Expense" else "Update Expense", maxLines = 1, softWrap = false)
            }
        }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; errorMsg = null },
            label = { Text("Expense Title *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("e.g. Monthly Rent or Stock Freight", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expense_title_input")
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it; errorMsg = null },
            label = { Text("Amount ($currency) *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expense_amount_input")
        )

        Text("Category", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                AgriFilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = cat
                )
            }
        }

        Text("Payment Method", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            paymentMethods.forEach { method ->
                AgriFilterChip(
                    selected = paymentMethod == method,
                    onClick = { paymentMethod = method },
                    label = method
                )
            }
        }

        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Date: ${Formatters.formatDate(dateMillis)}", maxLines = 1, softWrap = false)
        }

        OutlinedTextField(
            value = payee,
            onValueChange = { payee = it },
            label = { Text("Paid To / Vendor (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.fillMaxWidth()
        )

        // Proof of Payment / Receipt Reference Image
        ProofImagePickerCard(
            label = "Receipt / Proof of Payment (optional)",
            imagePath = proofImagePath,
            onPickImageUri = { uri ->
                scope.launch {
                    val saved = onSaveProofImage(uri)
                    if (saved != null) proofImagePath = saved
                }
            },
            onRemoveImage = { proofImagePath = null },
            testTagPrefix = "expense_proof"
        )

        if (errorMsg != null) {
            Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialMillis = dateMillis,
            onDateSelected = { dateMillis = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
