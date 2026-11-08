# Banter Android

Native Android client for [Banter](https://github.com/Banter-Foundation/Banter). Kotlin + Jetpack Compose, Discord-like dark UI.

## Alpha 5

Tag: `v1.0.0-alpha.5`

- **Watch other streams**: WebRTC SFU/P2P session + `SubscribeLayer` so remote Go Live / camera tiles appear on the call stage (cycle when multiple people are live).
- Local camera preview (CameraX) and local screen-share preview while you’re Go Live.
- Settings page (account, server URL, front/mirror camera) from the user bar gear / avatar.
- System red status-bar share pill via MediaProjection FGS (Alpha 4).

Install the APK from [Releases](https://github.com/Banter-Foundation/android-banter/releases), or build locally:

```bash
export ANDROID_HOME=~/android-sdk   # API 34 SDK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Minimum Android 8.0 (API 26). Point the app at a running Banter server (`host:3970`).

## Features

- Login / register against any Banter server
- Server rail, channel sidebar, member list
- Text chat + DMs with live WebSocket updates (BFMD)
- Friends (online / all / pending)
- Phone-call VC / DM UI: mute, deafen, camera, Go Live
- Watch remote Go Live / camera over WebRTC
- Settings (host URL, camera prefs, log out)
