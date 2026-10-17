package com.example.wirelessadb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wirelessadb.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single screen: status, toggle, port, and root — all in one place.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    /** Guards the switch listener while we refresh programmatic state. */
    private var suppressSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        binding.editPort.setText(prefs.port.toString())

        binding.switchAdb.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSwitch) toggleAdb(isChecked)
        }
        binding.btnRefresh.setOnClickListener { refreshState() }
        binding.btnCopy.setOnClickListener { copyAddress() }
        binding.btnSavePort.setOnClickListener { savePort() }
        binding.btnRequestRoot.setOnClickListener { checkRoot(forcePrompt = true) }

        checkRoot(forcePrompt = false)
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    // ----- status ---------------------------------------------------------

    private fun refreshState() {
        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) {
                Triple(
                    AdbController.isWirelessAdbActive(),
                    AdbController.getWifiIpAddress(),
                    AdbController.getCurrentPort() ?: prefs.port
                )
            }
            val (active, ip, port) = state
            bindState(active, ip, port)
        }
    }

    private fun bindState(active: Boolean, ip: String?, port: Int) {
        binding.txtStatus.text = if (active) "Active" else "Inactive"
        binding.txtStatus.setTextColor(
            getColor(if (active) R.color.active else R.color.inactive)
        )

        binding.txtIp.text = "IP: ${ip ?: "—"}"
        binding.txtPort.text = "Port: $port"
        binding.txtAddress.text = if (ip != null) "$ip:$port" else "—"
        binding.txtSubStatus.text =
            if (active) "Enabled · tap to disable" else "Disabled · tap to enable"

        suppressSwitch = true
        binding.switchAdb.isChecked = active
        suppressSwitch = false
    }

    // ----- toggle ---------------------------------------------------------

    private fun toggleAdb(enable: Boolean) {
        lifecycleScope.launch {
            binding.switchAdb.isEnabled = false
            binding.btnRefresh.isEnabled = false

            val result = withContext(Dispatchers.IO) {
                if (enable) AdbController.enable(prefs.port) else AdbController.disable()
            }

            binding.switchAdb.isEnabled = true
            binding.btnRefresh.isEnabled = true

            if (result.success) {
                prefs.isEnabled = enable
                Toast.makeText(
                    this@MainActivity,
                    if (enable) "Wireless ADB enabled" else "Wireless ADB disabled",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Root command failed: ${result.output.ifBlank { "exit ${result.exitCode}" }}",
                    Toast.LENGTH_LONG
                ).show()
            }
            refreshState()
        }
    }

    // ----- port -----------------------------------------------------------

    private fun savePort() {
        val port = binding.editPort.text?.toString()?.trim()?.toIntOrNull()
        if (port == null || port !in 1024..65535) {
            binding.editPort.error = "Port must be 1024–65535"
            return
        }
        binding.editPort.error = null

        val oldPort = prefs.port
        prefs.port = port
        val wasActive = binding.switchAdb.isChecked
        Toast.makeText(this, "Port saved: $port", Toast.LENGTH_SHORT).show()

        if (wasActive && oldPort != port) {
            lifecycleScope.launch {
                binding.switchAdb.isEnabled = false
                val r = withContext(Dispatchers.IO) { AdbController.enable(port) }
                binding.switchAdb.isEnabled = true
                Toast.makeText(
                    this@MainActivity,
                    if (r.success) "Reapplied on port $port"
                    else "Reapply failed: ${r.output.ifBlank { "exit ${r.exitCode}" }}",
                    Toast.LENGTH_SHORT
                ).show()
                refreshState()
            }
        } else {
            refreshState()
        }
    }

    // ----- root -----------------------------------------------------------

    private fun checkRoot(forcePrompt: Boolean) {
        binding.txtRootStatus.text = "Checking…"
        binding.btnRequestRoot.isEnabled = false

        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { RootManager.isRootAvailable() }
            binding.btnRequestRoot.isEnabled = true
            if (ok) {
                binding.txtRootStatus.text = "Root granted ✓"
                binding.txtRootStatus.setTextColor(getColor(R.color.active))
                if (forcePrompt) {
                    Toast.makeText(
                        this@MainActivity,
                        "Root access confirmed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                binding.txtRootStatus.text =
                    if (forcePrompt) "Root denied ✗" else "Root not granted — tap to request"
                binding.txtRootStatus.setTextColor(getColor(R.color.inactive))
            }
        }
    }

    // ----- copy -----------------------------------------------------------

    private fun copyAddress() {
        val text = binding.txtAddress.text?.toString().orEmpty()
        if (text.isBlank() || text == "—") return
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("adb address", text))
        Toast.makeText(this, "Copied $text", Toast.LENGTH_SHORT).show()
    }
}
