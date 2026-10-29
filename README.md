# FG Chat

Android-first privacy messenger without phone numbers, email accounts or infrastructure operated by NEXVARY. **Early alpha, not a completed or independently audited product.** GPL-3.0-or-later.

## Implemented

- Native c-toxcore identity and encrypted peer messaging, public DHT bootstrap/TCP relays.
- Android Kotlin/Compose application, Android 8+ (API 26), target Android 15 (35), arm64 + x86_64.
- Keystore AES-GCM encrypted identity/contact/block state, atomic writes, backups disabled, fail-closed corrupt identity handling.
- Full identity invitation by QR image, link and copy/share; QR image import with bounded downsampling.
- Explicit friend approval, reject/block, local profile name editing.
- Online-only send attempts, no app outbox/server mailbox/automatic retries; session tokens reject messages from earlier observed connections.
- Separate submitted/transport-received states; memory-only bounded conversation history and confirmed local clear.
- English + Arabic strings, RTL navigation; release screenshots blocked, debug screenshots allowed for test evidence.
- Shared JVM Tox engine and real native two-peer loopback integration tests.

Native transport is experimental: peers can learn IP addresses, public nodes observe network metadata, and already in-flight traffic can arrive during an undetected short interruption. No human-read receipt is claimed. See [threat model](docs/THREAT-MODEL.md) and [engine decision](docs/ADR-0004-tox-alpha.md).

## Build

Linux prerequisites: JDK 17, Python 3.12+, make, C/C++ compiler, pkg-config, CMake, Android SDK; set `ANDROID_HOME` and `JAVA_HOME`.

```sh
sdkmanager 'platforms;android-35' 'build-tools;34.0.0' 'build-tools;35.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1'
python3 scripts/fetch-native.py linux-x86_64
python3 scripts/build-android-native.py
cmake -S native -B native/build
cmake --build native/build
./gradlew :invite-core:test :tox-engine:test :android-app:lintDebug :android-app:assembleDebug
```

Android Tox/libsodium are built from hash-pinned source, without AV dependencies and with 16-KB ELF page alignment. `fetch-native.py` Android prebuilt targets are retained only for upstream comparison; do not use them for release (4-KB alignment). Source archives and native builds stay in ignored `.native-deps/`. Host integration tests use the hash-pinned upstream Linux binary. Gradle wrapper distribution checksum is pinned.

Debug APK: `android-app/build/outputs/apk/debug/android-app-debug.apk`. This is signed with a development key, not a production signing identity. No production key is generated/committed.

```sh
adb install -r android-app/build/outputs/apk/debug/android-app-debug.apk
```

Open both apps; create a local identity on each; exchange a complete invitation through a trusted channel, accept the incoming request, compare full public keys, wait for Available, then send. The apps must remain visible in this alpha. Leaving the app disconnects and discards in-flight transport state. Chat history disappears on process death. Do not uninstall/clear data: account transfer/recovery is not available yet.

## Tests and evidence

CI builds APK, runs Kotlin/native integration tests, Android lint, Android 35 UI tests, OSV scan of resolved Android runtime Maven dependencies, weekly Dependabot. Instrumentation must use a disposable emulator with clean application data. Tests cover real peer transport on loopback, not two physical devices or production CGNAT/mobile networks. Evidence is retained in Actions artifacts and `evidence/`; only recorded successful results count.

## Not implemented

Voice/video calls, voice notes, media, live file transfer/resume, signed mutual deletion, secure account transfer/export, independent device certificates/revocation, linked devices, Windows app, short/one-time/rotating invitation codes, encrypted persistent history, avatar profile sync, background service/notifications and Tor. The standalone invitation/file/deletion contracts in `invite-core` are legacy prototypes, not working application capabilities. SQL is archived under `archive/server-prototype`.

Windows will reuse `tox-engine` plus a Windows JNI build after Android physical-device and account/device authorization gates are met. No copied Tox private key may be used for concurrent devices. See [continuation](docs/WORK-CONTINUATION.md) for next work and current validation.
