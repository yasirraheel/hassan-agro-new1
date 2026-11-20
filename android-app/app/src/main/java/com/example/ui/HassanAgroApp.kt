package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.ProductEntity
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.BusinessSwitcherPill
import com.example.ui.components.BusinessSwitcherSheet
import com.example.ui.components.SyncStatusBadge
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.BusinessProfileEditorSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.InventoryFilter
import kotlinx.coroutines.launch

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val INVENTORY = "inventory"
    const val BILLING = "billing"
    const val EXPENSES = "expenses"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(NavRoutes.DASHBOARD, R.string.nav_home, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    TopLevelDestination(NavRoutes.INVENTORY, R.string.nav_stock, Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    TopLevelDestination(NavRoutes.BILLING, R.string.nav_billing, Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    TopLevelDestination(NavRoutes.EXPENSES, R.string.nav_expenses, Icons.Filled.Payments, Icons.Outlined.Payments),
    TopLevelDestination(NavRoutes.REPORTS, R.string.nav_reports, Icons.Filled.BarChart, Icons.Outlined.BarChart)
)

private enum class ScanTarget {
    NONE,
    STOCK_IN,
    BILLING_CART
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HassanAgroApp(viewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NavRoutes.DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val businesses by viewModel.businesses.collectAsStateWithLifecycle()
    val activeBusiness by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
    val syncState by viewModel.syncUiState.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val inventoryFilter by viewModel.inventoryFilter.collectAsStateWithLifecycle()
    val billingDraft by viewModel.billingDraft.collectAsStateWithLifecycle()
    val heldCarts by viewModel.heldCarts.collectAsStateWithLifecycle()
    val generatedInvoice by viewModel.generatedInvoice.collectAsStateWithLifecycle()

    var showBusinessSwitcher by remember { mutableStateOf(false) }
    var showQuickAddBusiness by remember { mutableStateOf(false) }
    var activeScanTarget by remember { mutableStateOf(ScanTarget.NONE) }
    var pendingNewProductBarcode by remember { mutableStateOf<String?>(null) }
    var pendingRestockProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var autoOpenAddExpense by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (currentRoute != NavRoutes.SETTINGS) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            BusinessSwitcherPill(
                                activeBusiness = activeBusiness,
                                businessCount = businesses.size,
                                onClick = { showBusinessSwitcher = true }
                            )
                        },
                        actions = {
                            SyncStatusBadge(
                                syncState = syncState,
                                serverConfig = serverConfig,
                                pendingCount = pendingSyncCount,
                                onClick = {
                                    if (serverConfig.isConfigured) {
                                        viewModel.syncNow()
                                    } else {
                                        navController.navigate(NavRoutes.SETTINGS)
                                    }
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { navController.navigate(NavRoutes.SETTINGS) },
                                modifier = Modifier.testTag("top_bar_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.cd_open_settings),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail && currentRoute != NavRoutes.SETTINGS && !isKeyboardVisible) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && currentRoute != NavRoutes.SETTINGS) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.DASHBOARD
                    ) {
                        composable(NavRoutes.DASHBOARD) {
                            DashboardScreen(
                                activeBusiness = activeBusiness,
                                products = products,
                                invoices = invoices,
                                expenses = expenses,
                                onQuickNewBill = { navigateTopLevel(NavRoutes.BILLING) },
                                onQuickScanStock = { activeScanTarget = ScanTarget.STOCK_IN },
                                onQuickAddExpense = {
                                    autoOpenAddExpense = true
                                    navigateTopLevel(NavRoutes.EXPENSES)
                                },
                                onQuickSync = {
                                    if (serverConfig.isConfigured) {
                                        viewModel.syncNow()
                                    } else {
                                        viewModel.emitMessage("Connect your custom server in Settings to sync")
                                        navController.navigate(NavRoutes.SETTINGS)
                                    }
                                },
                                onOpenInventoryWithFilter = { filter ->
                                    viewModel.setInventoryFilter(filter)
                                    navigateTopLevel(NavRoutes.INVENTORY)
                                },
                                onOpenInvoice = { entry ->
                                    viewModel.openExistingInvoicePdf(entry)
                                    navigateTopLevel(NavRoutes.BILLING)
                                }
                            )
                        }

                        composable(NavRoutes.INVENTORY) {
                            InventoryScreen(
                                businessId = activeBusiness.id,
                                businessName = activeBusiness.name,
                                products = products,
                                activeFilter = inventoryFilter,
                                onFilterChange = viewModel::setInventoryFilter,
                                onRequestScan = { activeScanTarget = ScanTarget.STOCK_IN },
                                pendingNewProductBarcode = pendingNewProductBarcode,
                                pendingRestockProduct = pendingRestockProduct,
                                onClearScanPrompts = {
                                    pendingNewProductBarcode = null
                                    pendingRestockProduct = null
                                },
                                onSaveProduct = viewModel::saveProduct,
                                onRestockProduct = viewModel::restockProduct,
                                onDeleteProduct = viewModel::deleteProduct
                            )
                        }

                        composable(NavRoutes.BILLING) {
                            BillingScreen(
                                businessName = activeBusiness.name,
                                products = products,
                                invoices = invoices,
                                draft = billingDraft,
                                heldCarts = heldCarts,
                                generatedInvoice = generatedInvoice,
                                onUpdateCustomer = viewModel::updateBillingCustomer,
                                onSelectTier = viewModel::updateBillingTier,
                                onUpdateTax = viewModel::updateBillingTax,
                                onUpdateDiscount = viewModel::updateBillingDiscount,
                                onUpdateNotes = viewModel::updateBillingNotes,
                                onUpdatePayment = viewModel::updateBillingPayment,
                                onAddProductToCart = viewModel::addProductToCart,
                                onUpdateCartQty = viewModel::updateCartLineQuantity,
                                onUpdateCartPrice = viewModel::updateCartLineUnitPrice,
                                onClearCart = viewModel::clearBillingDraft,
                                onHoldCart = viewModel::holdCurrentCart,
                                onResumeHeldCart = viewModel::resumeHeldCart,
                                onDiscardHeldCart = viewModel::discardHeldCart,
                                onRequestScanToCart = { activeScanTarget = ScanTarget.BILLING_CART },
                                onFinalizeInvoice = viewModel::finalizeInvoiceAndGeneratePdf,
                                onOpenPastInvoice = viewModel::openExistingInvoicePdf,
                                onRecordPayment = viewModel::recordInvoicePayment,
                                onReturnInvoice = viewModel::returnInvoiceAndRestock,
                                onGoToInventory = { navigateTopLevel(NavRoutes.INVENTORY) },
                                onDismissGeneratedInvoice = viewModel::dismissGeneratedInvoice
                            )
                        }

                        composable(NavRoutes.EXPENSES) {
                            ExpensesScreen(
                                businessId = activeBusiness.id,
                                businessName = activeBusiness.name,
                                expenses = expenses,
                                autoOpenAdd = autoOpenAddExpense,
                                onConsumeAutoOpenAdd = { autoOpenAddExpense = false },
                                onSaveExpense = viewModel::saveExpense,
                                onDeleteExpense = viewModel::deleteExpense
                            )
                        }

                        composable(NavRoutes.REPORTS) {
                            ReportsScreen(
                                activeBusiness = activeBusiness,
                                businessCount = businesses.size,
                                products = products,
                                invoices = invoices,
                                expenses = expenses,
                                allProducts = allProducts,
                                allInvoices = allInvoices,
                                allExpenses = allExpenses,
                                onGenerateReportPdf = viewModel::generateMonthlyReportPdf,
                                onExportCsv = viewModel::exportCsvToCache,
                                onNotify = viewModel::emitMessage
                            )
                        }

                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                businesses = businesses,
                                activeBusiness = activeBusiness,
                                serverConfig = serverConfig,
                                syncState = syncState,
                                pendingSyncCount = pendingSyncCount,
                                onSwitchBusiness = viewModel::switchBusiness,
                                onSaveBusiness = viewModel::saveBusiness,
                                onDeleteBusiness = viewModel::deleteBusiness,
                                onUpdateBusinessLogo = viewModel::updateBusinessLogo,
                                onSaveServerConfig = viewModel::saveServerConfig,
                                onTestServerConnection = viewModel::testServerConnection,
                                onSaveAndSync = viewModel::saveAndSyncServerConfig,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBusinessSwitcher) {
        BusinessSwitcherSheet(
            businesses = businesses,
            activeBusiness = activeBusiness,
            onSelectBusiness = viewModel::switchBusiness,
            onAddNewBusiness = { showQuickAddBusiness = true },
            onManageBusinesses = { navController.navigate(NavRoutes.SETTINGS) },
            onDismiss = { showBusinessSwitcher = false }
        )
    }

    if (showQuickAddBusiness) {
        BusinessProfileEditorSheet(
            existing = null,
            onSave = { biz, switchToIt ->
                viewModel.saveBusiness(biz, switchToIt)
                showQuickAddBusiness = false
            },
            onDismiss = { showQuickAddBusiness = false }
        )
    }

    if (activeScanTarget != ScanTarget.NONE) {
        val target = activeScanTarget
        BarcodeScannerDialog(
            title = if (target == ScanTarget.STOCK_IN) "Scan Pesticide Stock In" else "Scan Product to Bill",
            onBarcodeScanned = { scannedCode ->
                activeScanTarget = ScanTarget.NONE
                scope.launch {
                    val found = viewModel.findProductByBarcode(scannedCode)
                    if (target == ScanTarget.STOCK_IN) {
                        if (found != null) {
                            pendingRestockProduct = found
                        } else {
                            pendingNewProductBarcode = scannedCode
                        }
                        viewModel.setInventoryFilter(InventoryFilter.ALL)
                        navigateTopLevel(NavRoutes.INVENTORY)
                    } else {
                        if (found != null) {
                            viewModel.addProductToCart(found)
                            viewModel.emitMessage("Added ${found.name} to bill")
                        } else {
                            viewModel.emitMessage("Barcode $scannedCode not found — add it in Stock first")
                        }
                    }
                }
            },
            onDismiss = { activeScanTarget = ScanTarget.NONE }
        )
    }
}
