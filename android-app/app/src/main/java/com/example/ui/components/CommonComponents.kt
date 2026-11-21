package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpiryStatus
import com.example.data.local.ServerSyncConfig
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.LocalSemanticColors
import com.example.util.Formatters
import com.example.viewmodel.SyncUiState
import java.io.File

/**
 * Full-screen modal form dialog that CANNOT be accidentally dismissed by scrolling down,
 * keeps all inputs cleanly above the soft keyboard with imePadding(), and pins a bottom action bar.
 */
@Composable
fun FormScreenDialog(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    topActions: @Composable (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("form_dialog_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (topActions != null) {
                            topActions()
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        bottomBar()
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/**
 * Sleek single-line 48dp search bar that never bloats vertically or wraps placeholder text.
 */
@Composable
fun CompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTagName: String = "compact_search_input"
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTagName)
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean single-line FilterChip styled with the active business's dynamic theme colors.
 */
@Composable
fun AgriFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = leadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        ),
        modifier = modifier
    )
}

/**
 * Clear audit badge displaying which admin performed a transaction.
 */
@Composable
fun PerformedByPill(
    name: String,
    modifier: Modifier = Modifier
) {
    val display = name.trim().ifBlank { "System" }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "By: $display",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Visual 6-color theme swatch selector for per-business branding.
 */
@Composable
fun ThemeColorSwatchPicker(
    selectedThemeKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalSemanticColors.current.isDark
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppThemeOption.entries.forEach { theme ->
            val selected = theme.key.equals(selectedThemeKey, ignoreCase = true)
            val accent = if (isDark) theme.darkPrimaryColor else theme.primaryColor
            val containerBg = if (isDark) theme.darkPrimaryContainerColor else theme.primaryContainerColor.copy(alpha = 0.6f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(theme.key) }
                    .background(
                        if (selected) containerBg
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("theme_swatch_${theme.key.lowercase()}")
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected theme",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = theme.label.substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) theme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Interactive chip editor allowing a business owner to tap an existing tag to edit/update it in-place,
 * add new tags, or remove tags with the close icon.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditableTagListEditor(
    title: String,
    placeholder: String,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    var draftText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    val activeEditIdx = editingIndex?.takeIf { it in items.indices }
    val isEditing = activeEditIdx != null

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                Text(
                    text = "$title (${items.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEditing) "Editing tag" else "Tap a tag to edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEachIndexed { idx, item ->
                    val isSelectedForEdit = activeEditIdx == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelectedForEdit) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelectedForEdit) 1.5.dp else 1.dp,
                            color = if (isSelectedForEdit) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (isSelectedForEdit) {
                                    editingIndex = null
                                    draftText = ""
                                } else {
                                    editingIndex = idx
                                    draftText = item
                                }
                            }
                            .testTag("${testTagPrefix}_chip_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                start = 10.dp,
                                end = if (items.size > 1) 4.dp else 10.dp,
                                top = 6.dp,
                                bottom = 6.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelectedForEdit) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelectedForEdit) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (items.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (editingIndex == idx) {
                                                editingIndex = null
                                                draftText = ""
                                            } else if (editingIndex != null && idx < editingIndex!!) {
                                                editingIndex = editingIndex!! - 1
                                            }
                                            onItemsChange(items.filterIndexed { i, _ -> i != idx })
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove $item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    placeholder = {
                        Text(
                            if (isEditing) "Edit selected tag..." else placeholder,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingIcon = if (isEditing || draftText.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = {
                                    editingIndex = null
                                    draftText = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_input")
                )
                Button(
                    onClick = {
                        val cleaned = draftText.trim()
                        if (cleaned.isEmpty()) return@Button
                        if (activeEditIdx != null) {
                            val duplicate = items.indices.any { i ->
                                i != activeEditIdx && items[i].equals(cleaned, ignoreCase = true)
                            }
                            if (!duplicate) {
                                val updated = items.toMutableList().apply { set(activeEditIdx, cleaned) }
                                onItemsChange(updated)
                                editingIndex = null
                                draftText = ""
                            }
                        } else if (items.none { it.equals(cleaned, ignoreCase = true) }) {
                            onItemsChange(items + cleaned)
                            draftText = ""
                        }
                    },
                    enabled = draftText.trim().isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("${testTagPrefix}_add_btn")
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (isEditing) "Update" else "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isEditing) "Update" else "Add",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun BusinessLogoAvatar(
    business: BusinessEntity,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val logoPath = business.logoFilePath
    val hasCustomLogo = !logoPath.isNullOrBlank() && File(logoPath).exists()
    val theme = AppThemeOption.fromKey(business.themeKey)
    val initials = remember(business.name) {
        business.name.trim()
            .split(Regex("\\s+"))
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .take(2)
            .ifBlank { "BM" }
    }

    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        shape = CircleShape,
        color = if (hasCustomLogo) Color.White else theme.primaryColor,
        border = BorderStroke(1.dp, if (hasCustomLogo) MaterialTheme.colorScheme.outlineVariant else theme.primaryColor)
    ) {
        if (hasCustomLogo) {
            AsyncImage(
                model = File(logoPath!!),
                contentDescription = stringResource(R.string.cd_brand_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun BusinessSwitcherPill(
    activeBusiness: BusinessEntity,
    businessCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("business_switcher_button")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BusinessLogoAvatar(business = activeBusiness, size = 28.dp)
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = activeBusiness.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (businessCount > 1) "$businessCount businesses" else "Switch / Customize",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_switch_business),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SyncStatusBadge(
    syncState: SyncUiState,
    serverConfig: ServerSyncConfig,
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val (bgColor, fgColor, icon, label) = when {
        syncState.isSyncing -> Quad(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.tertiary,
            Icons.Default.CloudSync,
            "Syncing"
        )
        !serverConfig.isConfigured -> Quad(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.CloudOff,
            "Offline"
        )
        pendingCount > 0 -> Quad(
            semantic.warningBg,
            semantic.warningFg,
            Icons.Default.CloudSync,
            "Sync ($pendingCount)"
        )
        else -> Quad(
            semantic.positiveBg,
            semantic.positiveFg,
            Icons.Default.CloudDone,
            if (syncState.wsConnected) "Live" else "Synced"
        )
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("sync_status_pill")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.cd_sync_now),
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fgColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ExpiryBadge(expiryMillis: Long, modifier: Modifier = Modifier) {
    val semantic = LocalSemanticColors.current
    val status = Formatters.expiryStatus(expiryMillis)
    val (bg, fg) = when (status) {
        ExpiryStatus.EXPIRED -> semantic.negativeBg to semantic.negativeFg
        ExpiryStatus.EXPIRING_SOON -> semantic.warningBg to semantic.warningFg
        ExpiryStatus.WATCH -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        ExpiryStatus.SAFE -> semantic.positiveBg to semantic.positiveFg
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = Formatters.expiryLabel(expiryMillis),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionTestTag: String = "empty_state_action",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.testTag(actionTestTag)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(actionLabel, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSwitcherSheet(
    businesses: List<BusinessEntity>,
    activeBusiness: BusinessEntity,
    canManageBusinesses: Boolean,
    onSelectBusiness: (String) -> Unit,
    onAddNewBusiness: () -> Unit,
    onManageBusinesses: () -> Unit,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Your Businesses",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Each business has its own theme colors, categories, stock, POS & ledger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            val isDark = LocalSemanticColors.current.isDark
            businesses.forEach { biz ->
                val isSelected = biz.id == activeBusiness.id
                val bizTheme = AppThemeOption.fromKey(biz.themeKey)
                val accent = if (isDark) bizTheme.darkPrimaryColor else bizTheme.primaryColor
                val selectedBg = if (isDark) bizTheme.darkPrimaryContainerColor else bizTheme.primaryContainerColor.copy(alpha = 0.55f)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("switch_business_${biz.id}")
                        .clickable {
                            onSelectBusiness(biz.id)
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) selectedBg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BusinessLogoAvatar(business = biz, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = biz.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${bizTheme.label} • ${biz.tagline.ifBlank { biz.invoicePrefix }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active business",
                                tint = accent
                            )
                        }
                    }
                }
            }

            if (canManageBusinesses) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onAddNewBusiness()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_add_business_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Business", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onManageBusinesses()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_manage_businesses_button")
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Customize", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let(onDateSelected)
                    onDismiss()
                }
            ) { Text("Select") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state)
    }
}
