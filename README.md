# Wireless ADB Root

> Toggle Wireless ADB from your phone's home screen — no PC, no USB cable, no `adb tcpip`.

A tiny Android app for **rooted devices** that turns TCP/IP ADB on and off with a single tap, shows you the exact `ip:port` to connect to, and ships a resizable home-screen widget that shrinks down to a single 1×1 cell.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-24-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-35-blue)
![Language](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-green)

---

## Why

Every "wireless ADB" app on the Play Store is either a paid widget, stuffed with ads, or just a shell shortcut. If you're rooted, this is the whole job:

```sh
setprop service.adb.tcp.port 5555
setprop ctl.restart adbd
```

This app wraps exactly that in a clean UI and a widget, and nothing else. No analytics, no network permission abuse, no background services.

## Features

- **One-tap toggle** — enable/disable Wireless ADB from the app or the widget
- **Live connection address** — shows the current `IP:PORT` with a copy button
- **Custom port** — change the TCP port (1024–65535); re-applied automatically if ADB is already running
- **Root status check** — explicit "Request root permission" button that triggers your root manager's prompt
- **Resizable widget** — drag it down to **1×1** and it becomes a single big toggle; enlarge it to also see the address
- **Zero fluff** — no ads, no analytics, ~5 Kotlin files

## Screenshots
<!-- Drop your screenshots in a /screenshots folder and reference them here -->

| Main screen | Widget (compact) | Widget (expanded) |
|---|---|---|
| *coming soon* | *coming soon* | *coming soon* |

## Requirements

- **A rooted device** — Magisk, KernelSU, or SuperSU. The app runs `su`; it will not work without root.
- **Android 7.0 (API 24)** or newer.
- The phone and your PC must be on the same network (or otherwise routable).

## Install

### From a release (recommended)

1. Grab the latest `app-debug.apk` from the releases page.
2. Enable **Install unknown apps** for your browser/file manager if prompted.
3. Install, open, grant root when asked.

### Build from source

```
git clone https://github.com/geethut-stack/Wireless-ADB-Root.git
cd Wireless-ADB-Root
```

Open the folder in **Android Studio**, let Gradle sync (it downloads Gradle 8.9 and any missing SDK bits automatically), then hit **Run ▶**.

Or from the terminal, after one Gradle sync in Android Studio:

```
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Open the app and tap **Request root permission** (or just flip the switch — you'll be prompted either way).
2. Toggle **Wireless ADB** on.
3. Note the address shown, e.g. `192.168.1.42:5555`.
4. On your PC:

```
adb connect 192.168.1.42:5555
```
5. To change ports, edit the **ADB TCP port** field and hit **Save**. If ADB is already active it restarts on the new port immediately.

### Widget

Long-press your home screen → **Widgets** → **Wireless ADB** → drag it out.

- **Compact (1×1)** — the whole widget is one big toggle. Tap anywhere to flip state.
- **Enlarged** — shows the connection address and a dedicated Enable/Disable button. Tap the address line to open the app.

## How it works

Enabling runs these three commands inside a single `su` shell (so you only see one root prompt):

```
settings put global adb_enabled 1
setprop service.adb.tcp.port <port>
setprop ctl.restart adbd
```

Disabling sets the port to `-1` and restarts `adbd`:

```
setprop service.adb.tcp.port -1
setprop ctl.restart adbd
```

Current state is read back from `getprop service.adb.tcp.port`, and the IP comes from the first non-loopback IPv4 address on a `wlan*` / `ap*` / `eth*` interface. No polling service, no wakelocks.

## Project layout

```
app/src/main/java/com/example/wirelessadb/
├── MainActivity.kt        # single screen: status, toggle, port, root
├── AdbController.kt       # enable/disable TCP ADB, IP + port lookup
├── AdbWidgetProvider.kt   # size-aware widget (1×1 → full layout)
├── RootManager.kt         # su wrapper, single-shell command batches
└── Prefs.kt               # SharedPreferences (port, last state)
```

## Troubleshooting

| Problem ↕▾ | Fix ↕▾ |
|---|---|
| −**"Root command failed"** | Your root manager denied the request. Open it (Magisk → Superuser) and grant the app root, or tap **Request root permission** again. |
| **`adb connect` times out** | Check both devices are on the same subnet; confirm the port in the app matches the one you're connecting to; check the PC firewall. |
| **ADB state resets after reboot** | Expected — TCP ADB is a runtime property. Toggle it from the widget after boot. |
| **Doesn't stick on my ROM** | Some ROMs (MIUI, One UI) prefer the Android 11+ property. Edit `AdbController.kt` and swap in `settings put global adb_wifi_enabled 1`. |
| **Widget missing from the picker** | Some launchers cache the widget list. Reboot the launcher (or the device). |
⚙

### Note on Android 11+

Android 11's **Wireless debugging** in Developer Options uses a different mechanism (`adb_wifi_enabled` + mDNS pairing). This app uses the classic `service.adb.tcp.port` path, which still works on rooted devices across all current versions. If it doesn't on yours, see the troubleshooting row above.

## Disclaimer

This app requires root and modifies system ADB properties. That's the entire point, but it means:

- ADB over TCP on an untrusted network is a **security risk** — disable it when you're done, especially on public Wi-Fi.
- Use at your own risk. No warranty of any kind.

## Contributing

Issues and PRs are welcome. Keep it small — this app's whole value is that it does one thing.

## License

[MIT](https://license/) — do whatever you want.

