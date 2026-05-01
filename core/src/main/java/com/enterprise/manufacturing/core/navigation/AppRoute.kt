package com.enterprise.manufacturing.core.navigation

import com.enterprise.manufacturing.core.model.UserRole

/**
 * Корневые маршруты приложения. Вложенные графы фич добавятся с модулями :defect, :timesheet и т.д.
 */
sealed class AppRoute(val route: String) {
    /** Промежуточный экран: чтение DataStore и выбор Login/Home. */
    data object Bootstrap : AppRoute("bootstrap")

    data object Login : AppRoute("login")

    data object Home : AppRoute("home")

    /** Фиксация брака (модуль :defect). */
    data object DefectList : AppRoute("defect_list")

    data object DefectNew : AppRoute("defect_new")

    /** Доступен только при [UserRole.ADMIN] через [RoleGuardedRoute]. */
    data object Admin : AppRoute("admin")

    /** Чертежи и документация (`:drawings`). */
    data object DrawingList : AppRoute("drawing_list")

    /** Учёт времени (`:timesheet`). */
    data object Timesheet : AppRoute("timesheet")

    data object TimesheetHistory : AppRoute("timesheet_history")

    /** Обновление приложения (`:update`). */
    data object Update : AppRoute("app_update")

    /** Статус синхронизации (`:sync`). */
    data object Sync : AppRoute("sync_hub")

    /** Главный экран мессенджера (`:core`). */
    data object ChatHub : AppRoute("chat_hub")
}

/** Аргументы навигации для модуля чертежей. */
object DrawingNavArgs {
    const val UploadRoute = "drawing_upload/{seriesArg}"

    fun uploadRoute(seriesId: String?): String =
        if (seriesId == null) "drawing_upload/new" else "drawing_upload/$seriesId"

    const val DetailRoute = "drawing_detail/{revisionId}"

    fun detailRoute(revisionId: Long): String = "drawing_detail/$revisionId"

    const val ChatRoute = "drawing_chat/{revisionId}"

    fun chatRoute(revisionId: Long): String = "drawing_chat/$revisionId"
}

/** Аргументы навигации для экранов брака. */
object DefectNavArgs {
    const val ChatRoute = "defect_chat/{defectId}"

    fun chatRoute(defectId: String): String = "defect_chat/$defectId"
}

/**
 * Стабильная ссылка на набор ролей для `LaunchedEffect` в [RoleGuardedRoute]
 * (не создавать новый `setOf` на каждой рекомпозиции NavHost).
 */
val AdminDestinationRoles: Set<UserRole> = setOf(UserRole.ADMIN)
