package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppPermission
import com.example.data.local.AuthSession
import com.example.data.local.BusinessEntity
import com.example.data.local.BusinessTypePreset
import com.example.data.local.CustomizationJson
import com.example.data.local.PricingTier
import com.example.data.local.ServerSyncConfig
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.BusinessLogoAvatar
import com.example.ui.components.EditableTagListEditor
import com.example.ui.components.FormScreenDialog
import com.example.ui.components.ThemeColorSwatchPicker
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.MonoCodeSmall
import com.example.util.Formatters
import com.example.viewmodel.SyncUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    authSession: AuthSession,
    businesses: List<BusinessEntity>,
    activeBusiness: BusinessEntity,
    serverConfig: ServerSyncConfig,
    syncState: SyncUiState,
    pendingSyncCount: Int,
    canManageBusinesses: Boolean,
    canManageSettings: Boolean,
    onSwitchBusiness: (String) -> Unit,
    onSaveBusiness: (BusinessEntity, Boolean) -> Unit,
    onDeleteBusiness: (String) -> Unit,
    onUpdateBusinessLogo: (String, Uri) -> Unit,
    onSaveServerConfig: (String, String, String, Boolean) -> Unit,
    onTestServerConnection: (String, String) -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingBusiness by remember { mutableStateOf<BusinessEntity?>(null) }
    var showAddBusinessSheet by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateBusinessLogo(activeBusiness.id, uri)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    text = "Settings & Customization",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("settings_tab_businesses")
                ) {
                    Text(
                        "Businesses (${businesses.size})",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("settings_tab_server")
                ) {
                    Text(
                        "Server Sync",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Signed-In Admin Account & RBAC Card
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = authSession.userName.ifBlank { "Signed-In Admin" },
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = authSession.userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = authSession.roleName.ifBlank { "Admin" },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                val grantedLabels = remember(authSession.permissions) {
                                    AppPermission.entries
                                        .filter { authSession.permissions.contains(it.key) }
                                        .map { it.label }
                                }
                                if (grantedLabels.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        grantedLabels.forEach { permLabel ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = permLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = onSignOut,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("sign_out_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sign Out & Switch Admin Account", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // Active Business Profile & Customization Card
                    item {
                        val activeTheme = AppThemeOption.fromKey(activeBusiness.themeKey)
                        val activePreset = BusinessTypePreset.fromKey(activeBusiness.businessType)
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BusinessLogoAvatar(business = activeBusiness, size = 56.dp)
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ACTIVE BUSINESS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = activeBusiness.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${activePreset.label} • ${activeTheme.label} • ${activeBusiness.resolvedCurrency()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (canManageBusinesses) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                logoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("upload_brand_logo_button")
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Logo", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                        }
                                        Button(
                                            onClick = { editingBusiness = activeBusiness },
                                            modifier = Modifier
                                                .weight(1.4f)
                                                .height(48.dp)
                                                .testTag("edit_active_business_button")
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Customize Business", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Accessible Businesses", style = MaterialTheme.typography.titleMedium)
                            if (canManageBusinesses) {
                                Button(
                                    onClick = { showAddBusinessSheet = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("add_new_business_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Business", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    items(businesses, key = { it.id }) { biz ->
                        val isSelected = biz.id == activeBusiness.id
                        val bizTheme = AppThemeOption.fromKey(biz.themeKey)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitchBusiness(biz.id) }
                                .testTag("settings_business_card_${biz.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) bizTheme.primaryContainerColor.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) bizTheme.primaryColor else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BusinessLogoAvatar(business = biz, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        biz.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${bizTheme.label} • ${biz.invoicePrefix} • ${biz.resolvedCurrency()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = bizTheme.primaryColor
                                    )
                                }
                                if (canManageBusinesses) {
                                    IconButton(onClick = { editingBusiness = biz }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_item))
                                    }
                                    if (businesses.size > 1) {
                                        IconButton(onClick = { onDeleteBusiness(biz.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.cd_delete_item),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                ServerSyncConfigTab(
                    serverConfig = serverConfig,
                    syncState = syncState,
                    pendingSyncCount = pendingSyncCount,
                    canEditServer = canManageSettings,
                    onSaveServerConfig = onSaveServerConfig,
                    onTestConnection = onTestServerConnection,
                    onSyncNow = onSyncNow
                )
            }
        }
    }

    if (showAddBusinessSheet || editingBusiness != null) {
        BusinessProfileEditorSheet(
            existing = editingBusiness,
            onSave = { biz, switchToIt ->
                onSaveBusiness(biz, switchToIt)
                showAddBusinessSheet = false
                editingBusiness = null
            },
            onDismiss = {
                showAddBusinessSheet = false
                editingBusiness = null
            }
        )
    }
}

@Composable
private fun ServerSyncConfigTab(
    serverConfig: ServerSyncConfig,
    syncState: SyncUiState,
    pendingSyncCount: Int,
    canEditServer: Boolean,
    onSaveServerConfig: (String, String, String, Boolean) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onSyncNow: () -> Unit
) {
    var baseUrl by remember(serverConfig.serverBaseUrl) { mutableStateOf(serverConfig.serverBaseUrl) }
    var apiToken by remember(serverConfig.apiToken) { mutableStateOf(serverConfig.apiToken) }
    var wsUrl by remember(serverConfig.wsUrl) { mutableStateOf(serverConfig.wsUrl) }
    var autoSync by remember(serverConfig.autoSyncEnabled) { mutableStateOf(serverConfig.autoSyncEnabled) }
    var showContract by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Offline & Multi-Device Sync Status", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Device ID: ${serverConfig.deviceId.ifBlank { "Initializing..." }}",
                    style = MonoCodeSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pending local changes: $pendingSyncCount • WebSocket: ${if (syncState.wsConnected) "Connected" else "Idle"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (serverConfig.lastSyncTimestamp > 0L)
                        "Last synced: ${Formatters.formatDateTime(serverConfig.lastSyncTimestamp)}"
                    else
                        "Your signed-in session authenticates sync automatically via Bearer token.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (syncState.lastError != null) {
                    Text(
                        text = syncState.lastError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Backend Server Connection", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    enabled = canEditServer,
                    label = { Text("Server Base URL", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("https://ledger.hassanagro.com/", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_base_url_input")
                )
                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    enabled = canEditServer,
                    label = { Text("Fallback API Token (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("Signed-in session token is used automatically", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_token_input")
                )
                OutlinedTextField(
                    value = wsUrl,
                    onValueChange = { wsUrl = it },
                    enabled = canEditServer,
                    label = { Text("WebSocket URL (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("wss://ledger.hassanagro.com/ws/sync", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Sync Changes", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Automatically push/pull when saving stock, bills, expenses, or settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoSync, enabled = canEditServer, onCheckedChange = { autoSync = it })
                }

                if (canEditServer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveServerConfig(baseUrl, apiToken, wsUrl, autoSync) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_server_config_button")
                        ) {
                            Text("Save", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = { onTestConnection(baseUrl, apiToken) },
                            enabled = !syncState.isTestingConnection,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("test_server_button")
                        ) {
                            if (syncState.isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Test", maxLines = 1, softWrap = false)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (canEditServer) {
                            onSaveServerConfig(baseUrl, apiToken, wsUrl, autoSync)
                        }
                        onSyncNow()
                    },
                    enabled = !syncState.isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_now_settings_button")
                ) {
                    if (syncState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (syncState.isSyncing) "Syncing..." else "Sync All Businesses Now",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showContract = !showContract },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Backend API Contract",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (showContract) "Hide" else "View",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = showContract) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "All authenticated requests send Authorization: Bearer <userToken> and X-Device-Id. Manage roles, admins, and permissions in the Laravel Web Admin Panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = """
                                    1) POST /api/v1/auth/login
                                       POST /api/v1/auth/register
                                       GET  /api/v1/auth/me
                                       POST /api/v1/auth/logout

                                    2) GET /api/v1/health
                                       -> { "status": "ok", "serverTime": ... }

                                    3) POST /api/v1/sync/push
                                       Body: { deviceId, pushedAt, businesses[], products[], invoices[], expenses[] }

                                    4) GET /api/v1/sync/pull?since=<epochMillis>
                                       -> { serverTime, user, businesses[], products[], invoices[], expenses[] }
                                """.trimIndent(),
                                style = MonoCodeSmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Full-screen, non-dismissible Business Customization Studio:
 * - Industry presets (pre-filled, 100% editable)
 * - 6 Material 3 Color Schemes (live theme per business)
 * - Inventory Tracking toggles (Expiry & Batch/Serial + custom field labels)
 * - Fully editable Product Categories, Expense Categories, Payment Methods & 4-Tier Price Labels
 */
@Composable
fun BusinessProfileEditorSheet(
    existing: BusinessEntity?,
    onSave: (BusinessEntity, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val initialPreset = remember(existing) {
        BusinessTypePreset.fromKey(existing?.businessType)
    }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var businessType by remember { mutableStateOf(existing?.businessType ?: BusinessTypePreset.GENERAL.key) }
    var themeKey by remember { mutableStateOf(existing?.themeKey ?: initialPreset.defaultThemeKey) }
    var tagline by remember { mutableStateOf(existing?.tagline ?: initialPreset.tagline) }
    var currencySymbol by remember { mutableStateOf(existing?.currencySymbol ?: "Rs.") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var taxId by remember { mutableStateOf(existing?.taxId ?: "") }
    var prefix by remember { mutableStateOf(existing?.invoicePrefix ?: "BM") }
    var defaultTax by remember { mutableStateOf(existing?.defaultTaxRate?.toString() ?: "0") }
    var footer by remember { mutableStateOf(existing?.invoiceFooter ?: "Thank you for your business!") }

    var trackExpiry by remember { mutableStateOf(existing?.trackExpiry ?: initialPreset.trackExpiry) }
    var trackBatch by remember { mutableStateOf(existing?.trackBatch ?: initialPreset.trackBatch) }
    var batchFieldLabel by remember { mutableStateOf(existing?.batchFieldLabel ?: initialPreset.batchFieldLabel) }
    var packSizeLabel by remember { mutableStateOf(existing?.packSizeLabel ?: initialPreset.packSizeLabel) }

    var productCategories by remember {
        mutableStateOf(existing?.productCategories() ?: initialPreset.productCategories)
    }
    var expenseCategories by remember {
        mutableStateOf(existing?.expenseCategories() ?: initialPreset.expenseCategories)
    }
    var paymentMethods by remember {
        mutableStateOf(existing?.paymentMethods() ?: initialPreset.paymentMethods)
    }

    var mrpLabel by remember {
        mutableStateOf(existing?.tierLabel(PricingTier.MRP) ?: (initialPreset.pricingTierLabels["MRP"] ?: "MRP"))
    }
    var distLabel by remember {
        mutableStateOf(existing?.tierLabel(PricingTier.DISTRIBUTOR) ?: (initialPreset.pricingTierLabels["DISTRIBUTOR"] ?: "Distributor"))
    }
    var wholesaleLabel by remember {
        mutableStateOf(existing?.tierLabel(PricingTier.WHOLESALER) ?: (initialPreset.pricingTierLabels["WHOLESALER"] ?: "Wholesaler"))
    }
    var retailLabel by remember {
        mutableStateOf(existing?.tierLabel(PricingTier.RETAIL) ?: (initialPreset.pricingTierLabels["RETAIL"] ?: "Retail"))
    }

    var pendingPresetApply by remember { mutableStateOf<BusinessTypePreset?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun applyPresetDefaults(preset: BusinessTypePreset) {
        businessType = preset.key
        themeKey = preset.defaultThemeKey
        tagline = preset.tagline
        trackExpiry = preset.trackExpiry
        trackBatch = preset.trackBatch
        batchFieldLabel = preset.batchFieldLabel
        packSizeLabel = preset.packSizeLabel
        productCategories = preset.productCategories
        expenseCategories = preset.expenseCategories
        paymentMethods = preset.paymentMethods
        mrpLabel = preset.pricingTierLabels["MRP"] ?: "MRP"
        distLabel = preset.pricingTierLabels["DISTRIBUTOR"] ?: "Distributor"
        wholesaleLabel = preset.pricingTierLabels["WHOLESALER"] ?: "Wholesaler"
        retailLabel = preset.pricingTierLabels["RETAIL"] ?: "Retail"
        pendingPresetApply = null
    }

    FormScreenDialog(
        title = if (existing == null) "Create & Customize Business" else "Customize Business",
        subtitle = "Theme, categories, filters, pricing tiers & branding",
        onDismiss = onDismiss,
        bottomBar = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Business name is required"
                        return@Button
                    }
                    val derivedPrefix = prefix.trim().ifBlank {
                        name.trim().split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            .take(3)
                            .ifBlank { "BM" }
                    }
                    val tierMap = mapOf(
                        "MRP" to mrpLabel.trim().ifBlank { "MRP" },
                        "DISTRIBUTOR" to distLabel.trim().ifBlank { "Distributor" },
                        "WHOLESALER" to wholesaleLabel.trim().ifBlank { "Wholesaler" },
                        "RETAIL" to retailLabel.trim().ifBlank { "Retail" }
                    )
                    val entity = (existing ?: BusinessEntity(name = name.trim())).copy(
                        name = name.trim(),
                        tagline = tagline.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        taxId = taxId.trim(),
                        invoicePrefix = derivedPrefix,
                        themeKey = themeKey,
                        businessType = businessType,
                        currencySymbol = currencySymbol.trim().ifBlank { "Rs." },
                        trackExpiry = trackExpiry,
                        trackBatch = trackBatch,
                        batchFieldLabel = batchFieldLabel.trim().ifBlank { "Batch / Lot" },
                        packSizeLabel = packSizeLabel.trim().ifBlank { "Unit / Size" },
                        productCategoriesJson = CustomizationJson.encodeList(productCategories),
                        expenseCategoriesJson = CustomizationJson.encodeList(expenseCategories),
                        paymentMethodsJson = CustomizationJson.encodeList(paymentMethods),
                        pricingTierLabelsJson = CustomizationJson.encodeTierLabels(tierMap),
                        defaultTaxRate = defaultTax.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        invoiceFooter = footer.trim()
                    )
                    onSave(entity, existing == null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_business_button")
            ) {
                Text(
                    if (existing == null) "Create & Switch Business" else "Save Customizations",
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    ) {
        // Section 1: Business Type Preset
        Text(
            text = "Business Industry Preset",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Pre-fills categories, payment methods & tracking fields. Everything below stays 100% editable.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BusinessTypePreset.entries.forEach { preset ->
                AgriFilterChip(
                    selected = businessType.equals(preset.key, ignoreCase = true),
                    onClick = {
                        if (existing == null) {
                            applyPresetDefaults(preset)
                        } else {
                            businessType = preset.key
                            pendingPresetApply = preset
                        }
                    },
                    label = preset.label,
                    modifier = Modifier.testTag("preset_chip_${preset.key.lowercase()}")
                )
            }
        }

        if (pendingPresetApply != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Replace lists & toggles with ${pendingPresetApply!!.label} defaults?",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { applyPresetDefaults(pendingPresetApply!!) }) {
                        Text("Apply")
                    }
                }
            }
        }

        // Section 2: Per-Business App Color Theme
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "App Color Scheme for This Business",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        ThemeColorSwatchPicker(
            selectedThemeKey = themeKey,
            onSelect = { themeKey = it }
        )

        // Section 3: Identity & Branding
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "Business Profile & Invoice Branding",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMsg = null },
            label = { Text("Business Name *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("e.g. City Pharmacy, Tech Hub, Hassan Agro", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("business_name_input")
        )
        OutlinedTextField(
            value = tagline,
            onValueChange = { tagline = it },
            label = { Text("Tagline / Subtitle", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = currencySymbol,
                onValueChange = { currencySymbol = it.take(8) },
                label = { Text("Currency", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("Rs. / $ / AED") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("business_currency_input")
            )
            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it.take(6).uppercase() },
                label = { Text("Invoice Prefix", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = defaultTax,
                onValueChange = { defaultTax = it },
                label = { Text("Tax (%)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone / WhatsApp", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Shop / Warehouse Address", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = taxId,
            onValueChange = { taxId = it },
            label = { Text("Tax / Registration ID (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = footer,
            onValueChange = { footer = it },
            label = { Text("Invoice Footer Note", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.fillMaxWidth()
        )

        // Section 4: Inventory Fields & Filter Controls
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "Stock Fields & Filters",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Track Expiry Dates", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Shows expiry pickers, alerts & Expiring/Expired filters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = trackExpiry,
                        onCheckedChange = { trackExpiry = it },
                        modifier = Modifier.testTag("toggle_track_expiry")
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Track Batch / Serial Numbers", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Adds Batch, Lot, or IMEI/Serial tracking on stock & bills",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = trackBatch,
                        onCheckedChange = { trackBatch = it },
                        modifier = Modifier.testTag("toggle_track_batch")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = packSizeLabel,
                        onValueChange = { packSizeLabel = it },
                        label = { Text("Unit / Pack Label", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("e.g. Size, Weight, Model") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (trackBatch) {
                        OutlinedTextField(
                            value = batchFieldLabel,
                            onValueChange = { batchFieldLabel = it },
                            label = { Text("Batch Field Label", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("e.g. Serial / IMEI") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 5: Fully Editable Categories & Payment Methods
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "Custom Categories & Payment Methods",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        EditableTagListEditor(
            title = "Product Categories",
            placeholder = "Add product category...",
            items = productCategories,
            onItemsChange = { productCategories = it },
            testTagPrefix = "custom_product_categories"
        )
        EditableTagListEditor(
            title = "Expense Categories",
            placeholder = "Add expense category...",
            items = expenseCategories,
            onItemsChange = { expenseCategories = it },
            testTagPrefix = "custom_expense_categories"
        )
        EditableTagListEditor(
            title = "Payment Methods",
            placeholder = "Add payment method...",
            items = paymentMethods,
            onItemsChange = { paymentMethods = it },
            testTagPrefix = "custom_payment_methods"
        )

        // Section 6: Custom Pricing Tier Labels
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "4-Tier Pricing Names",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = mrpLabel,
                onValueChange = { mrpLabel = it },
                label = { Text("Tier 1 (MRP)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = distLabel,
                onValueChange = { distLabel = it },
                label = { Text("Tier 2 (Distributor)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = wholesaleLabel,
                onValueChange = { wholesaleLabel = it },
                label = { Text("Tier 3 (Wholesaler)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = retailLabel,
                onValueChange = { retailLabel = it },
                label = { Text("Tier 4 (Retail)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        if (errorMsg != null) {
            Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
    }
}
