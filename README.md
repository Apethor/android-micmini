# MicMini — Android mic → raw PCM over TCP (low latency)

[![CI](https://github.com/Apethor/android-micmini/actions/workflows/build.yml/badge.svg)](https://github.com/Apethor/android-micmini/actions/workflows/build.yml)
[![license: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![min SDK 23](https://img.shields.io/badge/minSdk-23%20(Android%206.0)-blue.svg)](#)

Turn **any Android phone** into a **low-latency microphone** exposed as a raw **PCM stream over TCP**.
The phone is the **server**; any client on the LAN connects and reads the bytes. No root.

**Wire format: raw PCM · s16le · mono · 16 kHz · TCP port 6000** (no header).

```
[ Android phone ]                                  [ any client on the LAN ]
  mic → AudioRecord → TCP server :6000   ───────►   connect() → read() → your code
  (foreground service, auto-start on boot)           (ffmpeg / Python / C / …)
```

Why not an existing app? Apps like WO Mic / IP Webcam are great but: their Linux receivers are x86-only
or HTTP-based (higher latency), and features like *run-on-boot* are paid. MicMini is a ~150-line,
no-root, open-source alternative that streams **raw PCM over a plain socket** any language can read —
so it works on tiny ARM64 boards (Raspberry Pi, Orange Pi, etc.) with nothing but a TCP socket.

> Latency ~50 ms over Wi-Fi in testing. Tested on a Nubia M2 (NX551J, Android 6.0.1) feeding an
> Orange Pi 5B, but nothing is phone- or board-specific.

## Quick start
```bash
# 1) get the APK from Releases (or from the CI "micmini-apk" artifact) and install it:
adb install -r micmini-v1.1.apk
adb shell pm grant com.apethor.micmini android.permission.RECORD_AUDIO   # grant mic, no UI
adb shell monkey -p com.apethor.micmini -c android.intent.category.LAUNCHER 1   # start the server

# 2) consume it from any client (replace <PHONE_IP> with the phone's LAN IP):
ffplay -f s16le -ar 16000 -ch_layout mono -i tcp://<PHONE_IP>:6000
```
Find `<PHONE_IP>` in the phone's *Settings → About → Status*, or `adb shell ip addr show wlan0`.

## Download / build
- **Prebuilt APK:** see **[Releases](https://github.com/Apethor/android-micmini/releases)**.
- **From source (no local Android SDK):** every push runs the `CI` workflow (GitHub Actions) → grab
  the `micmini-apk` artifact, or run it via *Actions → CI → Run workflow*.
- **Locally:** `gradle assembleDebug` (needs JDK 17 + Android SDK).

## Consume it
Any TCP client works — the payload is just raw PCM (s16le, mono, 16 kHz):
```bash
ffplay  -f s16le -ar 16000 -ch_layout mono -i tcp://<PHONE_IP>:6000   # listen  (ffplay: -ch_layout)
ffmpeg  -f s16le -ar 16000 -ac 1           -i tcp://<PHONE_IP>:6000 out.wav   # record (ffmpeg: -ac)
```
From your own program, see [`receiver_example.py`](receiver_example.py) (pure Python, ~20 lines). In
C or any language it is the same: open a TCP socket to `<PHONE_IP>:6000` and read PCM 16-bit LE mono 16 kHz.

## On the phone
- **Foreground service** — keeps running with the screen off.
- **Auto-start on boot** via a `BootReceiver`. Some vendor ROMs (e.g. Nubia/MIUI) block auto-start by
  default — whitelist the app in *Settings → Apps → ⋮ → Autostart management*.
- **One client at a time** — a second connection waits in the accept backlog until the first drops.

## Configuration
Constants live in [`MicService.java`](app/src/main/java/com/apethor/micmini/MicService.java):
| Constant | Default | Notes |
|---|---|---|
| `SAMPLE_RATE` | `16000` | Fixed wire contract; change it here **and in the client** (raw stream has no header). |
| `PORT` | `6000` | TCP port. |
| `BIND_LOOPBACK` | `false` | `true` = listen on `127.0.0.1` only (use with `adb forward tcp:6000 tcp:6000`). |

## Troubleshooting
- **Connection refused / times out:** the service isn't running (re-launch via `monkey`), wrong IP,
  or the phone is asleep/Doze — keep it charging or exclude it from battery optimization.
- **Bytes arrive but audio is garbled:** the client's format doesn't match — it must be
  **s16le, mono, 16 kHz**.
- **Nothing after a reboot:** the ROM blocked auto-start — see *Autostart management* above.
- **Confirm the port is listening:** `adb shell "cat /proc/net/tcp6" | grep 1770` (0x1770 = 6000).
- **On a busy Wi-Fi:** move the phone/board to a quieter channel, or use `adb forward` over USB.

## ⚠️ Security / privacy
The stream is **unauthenticated and unencrypted** and, by default, listens on **all interfaces**
(`0.0.0.0:6000`). While running, **anyone on the same network can connect and listen to the mic**.
Use only on a **trusted LAN**. To reduce exposure: set `BIND_LOOPBACK = true` and reach it via
`adb forward`, use an isolated Wi-Fi / AP client isolation, or stop the service when idle.
*(Roadmap: optional access token.)*

## Contributing
Small, single-purpose utility — issues and PRs welcome. CI runs Android lint + build on every PR,
plus an automated Claude review. Releases are cut automatically from `v*` tags.

## License
[MIT](LICENSE) © 2026 Guilherme Nicolino ([Apethor](https://github.com/Apethor)).
