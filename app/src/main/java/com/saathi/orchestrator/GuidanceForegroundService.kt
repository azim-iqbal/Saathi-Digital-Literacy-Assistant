package com.saathi.orchestrator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

/** Keeps a visible, user-controlled guidance session alive while the user switches apps. */
class GuidanceForegroundService : Service() {
    private var ownedSession: String? = null
    private val permissionHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val overlayPermissionChanged = android.app.AppOpsManager.OnOpChangedListener { op, packageName ->
        if (op == android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW && (packageName == null || packageName == this.packageName)) {
            // AppOps may deliver off-main; keep session invalidation on its owning thread.
            permissionHandler.post {
                if (SaathiSession.isActive() && !android.provider.Settings.canDrawOverlays(this)) SaathiSession.stop()
            }
        }
    }
    private val screenOff = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { SaathiSession.stop() }
    }
    override fun onCreate() {
        super.onCreate()
        getSystemService(android.app.AppOpsManager::class.java).startWatchingMode(
            android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, packageName, overlayPermissionChanged
        )
        val filter = android.content.IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenOff, filter, Context.RECEIVER_NOT_EXPORTED)
        else { @Suppress("DEPRECATION") registerReceiver(screenOff, filter) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(
                NotificationChannel(CHANNEL, "Saathi guidance", NotificationManager.IMPORTANCE_LOW)
            )
        }
        // startForegroundService can arrive after the user has already paused/stopped.
        // Fulfil Android's foreground-start contract before any inactive/permission early
        // return in onStartCommand; otherwise that race can kill the process later.
        runCatching {
            startForeground(NOTIFICATION_ID, android.app.Notification.Builder(this, CHANNEL)
                .setSmallIcon(com.saathi.R.drawable.ic_saathi_mark)
                .setContentTitle("Saathi guidance")
                .setContentText("Checking the current session…")
                .setOnlyAlertOnce(true).build())
        }.onFailure { stopSelf() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_STOP) pendingStarts = (pendingStarts - 1).coerceAtLeast(0)
        if (intent?.action == ACTION_STOP) {
            SaathiSession.stopForSession(intent.getStringExtra(EXTRA_SESSION))
            if (!SaathiSession.isActive()) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
            return START_NOT_STICKY
        }
        if (!SaathiSession.isActive()) {
            if (pendingStarts == 0) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
            return START_NOT_STICKY
        }
        ownedSession = SaathiSession.sessionKey()
        // Covers permission loss before the watcher was registered, even without a new tree.
        if (!android.provider.Settings.canDrawOverlays(this)) {
            SaathiSession.stop()
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = android.app.Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Saathi guidance is active")
            .setContentText("Saathi is waiting for the next supported screen. Tap Stop to end guidance.")
            .setOngoing(true)
            .setContentIntent(android.app.PendingIntent.getActivity(this, 45,
                Intent(this, com.saathi.AssistantActivity::class.java),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", Intent(this, GuidanceForegroundService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_SESSION, SaathiSession.sessionKey()).let {
                android.app.PendingIntent.getService(this, 1, it, android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            })
            .build()
        runCatching { startForeground(NOTIFICATION_ID, notification) }.onFailure {
            SaathiSession.stopForSession(ownedSession); stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        getSystemService(android.app.AppOpsManager::class.java).stopWatchingMode(overlayPermissionChanged)
        permissionHandler.removeCallbacksAndMessages(null)
        unregisterReceiver(screenOff)
        SaathiSession.stopForSession(ownedSession)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "saathi_guidance"
        private const val NOTIFICATION_ID = 43
        private const val EXTRA_SESSION = "session_key"
        private const val ACTION_STOP = "com.saathi.action.STOP_GUIDANCE"
        // Main-thread start/stop calls can precede service creation. Stopping the service
        // before it acknowledges startForegroundService can itself trigger Android's
        // foreground-start exception, even if onCreate would immediately promote it.
        private var pendingStarts = 0
        fun start(context: Context) {
            val intent = Intent(context, GuidanceForegroundService::class.java)
            pendingStarts++
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
            } catch (failure: RuntimeException) {
                pendingStarts = (pendingStarts - 1).coerceAtLeast(0)
                throw failure
            }
        }
        fun stop(context: Context) {
            if (pendingStarts == 0) context.stopService(Intent(context, GuidanceForegroundService::class.java))
            // Otherwise onStartCommand sees the inactive session and removes the service
            // after promotion; observation/audio have already been stopped synchronously.
        }
    }
}
