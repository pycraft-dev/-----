package com.enterprise.manufacturing.update.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.enterprise.manufacturing.core.navigation.LaunchIntentExtras
import com.enterprise.manufacturing.update.BuildConfig
import com.enterprise.manufacturing.update.R
import com.enterprise.manufacturing.update.data.RemoteUpdateManifest
import com.enterprise.manufacturing.update.data.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Проверка манифеста обновления без UI (WorkManager). При новой версии — уведомление
 * (если разрешены уведомления на API 33+) и intent на лаунчер с [LaunchIntentExtras.OPEN_APP_UPDATE].
 */
@Singleton
class UpdateBackgroundCheck @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateRepository: UpdateRepository,
) {

    suspend fun runIfConfigured() {
        if (BuildConfig.UPDATE_MANIFEST_URL.isBlank()) return
        val installed = installedVersionCode()
        val manifest = updateRepository.fetchManifest().getOrNull() ?: return
        if (manifest.latestVersionCode <= installed) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getInt(KEY_LAST_NOTIFIED_VERSION, 0)
        if (manifest.latestVersionCode <= lastNotified) return
        prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, manifest.latestVersionCode).apply()
        postNotification(manifest)
    }

    private fun installedVersionCode(): Int {
        val pm = context.packageManager
        val pkg = context.packageName
        val info =
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
        return if (Build.VERSION.SDK_INT >= 28) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    }

    private fun postNotification(manifest: RemoteUpdateManifest) {
        if (Build.VERSION.SDK_INT >= 33) {
            val ok =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (!ok) return
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannel(nm)
        val launch =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                putExtra(LaunchIntentExtras.OPEN_APP_UPDATE, true)
            } ?: return

        val pending =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_UPDATE,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val note =
            if (manifest.releaseNotes.isNotBlank()) {
                manifest.releaseNotes.take(NOTES_MAX)
            } else {
                context.getString(R.string.update_bg_notify_text, manifest.latestVersionCode)
            }

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.update_bg_notify_title))
                .setContentText(context.getString(R.string.update_bg_notify_line, manifest.latestVersionCode))
                .setStyle(NotificationCompat.BigTextStyle().bigText(note))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()

        nm.notify(NOTIFICATION_ID_UPDATE, notification)
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ch =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.update_bg_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.update_bg_channel_desc)
            }
        nm.createNotificationChannel(ch)
    }

    private companion object {
        const val PREFS_NAME = "enterprise_update_bg"
        const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version_code"
        const val CHANNEL_ID = "enterprise_app_updates"
        const val NOTIFICATION_ID_UPDATE = 71001
        const val REQUEST_CODE_UPDATE = 71001
        const val NOTES_MAX = 400
    }
}
