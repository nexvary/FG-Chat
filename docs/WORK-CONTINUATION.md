# Work continuation — 2026-10-29

Base commit: 725c65fb6c34ab6bf6266163d955cfc2f3a0310f (main at initial inspection).
Working branch: fgchat-android-tox-alpha. Verified source checkpoint 5ea5fb34ddbaa9ca35eada33dfd298835b26a241; screenshot collection fix pending. PR #1: https://github.com/nexvary/FG-Chat/pull/1
Find final exact revision with `git rev-parse HEAD`; this file cannot contain the hash of the commit that contains itself.

## Repository audit

Inspected all original files (invite-core contracts/tests, SQL, ADRs, workflow). GitHub API: main was the sole branch, no Issues/PRs; all six original runs failed. Latest run 39810317856/job 119934482360 failed because JUnit Platform engine was incorrectly versioned 5.11.3. Fixed with launcher 1.11.3. SQL archived; README reconciled with ADR-0003. No pre-existing Android or Windows app.

## Implemented files

Root Gradle build/wrapper, android-app application/Keystore vault/Compose UI/English+Arabic/QR import/export/instrumentation, tox-engine Kotlin JNI wrapper/session framing/bootstrap/native tests, native C++/CMake, source build/download scripts, CI/Dependabot/OSV runtime scan, GPL license, current README/ADR/threat model, evidence. Original 15-character invitation generator is unused legacy API. See `git show --stat` or `git status --short` for exact modified files.

## Verified checkpoint

- Host JNI native C++ built successfully, warnings as errors.
- invite-core tests passed (6 tests).
- tox-engine tests passed (6 tests), including two real Tox identities connecting via local UDP, Arabic/English bidirectional text and read receipts; offline attempt absent after connection, persistence/corruption checks, validation/closed lifecycle.
- Initial Android debug APK assembled successfully (before source-built 16-KB native replacement).
- Source-built Android c-toxcore 0.2.23 with libsodium 1.0.22 built for arm64/x86_64; ELF 16-KB alignment verified, APK zipalign -P16 and v2 signature verification passed.
- Updated APK + instrumentation APK build and Android lint passed (no lint issues). OSV scan of 85 resolved Android runtime Maven artifacts returned 0 vulnerability records (not a native/build/test audit).
- Local Android35 software emulator boot timed out after 600s (no KVM here); no UI test/screenshot claim from local run. GitHub KVM instrumentation passed all 3 tests: UI/Arabic/navigation/recreation, native Android loopback messaging, Keystore tamper rejection. Run https://github.com/nexvary/FG-Chat/actions/runs/40764718624. Screenshot export was empty despite passing tests; collection fix and explicit four-image gate added.
- Optional public network probe failed: peerA=false, peerB=false, friendsReady=false after 60s. Public-node reachability is unproven from this environment. Run `./gradlew :tox-engine:publicNetworkProbe` on an unrestricted network.
- No physical-device/cross-network/battery/Windows/security-audit claims.

Evidence is in evidence/2026-10-28. Update this checkpoint with final additional results before finishing.

## Reproduce / build

See README build commands. Linux requires pkg-config, make, C/C++ and JDK 17; NDK 27.2.12479018, CMake 3.22.1, compile/target SDK35. Native scripts verify archive SHA-256 before extraction. A normal environment needs no proxy configuration. Work sandbox needed SDK/JDK downloads and local extracted OS packages because package installation was restricted. Do not commit local paths, proxy settings or development keys.

Test: `./gradlew :invite-core:test :tox-engine:test :android-app:lintDebug :android-app:assembleDebug` after native source build and host CMake build.
UI: clean disposable API35 x86_64 emulator, `./gradlew :android-app:connectedDebugAndroidTest`.

## Next priorities

1. Validate the screenshot collection fix, inspect all four actual screenshots, merge verified alpha and publish the test APK with native corresponding sources. Physical phone test still required.
2. Two Android phones (Wi-Fi/mobile/CGNAT, IPv4/IPv6, public relay fallback), record timing/offline/race/battery/lifecycle tests; compare native upstream handshake hardening before wider testing.
3. Add protocol-level app accepted-message acknowledgments and receipt generation scoping, stronger invitation validation UX; data persistence rollback/fault-injection tests.
4. Independent app root/device authorization using reviewed standard formats/library; QR authenticated transfer with confirmation and atomic activation/rollback, encrypted export; do not duplicate live Tox keys.
5. Persistent encrypted history and consented chunked streaming file transfer with integrity, quotas/resume; Tox has APIs but none are wired.
6. Signed scoped mutual deletion with replay journal and verified signed receipt; no remote deletion claims offline.
7. Media/AV and Android foreground operation/notifications, battery/permission tests, avatars, safe temporary invite semantics, accessibility.
8. Windows CMake JNI packaging with same Kotlin engine after Android stability, then independent security review and protected production signing.

Phases 1–60 are not declared complete. Threat model includes unimplemented features as open gates, not security guarantees.
