package com.enterprise.manufacturing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.admin.R
import com.enterprise.manufacturing.core.db.entity.RoleDefinitionEntity
import com.enterprise.manufacturing.core.db.entity.UserEntity

@Composable
fun AdminPanelRoute(
    navController: NavHostController,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val roles by viewModel.roles.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    AdminPanelScreen(
        users = users,
        roles = roles,
        state = form,
        onBack = { navController.popBackStack() },
        onFullNameChange = viewModel::onFullNameChange,
        onPositionChange = viewModel::onPositionChange,
        onRoleCodeChange = viewModel::onRoleCodeChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onAddRole = viewModel::addCustomRole,
        onClearAddRoleMessage = viewModel::clearAddRoleMessage,
        onUpdateManifestUrlDraftChange = viewModel::onUpdateManifestUrlDraftChange,
        onSaveUpdateManifestUrl = viewModel::saveUpdateManifestUrl,
        onClearUpdateManifestUrlOverride = viewModel::clearUpdateManifestUrlOverride,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    users: List<UserEntity>,
    roles: List<RoleDefinitionEntity>,
    state: AdminUiState,
    onBack: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onRoleCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAddRole: (String, String) -> Unit,
    onClearAddRoleMessage: () -> Unit,
    onUpdateManifestUrlDraftChange: (String) -> Unit,
    onSaveUpdateManifestUrl: () -> Unit,
    onClearUpdateManifestUrlOverride: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddRole by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.admin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.admin_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.admin_section_update_manifest),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.admin_manifest_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.updateManifestUrlDraft,
                            onValueChange = onUpdateManifestUrlDraftChange,
                            label = { Text(stringResource(R.string.admin_manifest_field)) },
                            singleLine = false,
                            minLines = 2,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = onSaveUpdateManifestUrl,
                            ) {
                                Text(stringResource(R.string.admin_manifest_save))
                            }
                            OutlinedButton(onClick = onClearUpdateManifestUrlOverride) {
                                Text(stringResource(R.string.admin_manifest_clear))
                            }
                        }
                        state.updateManifestFeedbackRes?.let { resId ->
                            Text(
                                text = stringResource(resId),
                                color =
                                    if (state.updateManifestFeedbackIsError) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.admin_section_roles_dictionary),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (roles.isEmpty()) {
                            Text(
                                text = stringResource(R.string.admin_roles_loading_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            roles.take(40).forEach { r ->
                                Text(
                                    text = "${r.label} · ${r.code}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                showAddRole = true
                                onClearAddRoleMessage()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.admin_action_add_role))
                            }
                        }
                        state.addRoleMessage?.let { msg ->
                            Text(
                                text = msg,
                                color =
                                    MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.admin_section_create),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                CreateUserForm(
                    roles = roles,
                    state = state,
                    onFullNameChange = onFullNameChange,
                    onPositionChange = onPositionChange,
                    onRoleCodeChange = onRoleCodeChange,
                    onPasswordChange = onPasswordChange,
                    onSubmit = onSubmit,
                )
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.admin_section_list),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (users.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.admin_empty_users),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(users, key = { it.id }) { user ->
                    val rl = roles.find { it.code == user.role }?.label
                    UserCard(user, roleDisplayLabel = rl)
                }
            }
        }

        if (showAddRole) {
            AddRoleDialog(
                onDismiss = { showAddRole = false },
                onConfirm = { code, label ->
                    onAddRole(code, label)
                    showAddRole = false
                },
            )
        }
    }
}

@Composable
private fun AddRoleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_dialog_role_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text(stringResource(R.string.admin_dialog_role_code)) },
                    placeholder = { Text(stringResource(R.string.admin_dialog_role_code_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.admin_dialog_role_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(code, label) },
                enabled = code.isNotBlank() && label.isNotBlank(),
            ) {
                Text(stringResource(R.string.admin_dialog_role_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserForm(
    roles: List<RoleDefinitionEntity>,
    state: AdminUiState,
    onFullNameChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onRoleCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.admin_section_role_login),
                    style = MaterialTheme.typography.titleSmall,
                )

                val selectedRole = roles.firstOrNull { it.code == state.selectedRoleCode }
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = selectedRole?.label ?: state.selectedRoleCode,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.admin_field_role)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        enabled = !state.isSaving,
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(role.label)
                                        Text(
                                            role.code,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                },
                                onClick = {
                                    onRoleCodeChange(role.code)
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.admin_next_login_preview, state.nextLoginPreview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = { Text(stringResource(R.string.admin_field_full_name)) },
            singleLine = true,
            enabled = !state.isSaving,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.position,
            onValueChange = onPositionChange,
            label = { Text(stringResource(R.string.admin_field_position)) },
            singleLine = true,
            enabled = !state.isSaving,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.plainPassword,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.admin_field_password)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !state.isSaving,
        )

        Button(
            onClick = onSubmit,
            enabled = !state.isSaving && roles.any { it.code == state.selectedRoleCode },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.admin_action_create))
        }

        state.statusCreatedLogin?.let { login ->
            Text(
                text = stringResource(R.string.admin_user_created, login),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.errorMessageRes?.let { resId ->
            Text(
                text = stringResource(resId),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun UserCard(user: UserEntity, roleDisplayLabel: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = user.position,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.admin_row_login, user.login))
            Text(text = stringResource(R.string.admin_row_group, roleDisplayLabel ?: user.groupKey))
            Text(
                text = user.role + (roleDisplayLabel?.let { " — $it" } ?: ""),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
