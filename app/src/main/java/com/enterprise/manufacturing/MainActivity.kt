package com.enterprise.manufacturing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.enterprise.manufacturing.admin.ui.AdminPanelRoute
import com.enterprise.manufacturing.auth.login.LoginRoute
import com.enterprise.manufacturing.core.navigation.EnterpriseNavHost
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ManufacturingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val mainViewModel: MainViewModel = hiltViewModel()
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
                        updateContent = {
                            UpdateRoute(navController = navController)
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
}
