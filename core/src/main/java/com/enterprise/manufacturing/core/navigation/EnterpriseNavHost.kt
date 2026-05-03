package com.enterprise.manufacturing.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.enterprise.manufacturing.core.model.BuiltInRoleCodes
import com.enterprise.manufacturing.core.session.SessionSnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * Корневой граф: Bootstrap → Login/Home, админка под [RoleGuardedRoute].
 *
 * @param loginContent экран входа из модуля :auth.
 * @param adminContent панель администратора из модуля :admin (только при роли ADMIN внутри гарда).
 * @param defectListContent список заявок брака (:defect).
 * @param defectNewContent камера / создание заявки (:defect).
 * @param defectChatContent чат по заявке (:defect), аргумент маршрута читается через SavedStateHandle во ViewModel.
 * @param drawingChatContent чат по версии чертежа (:drawings).
 * @param chatListContent список чатов / пользователей (:core).
 * @param directChatContent личный чат (:core), аргумент [DirectChatNavArgs.PeerUserId] во ViewModel.
 * @param drawingListContent список версий чертежей (:drawings).
 * @param drawingUploadContent загрузка новой версии PDF/DWG (:drawings).
 * @param drawingDetailContent карточка версии и превью PDF (:drawings).
 * @param timesheetTimerContent таймер учёта времени (:timesheet).
 * @param timesheetHistoryContent история интервалов и экспорт CSV (:timesheet).
 * @param updateContent проверка версии и установка APK (:update); `true` — авто-проверка и скачивание с рабочего стола.
 * @param syncContent статус синхронизации и ручной запуск WorkManager (:sync).
 */
