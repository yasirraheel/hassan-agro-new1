package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AuthSession
import com.example.data.local.BusinessEntity
import com.example.data.local.ServerSyncConfig
import com.example.ui.screens.SettingsTabKey
import com.example.viewmodel.SyncUiState

data class DrawerDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigationDrawerSheet(
    activeBusiness: BusinessEntity,
    businessCount: Int,
    authSession: AuthSession,
    serverConfig: ServerSyncConfig,
    syncState: SyncUiState,
    pendingSyncCount: Int,
    destinations: List<DrawerDestinationItem>,
    currentRoute: String,
    canInventory: Boolean,
    canManageBusinesses: Boolean,
    canManageTeam: Boolean,
    canManageSettings: Boolean,
    onNavigateRoute: (String) -> Unit,
    onOpenSettings: (SettingsTabKey) -> Unit,
    onOpenBusinessSwitcher: () -> Unit,
    onScanStockBarcode: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRestrictedStaff = !canManageBusinesses && !canManageTeam && !canManageSettings
    val itemPadding = NavigationDrawerItemDefaults.ItemPadding
    val itemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurface
    )

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(min = 280.dp, max = 320.dp)
            .fillMaxHeight()
            .testTag("app_navigation_drawer"),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Branded Business & User Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BusinessLogoAvatar(
                            business = activeBusiness,
                            size = 48.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeBusiness.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeBusiness.tagline.ifBlank { activeBusiness.currencySymbol },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Active signed-in user identity card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = authSession.userName.ifBlank { "Admin" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (authSession.userEmail.isNotBlank()) {
                                    Text(
                                        text = authSession.userEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = authSession.roleName.ifBlank { "Staff" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable Navigation Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp)
            ) {
                DrawerSectionTitle(stringResource(R.string.drawer_section_workspace))

                destinations.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = dest.label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = selected,
                        onClick = { onNavigateRoute(dest.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier
                            .padding(itemPadding)
                            .testTag("drawer_nav_${dest.route}")
                    )
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                DrawerSectionTitle(stringResource(R.string.drawer_section_management))

                // Settings & Customization (or My Account & Sync for restricted staff)
                val settingsLabel = if (isRestrictedStaff) {
                    stringResource(R.string.drawer_my_account)
                } else {
                    stringResource(R.string.drawer_settings_customization)
                }
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = settingsLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    selected = false,
                    onClick = { onOpenSettings(SettingsTabKey.BUSINESSES) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = settingsLabel
                        )
                    },
                    colors = itemColors,
                    modifier = Modifier
                        .padding(itemPadding)
                        .testTag("drawer_settings")
                )

                // Team & Roles shortcut (owners / managers only)
                if (canManageTeam) {
                    val teamLabel = stringResource(R.string.drawer_team_roles)
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = teamLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = false,
                        onClick = { onOpenSettings(SettingsTabKey.TEAM) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AdminPanelSettings,
                                contentDescription = teamLabel
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier
                            .padding(itemPadding)
                            .testTag("drawer_team_roles")
                    )
                }

                // Server Sync settings shortcut (owners / settings managers)
                if (canManageSettings || canManageBusinesses) {
                    val syncSettingsLabel = stringResource(R.string.drawer_server_sync)
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = syncSettingsLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = false,
                        onClick = { onOpenSettings(SettingsTabKey.SYNC) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CloudSync,
                                contentDescription = syncSettingsLabel
                            )
                        },
                        badge = if (pendingSyncCount > 0) {
                            {
                                DrawerCountBadge(text = "$pendingSyncCount")
                            }
                        } else null,
                        colors = itemColors,
                        modifier = Modifier
                            .padding(itemPadding)
                            .testTag("drawer_server_sync")
                    )
                }

                // Switch / Add Business shortcut
                if (canManageBusinesses || businessCount > 1) {
                    val switchLabel = stringResource(R.string.drawer_switch_business)
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = switchLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = false,
                        onClick = onOpenBusinessSwitcher,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = switchLabel
                            )
                        },
                        badge = {
                            DrawerCountBadge(text = "$businessCount")
                        },
                        colors = itemColors,
                        modifier = Modifier
                            .padding(itemPadding)
                            .testTag("drawer_switch_business")
                    )
                }

                // Quick Stock Barcode Scanner
                if (canInventory) {
                    val scanLabel = stringResource(R.string.drawer_scan_barcode)
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = scanLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = false,
                        onClick = onScanStockBarcode,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = scanLabel
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier
                            .padding(itemPadding)
                            .testTag("drawer_scan_barcode")
                    )
                }

                // Sync Now action
                val syncNowLabel = stringResource(R.string.drawer_sync_now)
                val syncStatusText = when {
                    syncState.isSyncing -> "Syncing..."
                    !serverConfig.isConfigured -> "Offline"
                    pendingSyncCount > 0 -> "$pendingSyncCount pending"
                    else -> "Synced"
                }
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = syncNowLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    selected = false,
                    onClick = onSyncNow,
                    icon = {
                        if (syncState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    !serverConfig.isConfigured -> Icons.Default.CloudOff
                                    pendingSyncCount > 0 -> Icons.Default.Sync
                                    else -> Icons.Default.CloudDone
                                },
                                contentDescription = syncNowLabel
                            )
                        }
                    },
                    badge = {
                        Text(
                            text = syncStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pendingSyncCount > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    colors = itemColors,
                    modifier = Modifier
                        .padding(itemPadding)
                        .testTag("drawer_sync_now")
                )
            }

            // Pinned Footer: Sign Out
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                val signOutLabel = stringResource(R.string.drawer_sign_out)
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = signOutLabel,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    selected = false,
                    onClick = onSignOut,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = signOutLabel
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.error,
                        unselectedTextColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .padding(itemPadding)
                        .testTag("drawer_sign_out")
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerCountBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
