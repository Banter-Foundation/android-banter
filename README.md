# Banter Android

Native Android client for [Banter](https://github.com/Banter-Foundation/Banter). Kotlin + Jetpack Compose, Discord-like dark UI.

## Alpha 1

Tag: `v1.0.0-alpha.1`

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
- Text chat + DMs with live WebSocket updates
- Friends (online / all / pending)
- Voice channel join / leave / mute / deafen UI
