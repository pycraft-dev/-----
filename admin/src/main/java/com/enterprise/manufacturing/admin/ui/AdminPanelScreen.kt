package com.enterprise.manufacturing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.admin.R
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.UserRole

@Composable
fun AdminPanelRoute(
    navController: NavHostController,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    AdminPanelScreen(
        users = users,
        state = form,
        onBack = { navController.popBackStack() },
        onFullNameChange = viewModel::onFullNameChange,
        onPositionChange = viewModel::onPositionChange,
        onGroupKeyChange = viewModel::onGroupKeyChange,
        onRoleChange = viewModel::onRoleChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    users: List<UserEntity>,
    state: AdminUiState,
    onBack: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onGroupKeyChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.admin_section_create),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                CreateUserForm(
                    state = state,
                    onFullNameChange = onFullNameChange,
                    onPositionChange = onPositionChange,
                    onGroupKeyChange = onGroupKeyChange,
                    onRoleChange = onRoleChange,
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
                    UserCard(user)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserForm(
    state: AdminUiState,
    onFullNameChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onGroupKeyChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            value = state.groupKey,
            onValueChange = onGroupKeyChange,
            label = { Text(stringResource(R.string.admin_field_group)) },
            placeholder = { Text(stringResource(R.string.admin_field_group_hint)) },
            singleLine = true,
            enabled = !state.isSaving,
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                value = stringResource(roleLabel(state.selectedRole)),
                onValueChange = {},
                label = { Text(stringResource(R.string.admin_field_role)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                enabled = !state.isSaving,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                UserRole.entries.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(stringResource(roleLabel(role))) },
                        onClick = {
                            onRoleChange(role)
                            expanded = false
                        },
                    )
                }
            }
        }

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
            enabled = !state.isSaving,
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
private fun UserCard(user: UserEntity) {
    val role = parseRole(user.role)
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
            Text(text = stringResource(R.string.admin_row_group, user.groupKey))
            Text(
                text = stringResource(roleLabel(role)),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun parseRole(raw: String): UserRole =
    runCatching { UserRole.valueOf(raw) }.getOrElse { UserRole.WORKER }

@Composable
private fun roleLabel(role: UserRole): Int = when (role) {
    UserRole.ADMIN -> R.string.admin_role_admin
    UserRole.CONSTRUCTOR -> R.string.admin_role_constructor
    UserRole.WORKER -> R.string.admin_role_worker
    UserRole.MASTER -> R.string.admin_role_master
}
