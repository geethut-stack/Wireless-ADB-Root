package com.example.wirelessadb

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * All logic for enabling/disabling ADB over TCP and reading the current
 * connection address. Uses root (see [RootManager]).
 */
object AdbController {

    /** Enables adb over TCP on the given port. */
    fun enable(port: Int): RootManager.RootResult {
        return RootManager.run(
            "settings put global adb_enabled 1",
            "setprop service.adb.tcp.port $port",
            "setprop ctl.restart adbd"
        )
    }

    /** Disables adb over TCP (port = -1) and restarts adbd. */
    fun disable(): RootManager.RootResult {
        return RootManager.run(
            "setprop service.adb.tcp.port -1",
            "setprop ctl.restart adbd"
        )
    }

    /** True when `service.adb.tcp.port` holds a valid (>0) port. */
    fun isWirelessAdbActive(): Boolean = getCurrentPort() != null

    /** Reads `service.adb.tcp.port`; null when unset / -1 / non-numeric. */
    fun getCurrentPort(): Int? {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("getprop", "service.adb.tcp.port")
            )
            val value = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            value.toIntOrNull()?.takeIf { it > 0 }
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Returns the first non-loopback IPv4 address on a Wi-Fi or AP interface,
     * or null if none is available.
     */
    fun getWifiIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .filter { nif ->
                    val n = nif.name.lowercase()
                    n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("eth")
                }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        } catch (t: Throwable) {
            null
        }
    }
}
