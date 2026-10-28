# Third-party software

FG Chat: GPL-3.0-or-later, see LICENSE. Commercial use must comply with all applicable source/notice requirements.

Android native components are built from:
- c-toxcore v0.2.23, GPL-3.0: https://github.com/TokTok/c-toxcore/tree/v0.2.23
  Exact source archive: https://github.com/TokTok/c-toxcore/releases/download/v0.2.23/c-toxcore-v0.2.23.tar.gz
  SHA-256: 15cdd006ed7793dfc657e340ef9f218f6637d2fe5b130704d39b961389bb6cd6
  Includes upstream third-party sources/notices (cmp and crypto components); retain archive as corresponding source.
- libsodium 1.0.22, ISC: https://github.com/jedisct1/libsodium/tree/1.0.22-RELEASE
  Exact source archive: https://github.com/jedisct1/libsodium/releases/download/1.0.22-RELEASE/libsodium-1.0.22.tar.gz
  SHA-256: adbdd8f16149e81ac6078a03aca6fc03b592b89ef7b5ed83841c086191be3349

Build scripts/configuration for these exact sources are scripts/build-android-native.py and native/CMakeLists.txt. No Opus/VPX/AV code is included in the source-built Android Tox library. Linux prebuilt is used only for host tests, not shipped in the Android APK. Android debug APK also includes AndroidX/Compose/Kotlin/coroutines and ZXing (Apache-2.0), plus NDK libc++ (Apache-2.0 with LLVM exception). Preserve these notices and upstream license texts when redistributing. Complete native source archives should accompany any public binary release.
