package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.data.local.AuthSession
import com.example.data.local.BusinessEntity
import com.example.data.remote.TeamPermissionItemDto
import com.example.data.remote.TeamRoleDto
import com.example.data.remote.TeamUserDto
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.AppConfirmDeleteDialog
import com.example.ui.components.FormScreenDialog
import com.example.ui.theme.LocalSemanticColors
import com.example.viewmodel.TeamUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamManagementSection(
    activeBusiness: BusinessEntity,
    authSession: AuthSession,
    teamState: TeamUiState,
    onLoadTeam: () -> Unit,
    onSaveRole: (String?, String, String, List<String>, () -> Unit) -> Unit,
    onDeleteRole: (String) -> Unit,
    onSaveUser: (String?, String, String, String, String, String, List<String>?, () -> Unit) -> Unit,
    onToggleUser: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    var showUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<TeamUserDto?>(null) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var editingRole by remember { mutableStateOf<TeamRoleDto?>(null) }
    var deleteUserTarget by remember { mutableStateOf<TeamUserDto?>(null) }
    var deleteRoleTarget by remember { mutableStateOf<TeamRoleDto?>(null) }

    LaunchedEffect(activeBusiness.id) {
        onLoadTeam()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header banner
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeBusiness.name}: Team & Roles",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Add Co-Owners, Salesmen, or Moderators with exact permissions. Every sale & entry logs their name.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onLoadTeam, enabled = !teamState.isLoading) {
                        if (teamState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh team")
                        }
                    }
                }
            }
        }

        if (teamState.errorMessage != null) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = semantic.negativeBg,
                    border = BorderStroke(1.dp, semantic.negativeFg.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = teamState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.negativeFg,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onLoadTeam) { Text("Retry") }
                    }
                }
            }
        }

        // Team Members Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Team Members (${teamState.users.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = {
                        editingUser = null
                        showUserDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_team_user_button")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Add Member", maxLines = 1, softWrap = false)
                }
            }
        }

        if (teamState.isLoading && teamState.users.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (teamState.users.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "No team members loaded yet. Tap \"Add Member\" to create a Co-Owner, Salesman, or Moderator login for ${activeBusiness.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(teamState.users, key = { it.id }) { member ->
                val isSelf = member.id == authSession.userId
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (member.isOwner) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = member.name + if (isSelf) " (You)" else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (member.isActive) semantic.positiveBg else semantic.negativeBg
                                    ) {
                                        Text(
                                            text = if (member.isActive) "Active" else "Disabled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (member.isActive) semantic.positiveFg else semantic.negativeFg,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = member.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (member.isOwner) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = member.roleName.ifBlank { "Staff" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (member.isOwner) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (member.permissions.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                member.permissions.take(6).forEach { key ->
                                    val label = teamState.permissionsCatalog
                                        .firstOrNull { it.key == key }?.label ?: key
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (member.permissions.size > 6) {
                                    Text(
                                        text = "+${member.permissions.size - 6} more",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }

                        if (!isSelf) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onToggleUser(member.id) }) {
                                    Text(if (member.isActive) "Disable" else "Enable")
                                }
                                IconButton(
                                    onClick = {
                                        editingUser = member
                                        showUserDialog = true
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.cd_edit_item),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { deleteUserTarget = member },
                                    modifier = Modifier.size(38.dp)
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

        // Custom Roles Section Header
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Roles (${teamState.roles.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    onClick = {
                        editingRole = null
                        showRoleDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_team_role_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Role", maxLines = 1, softWrap = false)
                }
            }
        }

        items(teamState.roles, key = { it.id }) { role ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = if (role.isSystem) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = role.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (role.isSystem) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Built-in",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${role.permissions.size} permissions" +
                                if (role.description.isNotBlank()) " • ${role.description}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!role.isSystem) {
                        IconButton(
                            onClick = {
                                editingRole = role
                                showRoleDialog = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit_item),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { deleteRoleTarget = role },
                            modifier = Modifier.size(36.dp)
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

    if (showUserDialog) {
        TeamUserEditorDialog(
            businessName = activeBusiness.name,
            existing = editingUser,
            roles = teamState.roles,
            permissionsCatalog = teamState.permissionsCatalog,
            isSaving = teamState.isSaving,
            onSave = { name, email, password, roleId, customRoleName, customPerms ->
                onSaveUser(
                    editingUser?.id,
                    name,
                    email,
                    password,
                    roleId,
                    customRoleName,
                    customPerms
                ) {
                    showUserDialog = false
                    editingUser = null
                }
            },
            onDismiss = {
                showUserDialog = false
                editingUser = null
            }
        )
    }

    if (showRoleDialog) {
        TeamRoleEditorDialog(
            businessName = activeBusiness.name,
            existing = editingRole,
            permissionsCatalog = teamState.permissionsCatalog,
            isSaving = teamState.isSaving,
            onSave = { name, description, perms ->
                onSaveRole(editingRole?.id, name, description, perms) {
                    showRoleDialog = false
                    editingRole = null
                }
            },
            onDismiss = {
                showRoleDialog = false
                editingRole = null
            }
        )
    }

    deleteUserTarget?.let { target ->
        AppConfirmDeleteDialog(
            title = "Remove Team Member?",
            itemLabel = "${target.name} (${target.email})",
            message = "Remove this member from ${activeBusiness.name}? They will immediately lose access to this business.",
            confirmText = "Remove",
            onConfirm = { onDeleteUser(target.id) },
            onDismiss = { deleteUserTarget = null }
        )
    }

    deleteRoleTarget?.let { target ->
        AppConfirmDeleteDialog(
            title = "Delete Custom Role?",
            itemLabel = target.name,
            message = "Delete this custom role? Make sure no team members are currently assigned to it.",
            confirmText = "Delete",
            onConfirm = { onDeleteRole(target.id) },
            onDismiss = { deleteRoleTarget = null }
        )
    }
}

@Composable
private fun TeamUserEditorDialog(
    businessName: String,
    existing: TeamUserDto?,
    roles: List<TeamRoleDto>,
    permissionsCatalog: List<TeamPermissionItemDto>,
    isSaving: Boolean,
    onSave: (String, String, String, String, String, List<String>?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var useCustomPermissions by remember { mutableStateOf(false) }
    var selectedRoleId by remember(roles, existing) {
        mutableStateOf(
            existing?.roleId?.takeIf { id -> roles.any { it.id == id } }
                ?: roles.firstOrNull { !it.id.equals("role-business-owner", ignoreCase = true) }?.id
                ?: roles.firstOrNull()?.id
                ?: ""
        )
    }
    var customRoleTitle by remember { mutableStateOf("") }
    var selectedPermissions by remember(existing, selectedRoleId) {
        mutableStateOf(
            existing?.permissions?.toSet()
                ?: roles.firstOrNull { it.id == selectedRoleId }?.permissions?.toSet()
                ?: setOf("view_dashboard", "use_pos", "collect_credit")
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    FormScreenDialog(
        title = if (existing == null) "Add Team Member" else "Edit Team Member",
        subtitle = "$businessName • Co-Owner, Salesman or Moderator",
        onDismiss = onDismiss,
        bottomBar = {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank()) {
                        errorMsg = "Name and email are required"
                        return@Button
                    }
                    if (existing == null && password.length < 6) {
                        errorMsg = "Password must be at least 6 characters"
                        return@Button
                    }
                    if (!useCustomPermissions && selectedRoleId.isBlank()) {
                        errorMsg = "Select a role or turn on custom permissions"
                        return@Button
                    }
                    if (useCustomPermissions && selectedPermissions.isEmpty()) {
                        errorMsg = "Select at least one allowed feature for this user"
                        return@Button
                    }
                    onSave(
                        name.trim(),
                        email.trim(),
                        password,
                        if (useCustomPermissions) "" else selectedRoleId,
                        if (useCustomPermissions) customRoleTitle.trim().ifBlank { "${name.trim()} Access" } else "",
                        if (useCustomPermissions) selectedPermissions.toList() else null
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_team_user_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (existing == null) "Create Member Login" else "Save Changes",
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMsg = null },
            label = { Text("Full Name *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("e.g. Bilal Ahmed (Salesman)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("team_user_name_input")
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMsg = null },
            label = { Text("Login Email *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("team_user_email_input")
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMsg = null },
            label = {
                Text(
                    if (existing == null) "Password (min 6 chars) *" else "New Password (leave blank to keep)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("team_user_password_input")
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Custom Feature Permissions", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Pick exact checkboxes for what this person can & cannot do",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = useCustomPermissions,
                onCheckedChange = { useCustomPermissions = it },
                modifier = Modifier.testTag("team_user_custom_perms_switch")
            )
        }

        if (!useCustomPermissions) {
            Text(
                text = "Assign Role (choose Business Owner for a Co-Owner)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roles.forEach { role ->
                    AgriFilterChip(
                        selected = selectedRoleId == role.id,
                        onClick = {
                            selectedRoleId = role.id
                            selectedPermissions = role.permissions.toSet()
                        },
                        label = role.name
                    )
                }
            }
            val chosen = roles.firstOrNull { it.id == selectedRoleId }
            if (chosen != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${chosen.name}: Included Access (${chosen.permissions.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        val labels = chosen.permissions.map { key ->
                            permissionsCatalog.firstOrNull { it.key == key }?.label ?: key
                        }
                        Text(
                            text = labels.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = customRoleTitle,
                onValueChange = { customRoleTitle = it },
                label = { Text("Custom Role Title (e.g. Field Salesman)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PermissionsCheckboxList(
                catalog = permissionsCatalog,
                selectedKeys = selectedPermissions,
                onToggle = { key ->
                    selectedPermissions = if (selectedPermissions.contains(key)) {
                        selectedPermissions - key
                    } else {
                        selectedPermissions + key
                    }
                }
            )
        }

        if (errorMsg != null) {
            Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TeamRoleEditorDialog(
    businessName: String,
    existing: TeamRoleDto?,
    permissionsCatalog: List<TeamPermissionItemDto>,
    isSaving: Boolean,
    onSave: (String, String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var selectedPermissions by remember {
        mutableStateOf(
            existing?.permissions?.toSet() ?: setOf("view_dashboard", "use_pos", "collect_credit")
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    FormScreenDialog(
        title = if (existing == null) "Create Custom Role" else "Edit Role",
        subtitle = businessName,
        onDismiss = onDismiss,
        bottomBar = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Role name is required"
                        return@Button
                    }
                    if (selectedPermissions.isEmpty()) {
                        errorMsg = "Select at least one permission"
                        return@Button
                    }
                    onSave(name.trim(), description.trim(), selectedPermissions.toList())
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_team_role_button")
            ) {
                Text(if (existing == null) "Create Role" else "Update Role", maxLines = 1, softWrap = false)
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMsg = null },
            label = { Text("Role Name *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("e.g. Salesman, Moderator, Cashier, Field Officer") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("team_role_name_input")
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Short Description (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Allowed Features (${selectedPermissions.size} selected)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        PermissionsCheckboxList(
            catalog = permissionsCatalog,
            selectedKeys = selectedPermissions,
            onToggle = { key ->
                selectedPermissions = if (selectedPermissions.contains(key)) {
                    selectedPermissions - key
                } else {
                    selectedPermissions + key
                }
            }
        )
        if (errorMsg != null) {
            Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PermissionsCheckboxList(
    catalog: List<TeamPermissionItemDto>,
    selectedKeys: Set<String>,
    onToggle: (String) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            catalog.forEachIndexed { index, item ->
                val checked = selectedKeys.contains(item.key)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(item.key) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggle(item.key) }
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (index < catalog.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}
