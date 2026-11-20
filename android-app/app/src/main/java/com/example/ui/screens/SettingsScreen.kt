package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.ServerSyncConfig
import com.example.ui.components.BusinessLogoAvatar
import com.example.ui.components.FormScreenDialog
import com.example.ui.theme.MonoCodeSmall
import com.example.util.Formatters
import com.example.viewmodel.SyncUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    businesses: List<BusinessEntity>,
    activeBusiness: BusinessEntity,
    serverConfig: ServerSyncConfig,
    syncState: SyncUiState,
    pendingSyncCount: Int,
    onSwitchBusiness: (String) -> Unit,
    onSaveBusiness: (BusinessEntity, Boolean) -> Unit,
    onDeleteBusiness: (String) -> Unit,
    onUpdateBusinessLogo: (String, Uri) -> Unit,
    onSaveServerConfig: (String, String, String, Boolean) -> Unit,
    onTestServerConnection: (String, String) -> Unit,
    onSaveAndSync: (String, String, String, Boolean) -> Unit,
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
                    text = "Businesses & Server Sync",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                ) { Text("Businesses (${businesses.size})", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("settings_tab_server")
                ) { Text("Server Sync", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
            }

            if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
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
                                            text = activeBusiness.tagline.ifBlank { "Prefix: ${activeBusiness.invoicePrefix}" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
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
                                            .testTag("upload_brand_logo_button")
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Upload Logo", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    }
                                    Button(
                                        onClick = { editingBusiness = activeBusiness },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("edit_active_business_button")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit Profile", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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
                            Text("All Businesses", style = MaterialTheme.typography.titleMedium)
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

                    items(businesses, key = { it.id }) { biz ->
                        val isSelected = biz.id == activeBusiness.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitchBusiness(biz.id) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
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
                                BusinessLogoAvatar(business = biz, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(biz.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "Prefix: ${biz.invoicePrefix} • Tax: ${biz.defaultTaxRate}%",
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
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
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
            } else {
                ServerSyncConfigTab(
                    serverConfig = serverConfig,
                    syncState = syncState,
                    pendingSyncCount = pendingSyncCount,
                    onSaveServerConfig = onSaveServerConfig,
                    onTestConnection = onTestServerConnection,
                    onSaveAndSync = onSaveAndSync
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
    onSaveServerConfig: (String, String, String, Boolean) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onSaveAndSync: (String, String, String, Boolean) -> Unit
) {
    var baseUrl by remember(serverConfig.serverBaseUrl) { mutableStateOf(serverConfig.serverBaseUrl) }
    var apiToken by remember(serverConfig.apiToken) { mutableStateOf(serverConfig.apiToken) }
    var wsUrl by remember(serverConfig.wsUrl) { mutableStateOf(serverConfig.wsUrl) }
    var autoSync by remember(serverConfig.autoSyncEnabled) { mutableStateOf(serverConfig.autoSyncEnabled) }
    var showContract by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        "All data is stored safely offline in local SQLite until you connect your server.",
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
                Text("Your Custom Backend Server", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Server Base URL", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("https://api.hassanagro.com/", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_base_url_input")
                )
                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("Bearer API Token / Key", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_token_input")
                )
                OutlinedTextField(
                    value = wsUrl,
                    onValueChange = { wsUrl = it },
                    label = { Text("WebSocket URL (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("wss://api.hassanagro.com/ws/sync", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                            text = "Automatically push/pull when saving stock, bills, or expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoSync, onCheckedChange = { autoSync = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSaveServerConfig(baseUrl, apiToken, wsUrl, autoSync) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_server_config_button")
                    ) {
                        Text("Save", maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = { onTestConnection(baseUrl, apiToken) },
                        enabled = !syncState.isTestingConnection,
                        modifier = Modifier
                            .weight(1f)
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

                Button(
                    onClick = {
                        onSaveAndSync(baseUrl, apiToken, wsUrl, autoSync)
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
                    Text(if (syncState.isSyncing) "Syncing..." else "Sync All Businesses Now", maxLines = 1, softWrap = false)
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
                            text = "Implement these 3 REST endpoints on your own server (Java Spring Boot, Node.js, Laravel, Django, etc.). Every request sends Authorization: Bearer <token> and X-Device-Id.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = """
                                    1) GET /api/v1/health
                                       -> { "status": "ok", "serverTime": 1730000000000 }

                                    2) POST /api/v1/sync/push
                                       Body: { deviceId, pushedAt, businesses[], products[], invoices[], expenses[] }
                                       -> { "accepted": true, "serverTime": 1730000000000 }

                                    3) GET /api/v1/sync/pull?since=<epochMillis>
                                       -> { serverTime, businesses[], products[], invoices[], expenses[] }

                                    4) Optional WebSocket: wss://.../ws/sync
                                       Send any text frame (e.g. "sync") to notify other devices to pull immediately.
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

@Composable
fun BusinessProfileEditorSheet(
    existing: BusinessEntity?,
    onSave: (BusinessEntity, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var tagline by remember { mutableStateOf(existing?.tagline ?: "Crop Protection & Agri Inputs") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var taxId by remember { mutableStateOf(existing?.taxId ?: "") }
    var prefix by remember { mutableStateOf(existing?.invoicePrefix ?: "HA") }
    var defaultTax by remember { mutableStateOf(existing?.defaultTaxRate?.toString() ?: "0") }
    var footer by remember { mutableStateOf(existing?.invoiceFooter ?: "Thank you for your business. Check batch & expiry before use.") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    FormScreenDialog(
        title = if (existing == null) "Add New Business" else "Edit Business Profile",
        subtitle = "Scroll freely — closes only when you tap X or Save",
        onDismiss = onDismiss,
        bottomBar = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Business name is required"
                        return@Button
                    }
                    val derivedPrefix = prefix.trim().ifBlank {
                        name.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(3).ifBlank { "HA" }
                    }
                    val entity = (existing ?: BusinessEntity(name = name.trim())).copy(
                        name = name.trim(),
                        tagline = tagline.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        taxId = taxId.trim(),
                        invoicePrefix = derivedPrefix,
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
                Text(if (existing == null) "Create & Switch Business" else "Save Business Profile", maxLines = 1, softWrap = false)
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMsg = null },
            label = { Text("Business Name *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("e.g. Hassan Agro Branch 2", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            label = { Text("NTN / STRN Tax ID", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                label = { Text("Default Tax (%)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = footer,
            onValueChange = { footer = it },
            label = { Text("Invoice Footer Note", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMsg != null) {
            Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
    }
}