@Composable
fun EnterpriseNavHost(
    navController: NavHostController,
    sessionSnapshot: StateFlow<SessionSnapshot>,
    modifier: Modifier = Modifier,
    loginContent: @Composable () -> Unit,
    adminContent: @Composable () -> Unit,
    defectListContent: @Composable () -> Unit,
    defectNewContent: @Composable () -> Unit,
    defectChatContent: @Composable () -> Unit,
    drawingChatContent: @Composable () -> Unit,
    drawingListContent: @Composable () -> Unit,
    drawingUploadContent: @Composable () -> Unit,
    drawingDetailContent: @Composable () -> Unit,
    timesheetTimerContent: @Composable () -> Unit,
    timesheetHistoryContent: @Composable () -> Unit,
    updateContent: @Composable (autoDownloadApk: Boolean) -> Unit,
    syncContent: @Composable () -> Unit,
    chatListContent: @Composable () -> Unit,
    directChatContent: @Composable () -> Unit,
    onSignOut: () -> Unit,
) {
    val snapshot by sessionSnapshot.collectAsStateWithLifecycle()
    val sessionRoleCode: String? = when (val s = snapshot) {
        is SessionSnapshot.Active -> s.roleCode
        else -> null
    }
    val showDefectEntry = snapshot is SessionSnapshot.Active
    val showDrawingsEntry = snapshot is SessionSnapshot.Active
    val showTimesheetEntry = snapshot is SessionSnapshot.Active
    val showUpdateEntry = snapshot is SessionSnapshot.Active
    val showSyncEntry = snapshot is SessionSnapshot.Active
    val showMessengerEntry = snapshot is SessionSnapshot.Active

    LaunchedEffect(snapshot) {
        if (snapshot is SessionSnapshot.LoggedOut) {
            val route = navController.currentDestination?.route
            if (route != null &&
                route != AppRoute.Login.route &&
                route != AppRoute.Bootstrap.route
            ) {
                navController.navigate(AppRoute.Login.route) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Bootstrap.route,
        modifier = modifier,
    ) {
        composable(route = AppRoute.Bootstrap.route) {
            LaunchedEffect(snapshot) {
                when (snapshot) {
                    SessionSnapshot.Loading -> Unit
                    SessionSnapshot.LoggedOut -> {
                        if (navController.currentDestination?.route != AppRoute.Login.route) {
                            navController.navigate(AppRoute.Login.route) {
                                popUpTo(AppRoute.Bootstrap.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    is SessionSnapshot.Active -> {
                        if (navController.currentDestination?.route != AppRoute.ChatList.route) {
                            navController.navigate(AppRoute.ChatList.route) {
                                popUpTo(AppRoute.Bootstrap.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        composable(route = AppRoute.Login.route) {
            loginContent()
        }

        composable(route = AppRoute.Home.route) {
            HomePlaceholderScreen(
                onSignOut = onSignOut,
                showAdminEntry = sessionRoleCode == BuiltInRoleCodes.ADMIN,
                onOpenAdmin = {
                    navController.navigate(AppRoute.Admin.route)
                },
                showDefectEntry = showDefectEntry,
                onOpenDefects = {
                    navController.navigate(AppRoute.DefectList.route)
                },
                showDrawingsEntry = showDrawingsEntry,
                onOpenDrawings = {
                    navController.navigate(AppRoute.DrawingList.route)
                },
                showTimesheetEntry = showTimesheetEntry,
                onOpenTimesheet = {
                    navController.navigate(AppRoute.Timesheet.route)
                },
                showUpdateEntry = showUpdateEntry,
                onOpenUpdate = {
                    navController.navigate(UpdateNavArgs.route(autoDownload = false))
                },
                showSyncEntry = showSyncEntry,
                onOpenSync = {
                    navController.navigate(AppRoute.Sync.route)
                },
                showMessengerEntry = showMessengerEntry,
                onOpenMessenger = {
                    navController.navigate(AppRoute.ChatList.route)
                },
            )
        }

        composable(route = AppRoute.Admin.route) {
            RoleGuardedRoute(
                allowedRoleCodes = AdminDestinationRoles,
                activeRoleCode = sessionRoleCode,
                onAccessDenied = {
                    navController.navigate(AppRoute.ChatList.route) {
                        launchSingleTop = true
                    }
                },
            ) {
                adminContent()
            }
        }

        composable(route = AppRoute.DefectList.route) {
            defectListContent()
        }

        composable(route = AppRoute.DefectNew.route) {
            defectNewContent()
        }

        composable(
            route = DefectNavArgs.ChatRoute,
            arguments = listOf(
                navArgument("defectId") { type = NavType.StringType },
            ),
        ) {
            defectChatContent()
        }

        composable(route = AppRoute.DrawingList.route) {
            drawingListContent()
        }

        composable(
            route = DrawingNavArgs.UploadRoute,
            arguments = listOf(
                navArgument("seriesArg") { type = NavType.StringType },
            ),
        ) {
            drawingUploadContent()
        }

        composable(
            route = DrawingNavArgs.DetailRoute,
            arguments = listOf(
                navArgument("revisionId") { type = NavType.LongType },
            ),
        ) {
            drawingDetailContent()
        }

        composable(
            route = DrawingNavArgs.ChatRoute,
            arguments = listOf(
                navArgument("revisionId") { type = NavType.LongType },
            ),
        ) {
            drawingChatContent()
        }

        composable(route = AppRoute.Timesheet.route) {
            timesheetTimerContent()
        }

        composable(route = AppRoute.TimesheetHistory.route) {
            timesheetHistoryContent()
        }

        composable(
            route = "${AppRoute.Update.route}?${UpdateNavArgs.AutoDownload}={${UpdateNavArgs.AutoDownload}}",
            arguments =
                listOf(
                    navArgument(UpdateNavArgs.AutoDownload) {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
        ) { entry ->
            val auto = (entry.arguments?.getInt(UpdateNavArgs.AutoDownload) ?: 0) == 1
            updateContent(auto)
        }

        composable(route = AppRoute.Sync.route) {
            syncContent()
        }

        composable(route = AppRoute.ChatList.route) {
            chatListContent()
        }

        composable(
            route = AppRoute.DirectChat.route,
            arguments =
                listOf(
                    navArgument(DirectChatNavArgs.PeerUserId) { type = NavType.LongType },
                ),
        ) {
            directChatContent()
        }
    }
}
