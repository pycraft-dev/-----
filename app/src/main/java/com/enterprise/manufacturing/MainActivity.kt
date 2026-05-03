package com.enterprise.manufacturing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.enterprise.manufacturing.admin.ui.AdminPanelRoute
import com.enterprise.manufacturing.auth.login.LoginRoute
import com.enterprise.manufacturing.core.navigation.EnterpriseNavHost
import com.enterprise.manufacturing.core.navigation.LaunchIntentExtras
import com.enterprise.manufacturing.core.navigation.UpdateNavArgs
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.core.navigation.ChatListRoute
import com.enterprise.manufacturing.core.navigation.DirectChatRoute
import com.enterprise.manufacturing.defect.ui.DefectCaptureRoute
import com.enterprise.manufacturing.defect.ui.DefectChatRoute
import com.enterprise.manufacturing.defect.ui.DefectListRoute
import com.enterprise.manufacturing.drawings.ui.DrawingChatRoute
import com.enterprise.manufacturing.drawings.ui.DrawingDetailRoute
import com.enterprise.manufacturing.drawings.ui.DrawingListRoute
import com.enterprise.manufacturing.drawings.ui.DrawingUploadRoute
import com.enterprise.manufacturing.sync.ui.SyncRoute
import com.enterprise.manufacturing.timesheet.ui.TimesheetHistoryRoute
import com.enterprise.manufacturing.timesheet.ui.TimesheetTimerRoute
import com.enterprise.manufacturing.update.ui.UpdateRoute
import com.enterprise.manufacturing.ui.theme.ManufacturingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ManufacturingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val activity = LocalContext.current as ComponentActivity
                    val snapshot by mainViewModel.sessionSnapshot.collectAsStateWithLifecycle()
                    val newIntentTick by mainViewModel.newIntentTick.collectAsStateWithLifecycle()

                    LaunchedEffect(snapshot, newIntentTick) {
                        if (snapshot !is SessionSnapshot.Active) return@LaunchedEffect
                        if (!activity.intent.getBooleanExtra(LaunchIntentExtras.OPEN_APP_UPDATE, false)) {
                            return@LaunchedEffect
                        }
                        navController.navigate(UpdateNavArgs.route(false)) {
                            launchSingleTop = true
                        }
                        activity.intent.removeExtra(LaunchIntentExtras.OPEN_APP_UPDATE)
                    }

                    EnterpriseNavHost(
                        navController = navController,
                        sessionSnapshot = mainViewModel.sessionSnapshot,
                        loginContent = {
                            LoginRoute(navController = navController)
                        },
                        adminContent = {
                            AdminPanelRoute(navController = navController)
                        },
                        defectListContent = {
                            DefectListRoute(navController = navController)
                        },
                        defectNewContent = {
                            DefectCaptureRoute(navController = navController)
                        },
                        defectChatContent = {
                            DefectChatRoute(navController = navController)
                        },
                        drawingChatContent = {
                            DrawingChatRoute(navController = navController)
                        },
                        drawingListContent = {
                            DrawingListRoute(navController = navController)
                        },
                        drawingUploadContent = {
                            DrawingUploadRoute(navController = navController)
                        },
                        drawingDetailContent = {
                            DrawingDetailRoute(navController = navController)
                        },
                        timesheetTimerContent = {
                            TimesheetTimerRoute(navController = navController)
                        },
                        timesheetHistoryContent = {
                            TimesheetHistoryRoute(navController = navController)
                        },
                        updateContent = { autoDownloadApk ->
                            UpdateRoute(
                                navController = navController,
                                autoDownloadApk = autoDownloadApk,
                            )
                        },
                        syncContent = {
                            SyncRoute(navController = navController)
                        },
                        chatListContent = {
                            ChatListRoute(navController = navController)
                        },
                        directChatContent = {
                            DirectChatRoute(navController = navController)
                        },
                        onSignOut = { mainViewModel.signOut() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(LaunchIntentExtras.OPEN_APP_UPDATE, false)) {
            mainViewModel.notifyLaunchIntentMayHaveChanged()
        }
    }
}
