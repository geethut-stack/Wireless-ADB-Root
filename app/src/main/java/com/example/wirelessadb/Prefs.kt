package com.example.wirelessadb

import android.content.Context

/** Tiny SharedPreferences wrapper. */
class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("wireless_adb_prefs", Context.MODE_PRIVATE)

    var port: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    /** Last known desired state (best-effort UI hint; the real state is read
     *  from the system property). */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    companion object {
        const val KEY_PORT = "port"
        const val KEY_ENABLED = "enabled"
        const val DEFAULT_PORT = 5555
    }
}
