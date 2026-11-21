package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.local.AppPermission
import com.example.data.local.ProductEntity
import com.example.ui.components.AppNavigationDrawerSheet
import com.example.ui.components.AppSplashScreen
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.BusinessSwitcherPill
import com.example.ui.components.BusinessSwitcherSheet
import com.example.ui.components.DrawerDestinationItem
import com.example.ui.components.SyncStatusBadge
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.BusinessProfileEditorSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsTabKey
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.HassanAgroTheme
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.InventoryFilter
import kotlinx.coroutines.delay
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

private val AllTopLevelDestinations = listOf(
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

private enum class AppRootStage {
    SPLASH,
    LOGIN,
    AUTHENTICATED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HassanAgroApp(viewModel: AppViewModel = viewModel()) {
    val authSessionLoaded by viewModel.authSessionLoaded.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
    val activeBusiness by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val appearanceMode by viewModel.appearanceMode.collectAsStateWithLifecycle()

    var splashMinDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(850)
        splashMinDone = true
    }

    var signupPreviewThemeKey by remember { mutableStateOf(AppThemeOption.OCEAN.key) }
    val appliedThemeKey = if (authSessionLoaded?.isLoggedIn == true) activeBusiness.themeKey else signupPreviewThemeKey

    val rootStage = when {
        authSessionLoaded == null || !splashMinDone -> AppRootStage.SPLASH
        authSessionLoaded?.isLoggedIn == true -> AppRootStage.AUTHENTICATED
        else -> AppRootStage.LOGIN
    }

    HassanAgroTheme(themeKey = appliedThemeKey, appearanceMode = appearanceMode) {
        AnimatedContent(
            targetState = rootStage,
            transitionSpec = {
                (fadeIn(animationSpec = tween(340, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(340, easing = FastOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)))
            },
            label = "root_stage_transition"
        ) { stage ->
            when (stage) {
                AppRootStage.SPLASH -> {
                    val statusText = when {
                        authSessionLoaded == null -> "Checking saved session..."
                        authSessionLoaded?.isLoggedIn == true -> "Opening ${activeBusiness.name}..."
                        else -> "Preparing workspace..."
                    }
                    AppSplashScreen(statusLabel = statusText)
                }
                AppRootStage.LOGIN -> {
                    LoginScreen(
                        initialServerUrl = serverConfig.serverBaseUrl,
                        authState = authUiState,
                        selectedPreviewThemeKey = signupPreviewThemeKey,
                        onPreviewThemeChange = { signupPreviewThemeKey = it },
                        onLogin = viewModel::login,
                        onRegister = viewModel::register,
                        onClearError = viewModel::clearAuthError
                    )
                }
                AppRootStage.AUTHENTICATED -> {
                    AuthenticatedBusinessApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedBusinessApp(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val authSession by viewModel.authSession.collectAsStateWithLifecycle()
    val businesses by viewModel.businesses.collectAsStateWithLifecycle()
    val activeBusiness by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val activeMonthlySales by viewModel.activeMonthlySalesTotals.collectAsStateWithLifecycle()
    val activeMonthlyExpenses by viewModel.activeMonthlyExpenseTotals.collectAsStateWithLifecycle()
    val allMonthlySales by viewModel.allMonthlySalesTotals.collectAsStateWithLifecycle()
    val allMonthlyExpenses by viewModel.allMonthlyExpenseTotals.collectAsStateWithLifecycle()
    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
    val syncState by viewModel.syncUiState.collectAsStateWithLifecycle()
    val teamState by viewModel.teamState.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val appearanceMode by viewModel.appearanceMode.collectAsStateWithLifecycle()
    val dashboardPeriod by viewModel.dashboardPeriod.collectAsStateWithLifecycle()
    val inventoryFilter by viewModel.inventoryFilter.collectAsStateWithLifecycle()
    val billingDraft by viewModel.billingDraft.collectAsStateWithLifecycle()
    val heldCarts by viewModel.heldCarts.collectAsStateWithLifecycle()
    val generatedInvoice by viewModel.generatedInvoice.collectAsStateWithLifecycle()

    val canDashboard = authSession.hasPermission(AppPermission.VIEW_DASHBOARD)
    val canInventory = authSession.hasPermission(AppPermission.MANAGE_INVENTORY)
    val canUsePos = authSession.hasPermission(AppPermission.USE_POS)
    val canCollectCredit = authSession.hasPermission(AppPermission.COLLECT_CREDIT)
    val canReturnSales = authSession.hasPermission(AppPermission.RETURN_SALES)
    val canBillingTab = canUsePos || canCollectCredit || canReturnSales
    val canExpenses = authSession.hasPermission(AppPermission.MANAGE_EXPENSES)
    val canReports = authSession.hasPermission(AppPermission.VIEW_REPORTS)
    val canExport = authSession.hasPermission(AppPermission.EXPORT_DATA)
    val canManageBusinesses = authSession.hasPermission(AppPermission.MANAGE_BUSINESSES)
    val canManageTeam = authSession.hasPermission(AppPermission.MANAGE_TEAM) || authSession.hasPermission(AppPermission.MANAGE_ROLES)
    val canManageSettings = authSession.hasPermission(AppPermission.MANAGE_SETTINGS)

    val allowedDestinations = remember(canDashboard, canInventory, canBillingTab, canExpenses, canReports) {
        val list = AllTopLevelDestinations.filter { dest ->
            when (dest.route) {
                NavRoutes.DASHBOARD -> canDashboard
                NavRoutes.INVENTORY -> canInventory
                NavRoutes.BILLING -> canBillingTab
                NavRoutes.EXPENSES -> canExpenses
                NavRoutes.REPORTS -> canReports
                else -> true
            }
        }
        list.ifEmpty { listOf(AllTopLevelDestinations.first()) }
    }

    val startRoute = remember(allowedDestinations) { allowedDestinations.first().route }
    val currentRoute = backStackEntry?.destination?.route ?: startRoute

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var settingsInitialTab by remember { mutableStateOf(SettingsTabKey.BUSINESSES) }
    var showBusinessSwitcher by remember { mutableStateOf(false) }
    var showQuickAddBusiness by remember { mutableStateOf(false) }
    var activeScanTarget by remember { mutableStateOf(ScanTarget.NONE) }
    var pendingNewProductBarcode by remember { mutableStateOf<String?>(null) }
    var pendingRestockProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var autoOpenAddExpense by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTopLevel(route: String) {
        if (allowedDestinations.none { it.route == route }) {
            viewModel.emitMessage("Your role (${authSession.roleName}) does not have access to this section")
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openSettings(tab: SettingsTabKey = SettingsTabKey.BUSINESSES) {
        settingsInitialTab = tab
        navController.navigate(NavRoutes.SETTINGS) {
            launchSingleTop = true
        }
    }

    val drawerDestinations = allowedDestinations.map { dest ->
        DrawerDestinationItem(
            route = dest.route,
            label = stringResource(dest.labelRes),
            selectedIcon = dest.selectedIcon,
            unselectedIcon = dest.unselectedIcon
        )
    }

    val isDrawerTargetOpen = drawerState.targetValue == DrawerValue.Open
    val contentScale by animateFloatAsState(
        targetValue = if (isDrawerTargetOpen) 0.965f else 1f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "drawer_content_scale"
    )
    val contentCornerRadius by animateDpAsState(
        targetValue = if (isDrawerTargetOpen) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "drawer_content_corner"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != NavRoutes.SETTINGS,
        drawerContent = {
            AppNavigationDrawerSheet(
                activeBusiness = activeBusiness,
                businessCount = businesses.size,
                authSession = authSession,
                appearanceMode = appearanceMode,
                serverConfig = serverConfig,
                syncState = syncState,
                pendingSyncCount = pendingSyncCount,
                destinations = drawerDestinations,
                currentRoute = currentRoute,
                canInventory = canInventory,
                canManageBusinesses = canManageBusinesses,
                canManageTeam = canManageTeam,
                canManageSettings = canManageSettings,
                onAppearanceModeChange = viewModel::setAppearanceMode,
                onNavigateRoute = { route ->
                    scope.launch { drawerState.close() }
                    navigateTopLevel(route)
                },
                onOpenSettings = { tab ->
                    scope.launch { drawerState.close() }
                    openSettings(tab)
                },
                onOpenBusinessSwitcher = {
                    scope.launch { drawerState.close() }
                    if (canManageBusinesses || businesses.size > 1) {
                        showBusinessSwitcher = true
                    }
                },
                onScanStockBarcode = {
                    scope.launch { drawerState.close() }
                    if (canInventory) {
                        activeScanTarget = ScanTarget.STOCK_IN
                    }
                },
                onSyncNow = {
                    scope.launch { drawerState.close() }
                    if (serverConfig.isConfigured) {
                        viewModel.syncNow(silent = false)
                    } else {
                        openSettings(SettingsTabKey.SYNC)
                    }
                },
                onSignOut = {
                    scope.launch { drawerState.close() }
                    viewModel.logout()
                }
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .clip(RoundedCornerShape(contentCornerRadius))
        ) {
            val useNavigationRail = maxWidth >= 600.dp

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
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("top_bar_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.cd_open_drawer),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            },
                            title = {
                                BusinessSwitcherPill(
                                    activeBusiness = activeBusiness,
                                    businessCount = businesses.size,
                                    onClick = {
                                        if (canManageBusinesses || businesses.size > 1) {
                                            showBusinessSwitcher = true
                                        }
                                    }
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
                                            openSettings(SettingsTabKey.SYNC)
                                        }
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { openSettings(SettingsTabKey.BUSINESSES) },
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
                if (!useNavigationRail && currentRoute != NavRoutes.SETTINGS) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        allowedDestinations.forEach { dest ->
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
                        allowedDestinations.forEach { dest ->
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
                        startDestination = startRoute,
                        enterTransition = {
                            fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> (fullWidth * 0.06f).toInt() },
                                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                                )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(210, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -(fullWidth * 0.04f).toInt() },
                                    animationSpec = tween(210, easing = FastOutSlowInEasing)
                                )
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -(fullWidth * 0.06f).toInt() },
                                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                                )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(210, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> (fullWidth * 0.06f).toInt() },
                                    animationSpec = tween(210, easing = FastOutSlowInEasing)
                                )
                        }
                    ) {
                        composable(NavRoutes.DASHBOARD) {
                            DashboardScreen(
                                activeBusiness = activeBusiness,
                                authSession = authSession,
                                products = products,
                                invoices = invoices,
                                expenses = expenses,
                                selectedPeriod = dashboardPeriod,
                                onPeriodChange = viewModel::setDashboardPeriod,
                                onQuickNewBill = { navigateTopLevel(NavRoutes.BILLING) },
                                onQuickScanStock = {
                                    if (canInventory) activeScanTarget = ScanTarget.STOCK_IN
                                    else viewModel.emitMessage("Your role cannot modify inventory")
                                },
                                onQuickAddExpense = {
                                    if (canExpenses) {
                                        autoOpenAddExpense = true
                                        navigateTopLevel(NavRoutes.EXPENSES)
                                    } else {
                                        viewModel.emitMessage("Your role cannot manage expenses")
                                    }
                                },
                                onQuickSync = { viewModel.syncNow() },
                                onOpenInventoryWithFilter = { filter ->
                                    if (canInventory) {
                                        viewModel.setInventoryFilter(filter)
                                        navigateTopLevel(NavRoutes.INVENTORY)
                                    }
                                },
                                onOpenInvoice = { entry ->
                                    viewModel.openExistingInvoicePdf(entry)
                                    if (canBillingTab) navigateTopLevel(NavRoutes.BILLING)
                                }
                            )
                        }

                        composable(NavRoutes.INVENTORY) {
                            InventoryScreen(
                                activeBusiness = activeBusiness,
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
                                activeBusiness = activeBusiness,
                                products = products,
                                invoices = invoices,
                                customers = customers,
                                draft = billingDraft,
                                heldCarts = heldCarts,
                                generatedInvoice = generatedInvoice,
                                canCreateSale = canUsePos,
                                canCollectCredit = canCollectCredit,
                                canReturnSale = canReturnSales,
                                onSelectSavedCustomer = viewModel::selectBillingCustomer,
                                onToggleSaveCustomer = viewModel::updateBillingSaveCustomer,
                                onSaveCustomer = viewModel::saveCustomer,
                                onDeleteCustomer = viewModel::deleteCustomer,
                                onUpdateCustomer = viewModel::updateBillingCustomer,
                                onSelectTier = viewModel::updateBillingTier,
                                onUpdateTax = viewModel::updateBillingTax,
                                onUpdateDiscount = viewModel::updateBillingDiscount,
                                onUpdateNotes = viewModel::updateBillingNotes,
                                onUpdatePayment = viewModel::updateBillingPayment,
                                onAddProductToCart = viewModel::addProductToCart,
                                onUpdateCartQty = viewModel::updateCartLineQuantity,
                                onUpdateCartPrice = viewModel::updateCartLineUnitPrice,
                                onAttachProofImage = viewModel::attachBillingProofImage,
                                onRemoveProofImage = viewModel::clearBillingProofImage,
                                onClearCart = viewModel::clearBillingDraft,
                                onHoldCart = viewModel::holdCurrentCart,
                                onResumeHeldCart = viewModel::resumeHeldCart,
                                onDiscardHeldCart = viewModel::discardHeldCart,
                                onRequestScanToCart = { activeScanTarget = ScanTarget.BILLING_CART },
                                onFinalizeInvoice = viewModel::finalizeInvoiceAndGeneratePdf,
                                onOpenPastInvoice = { entry -> viewModel.openExistingInvoicePdf(entry) },
                                onSwitchInvoiceStyle = viewModel::switchGeneratedInvoiceStyle,
                                onRecordPayment = viewModel::recordInvoicePayment,
                                onReturnInvoice = viewModel::returnInvoiceAndRestock,
                                onGoToInventory = { navigateTopLevel(NavRoutes.INVENTORY) },
                                onDismissGeneratedInvoice = viewModel::dismissGeneratedInvoice
                            )
                        }

                        composable(NavRoutes.EXPENSES) {
                            ExpensesScreen(
                                activeBusiness = activeBusiness,
                                expenses = expenses,
                                autoOpenAdd = autoOpenAddExpense,
                                onConsumeAutoOpenAdd = { autoOpenAddExpense = false },
                                onSaveProofImage = { uri -> viewModel.saveProofImage("expense_proof", uri) },
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
                                activeMonthlySalesRows = activeMonthlySales,
                                activeMonthlyExpenseRows = activeMonthlyExpenses,
                                allMonthlySalesRows = allMonthlySales,
                                allMonthlyExpenseRows = allMonthlyExpenses,
                                canExportData = canExport,
                                onGenerateReportPdf = viewModel::generateMonthlyReportPdf,
                                onExportCsv = viewModel::exportCsvToCache,
                                onNotify = viewModel::emitMessage
                            )
                        }

                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                authSession = authSession,
                                businesses = businesses,
                                activeBusiness = activeBusiness,
                                appearanceMode = appearanceMode,
                                serverConfig = serverConfig,
                                syncState = syncState,
                                teamState = teamState,
                                pendingSyncCount = pendingSyncCount,
                                canManageBusinesses = canManageBusinesses,
                                canManageTeam = canManageTeam,
                                canManageSettings = canManageSettings,
                                onAppearanceModeChange = viewModel::setAppearanceMode,
                                onSwitchBusiness = viewModel::switchBusiness,
                                onSaveBusiness = viewModel::saveBusiness,
                                onDeleteBusiness = viewModel::deleteBusiness,
                                onUpdateBusinessLogo = viewModel::updateBusinessLogo,
                                onSaveServerConfig = viewModel::saveServerConfig,
                                onTestServerConnection = viewModel::testServerConnection,
                                onSyncNow = { viewModel.syncNow(silent = false) },
                                onLoadTeam = viewModel::loadTeamOverview,
                                onSaveTeamRole = viewModel::saveTeamRole,
                                onDeleteTeamRole = viewModel::deleteTeamRole,
                                onSaveTeamUser = viewModel::saveTeamUser,
                                onToggleTeamUser = viewModel::toggleTeamUser,
                                onDeleteTeamUser = viewModel::deleteTeamUser,
                                onSignOut = viewModel::logout,
                                onBack = { navController.popBackStack() },
                                initialTab = settingsInitialTab
                            )
                        }
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
            canManageBusinesses = canManageBusinesses,
            onSelectBusiness = viewModel::switchBusiness,
            onAddNewBusiness = { showQuickAddBusiness = true },
            onManageBusinesses = { openSettings(SettingsTabKey.BUSINESSES) },
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
            title = if (target == ScanTarget.STOCK_IN) "Scan Product Stock In" else "Scan Product to Bill",
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
