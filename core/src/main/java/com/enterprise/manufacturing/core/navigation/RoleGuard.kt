package com.enterprise.manufacturing.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enterprise.manufacturing.core.R
import com.enterprise.manufacturing.core.model.UserRole

/**
 * Обёртка для экранов NavGraph: при недопустимой роли вызывается [onAccessDenied]
 * (например, `popBackStack` или переход на безопасный маршрут).
 *
 * Важно: колбэк должен быть идемпотентным — повторные вызовы при рекомпозиции не должны зацикливать стек.
 */
@Composable
fun RoleGuardedRoute(
    allowedRoles: Set<UserRole>,
    userRole: UserRole?,
    modifier: Modifier = Modifier,
    onAccessDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    val granted = userRole != null && userRole in allowedRoles

    LaunchedEffect(userRole, allowedRoles) {
        if (!granted) {
            onAccessDenied()
        }
    }

    if (granted) {
        Box(modifier = modifier) {
            content()
        }
    } else {
        RoleGuardFallback(modifier = modifier)
    }
}

/**
 * Явная заглушка «нет роли» — чтобы админ-ветка не оставалась пустой при первом кадре до редиректа.
 */
@Composable
fun RoleGuardFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.core_role_check),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
