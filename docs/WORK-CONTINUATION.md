# Work continuation — verified release, 2026-10-29

Branch: main. Last tested/released application commit: 57925741978b698937077fb6ad2055e8fa65163c.
The follow-up commit containing this checkpoint changes documentation/evidence only; application sources are identical to that release. Use `git rev-parse HEAD` for the exact current documentation revision.

Release: https://github.com/nexvary/FG-Chat/releases/tag/v0.1.0-alpha.49
APK: https://github.com/nexvary/FG-Chat/releases/download/v0.1.0-alpha.49/FG-Chat-debug.apk
APK SHA-256: c28dd22d238ab35bfdb1b5e297d134ac1955849e22f0eacdac9a4eca1661019a
Build/tests: https://github.com/nexvary/FG-Chat/actions/runs/40802029507
Publication/public network experiment: https://github.com/nexvary/FG-Chat/actions/runs/40802481403

## Source audit and implementation

Initial main was 725c65fb6c34ab6bf6266163d955cfc2f3a0310f, with no Android app, one branch and no Issues/PRs. All six original CI runs failed; latest 39810317856 failed on incorrectly versioned JUnit Platform 5.11.3. Fixed with launcher 1.11.3. Archived operator-run SQL and reconciled README/ADRs with public-relay permission.

PR #1 merged as 07b623c02dcab025953b76c07962abbe1611e957. Added root Gradle/wrapper, android-app (Compose/Keystore/QR/RTL), tox-engine Kotlin JNI/session framing, native C++/CMake, source-build scripts, real integration tests, CI/dependency scan, GPL license and notices, ADR-0004, threat model and evidence. Later main fixes enforce a single identity-owning Activity and route repeated invitation events without replacing the canonical launch intent.

## Verified results

- 12 JVM tests passed: 6 invite-core policy/generator tests and 6 real Tox engine tests. Includes two actual native peers, Arabic/English text/transport receipts, no deferred delivery of an offline attempt, explicit friend approval, identity restoration/corruption, validation and closed lifecycle.
- All 3 Android 15/API35 instrumentation tests passed: UI identity creation, repeated warm invitation routing with actual resumed-Activity identity checks, recreation, Arabic RTL/navigation; Android-native bidirectional loopback messages; Keystore round-trip/tamper rejection.
- Four real emulator screenshots exported and visually inspected, including matching identity QR before/after navigation/language change. Attached to the release as chats-ar/en.png and identity-ar/en.png.
- Android debug APK built, lint passed with no issues, v2 APK signature verified, ZIP 16-KB alignment passed.
- Downloaded the published APK again: SHA-256 matches release digest; all six packaged native libraries (arm64/x86_64) have 16-KB ELF load alignment. See evidence/2026-10-29/release-verification.json.
- OSV scan of 85 resolved Android runtime Maven artifacts returned 0 vulnerability records. This does not scan all native/build/test dependencies or constitute an independent audit.
- Public Tox experiment PASSED on GitHub: peerA=true, peerB=true, friendsReady=true; encrypted message received=true, using public bootstrap/relays and no local bootstrap seed. Both peers run on one host; this does not validate two physical phones or cellular/CGNAT.
- Development-signed alpha only. No protected production signing key was generated or committed.

## Earlier failures and corrections

- Initial local public-network probe could not reach public nodes; GitHub's later experiment passed. Keep the failed local log as environment-specific evidence, not a universal network failure.
- Local software API35 emulator boot timed out after 600s without KVM. GitHub KVM tests passed; no local emulator success is claimed.
- Screenshot export initially produced an empty folder because compound shell commands were not interpreted as expected. Fixed using separate screencap commands and an explicit four-image gate.
- Run 40800862203: UI lifecycle test failed because ActivityScenario ignored lifecycle events after getIntent changed from MAIN to VIEW. Application reused the same Activity, but recreation/teardown tracking timed out. Final fix treats incoming invitations as transient events, retains the canonical launch Intent and keeps all lifecycle assertions. Run 40802029507 passed.
- Initial Dependabot update batch consumed CI runners; grouped/bounded future PR batches and closed the nine individual automated update PRs (#2–#10) without merging unverified upgrades. No branches were deleted. Closed-PR events now skip heavy jobs and cancel superseded runs. Grouped update PRs require their own compatibility review.

## Known open gates

No physical-device test: the connected Desktop Commander computer was offline. No two-phone Wi-Fi/mobile/CGNAT/IPv6/battery validation or Windows validation. Tox is experimental; direct peers can learn IP addresses; Tor is absent. Public infrastructure availability is not guaranteed.

No account transfer/export/recovery, independent app-root/device certificates/revocation, linked devices, persistent encrypted chat history, short/rotating/one-time application invitations, file transfer/resume, media/voice/video/groups, signed bilateral deletion, background service/notifications or avatar synchronization. Legacy invite-core contracts/generator are not production app features. History is bounded and memory-only. Debug signing keys can change between builds; uninstalling/clearing storage loses the identity.

No app outbox/mailbox/automatic retry. Already in-flight packets can arrive during an undetected brief interruption; observed disconnect rotates session tokens. Receipts mean transport reception, not human reading or guaranteed app-level acceptance.

## Build and reproduce

Linux prerequisites: JDK17, Python3.12+, make, C/C++ compiler, pkg-config, CMake and Android SDK with ANDROID_HOME/JAVA_HOME set.

```sh
sdkmanager 'platforms;android-35' 'build-tools;34.0.0' 'build-tools;35.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1'
python3 scripts/fetch-native.py linux-x86_64
python3 scripts/build-android-native.py
cmake -S native -B native/build
cmake --build native/build
./gradlew :invite-core:test :tox-engine:test :android-app:lintDebug :android-app:assembleDebug
./gradlew :android-app:exportRuntimeDependencies
python3 scripts/scan-dependencies.py
```

Disposable clean API35 emulator: `bash scripts/run-android-instrumentation.sh`.
Public-network experiment: `./gradlew :tox-engine:publicNetworkProbe`.
APK installation: `adb install -r android-app/build/outputs/apk/debug/android-app-debug.apk`.
Source archives are hash-pinned; Android builds c-toxcore 0.2.23 + libsodium 1.0.22 without AV. Do not replace Android native libraries with upstream 4-KB-aligned prebuilt ones. No proxy-specific settings/keys are committed.

## Next priorities

1. Two real Android phones: Wi-Fi/mobile/CGNAT, public relay fallback, network loss/manual retry, process/background behavior and battery; record evidence. Do not use emulator success as a substitute.
2. App-accepted message acknowledgments with session/generation scoping; persistence failure injection and UI send/contact tests; encrypted persistent history.
3. Reviewed independent app-root/device authorization, authenticated QR transfer, explicit confirmation, atomic activation/rollback and old-device revocation; no concurrently copied Tox identity keys.
4. Consent-based streaming/chunked files with bounded memory, storage quotas, safe names, digest verification, resume, pause/cancel/progress.
5. Scoped signed mutual deletion, replay journal and verified receipt; no claim of deleting screenshots/backups/uncooperative clients.
6. Calls/media, temporary invitation semantics, avatar sync, background foreground-service/notifications and accessibility/performance tests.
7. Windows 10/11 JNI packaging and desktop UI reusing tox-engine after Android/device trust stability; no alternate cryptographic protocol.
8. Independent security review, native dependency/advisory review and protected production signing before wider release.

Phases 1–60 are not declared complete. The release proves the messaging foundation under the exact tested conditions above.
