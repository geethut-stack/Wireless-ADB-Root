package com.example.wirelessadb

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews

/**
 * Home-screen widget.
 *
 * - In compact mode (1x1) the whole widget becomes a single big on/off
 *   toggle button.
 * - In normal mode it shows the address + a dedicated toggle button.
 *
 * Size threshold: < 100dp wide OR < 60dp tall → compact.
 */
class AdbWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        Thread {
            for (id in appWidgetIds) {
                updateWidget(appContext, appWidgetManager, id, null)
            }
        }.start()
    }

    /** Called when the user resizes the widget. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(context.applicationContext, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return

        val pending = goAsync()
        val appContext = context.applicationContext
        Thread {
            try {
                val prefs = Prefs(appContext)
                val wasActive = AdbController.isWirelessAdbActive()
                if (wasActive) AdbController.disable() else AdbController.enable(prefs.port)
                prefs.isEnabled = !wasActive
            } finally {
                val manager = AppWidgetManager.getInstance(appContext)
                val ids = manager.getAppWidgetIds(
                    ComponentName(appContext, AdbWidgetProvider::class.java)
                )
                for (id in ids) updateWidget(appContext, manager, id, null)
                pending.finish()
            }
        }.start()
    }

    private fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        options: Bundle?
    ) {
        val opts = options ?: manager.getAppWidgetOptions(widgetId)
        val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val compact = minW < 100 || minH < 60

        val views = RemoteViews(context.packageName, R.layout.widget_adb)
        val prefs = Prefs(context)
        val active = AdbController.isWirelessAdbActive()
        val ip = AdbController.getWifiIpAddress()
        val port = AdbController.getCurrentPort() ?: prefs.port

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (active) R.drawable.widget_bg_on else R.drawable.widget_bg_off
        )

        val toggleIntent = Intent(context, AdbWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE
        }
        val togglePi = PendingIntent.getBroadcast(
            context,
            100,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (compact) {
            views.setTextViewText(
                R.id.widget_status,
                if (active) "ADB\nON" else "ADB\nOFF"
            )
            views.setViewVisibility(R.id.widget_address, View.GONE)
            views.setViewVisibility(R.id.widget_toggle, View.GONE)
            views.setOnClickPendingIntent(R.id.widget_root, togglePi)
        } else {
            views.setTextViewText(
                R.id.widget_status,
                if (active) "ADB: ON" else "ADB: OFF"
            )
            views.setTextViewText(
                R.id.widget_address,
                if (active && ip != null) "$ip:$port" else "—"
            )
            views.setTextViewText(
                R.id.widget_toggle,
                if (active) "Disable" else "Enable"
            )
            views.setViewVisibility(R.id.widget_address, View.VISIBLE)
            views.setViewVisibility(R.id.widget_toggle, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.widget_root, openPi)
            views.setOnClickPendingIntent(R.id.widget_status, openPi)
            views.setOnClickPendingIntent(R.id.widget_toggle, togglePi)
        }

        manager.updateAppWidget(widgetId, views)
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.wirelessadb.WIDGET_TOGGLE"
    }
}
