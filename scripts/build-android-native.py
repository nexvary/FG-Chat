#!/usr/bin/env python3
"""Reproducible minimal Tox build; 16-KB ELF alignment, no AV dependencies.
Requires Linux, JDK/Android SDK with NDK 27.2.12479018, CMake 3.22.1, make, pkg-config.
Source archives remain beside outputs for corresponding-source distribution.
"""
import hashlib, os, pathlib, subprocess, sys, tarfile, urllib.request, shutil
ROOT=pathlib.Path(__file__).resolve().parents[1]
DEPS=ROOT/'.native-deps'
SDK=pathlib.Path(os.environ['ANDROID_HOME'])
NDK=SDK/'ndk/27.2.12479018'
CMAKE=str(SDK/'cmake/3.22.1/bin/cmake')
TOOLCHAIN=NDK/'toolchains/llvm/prebuilt/linux-x86_64'
DEPS.mkdir(exist_ok=True)
def source(name,url,digest,dest):
    archive=DEPS/name
    if not archive.exists():
        with urllib.request.urlopen(url,timeout=120) as response: archive.write_bytes(response.read())
    if hashlib.sha256(archive.read_bytes()).hexdigest()!=digest: raise SystemExit(f'Checksum mismatch: {name}')
    if not dest.exists():
        dest.mkdir()
        with tarfile.open(archive) as tf: tf.extractall(dest,filter='data')
    return dest
sodium=source('libsodium-1.0.22.tar.gz','https://github.com/jedisct1/libsodium/releases/download/1.0.22-RELEASE/libsodium-1.0.22.tar.gz','adbdd8f16149e81ac6078a03aca6fc03b592b89ef7b5ed83841c086191be3349',DEPS/'sodium-source-1.0.22')/'libsodium-1.0.22'
tox=source('c-toxcore-v0.2.23.tar.gz','https://github.com/TokTok/c-toxcore/releases/download/v0.2.23/c-toxcore-v0.2.23.tar.gz','15cdd006ed7793dfc657e340ef9f218f6637d2fe5b130704d39b961389bb6cd6',DEPS/'tox-source')
for abi in sys.argv[1:] or ['arm64-v8a','x86_64']:
    target={'arm64-v8a':'aarch64-linux-android','x86_64':'x86_64-linux-android'}[abi]
    prefix=DEPS/f'sodium-1.0.22-{abi}'; build=DEPS/f'sodium-build-1.0.22-{abi}'; build.mkdir(exist_ok=True)
    env=os.environ.copy()
    env.update(CC=str(TOOLCHAIN/f'bin/{target}26-clang'),AR=str(TOOLCHAIN/'bin/llvm-ar'),RANLIB=str(TOOLCHAIN/'bin/llvm-ranlib'),STRIP=str(TOOLCHAIN/'bin/llvm-strip'),CFLAGS='-O2 -fPIC',LDFLAGS='-Wl,-z,max-page-size=16384')
    if not (prefix/'lib/libsodium.a').exists():
        subprocess.run([str(sodium/'configure'),f'--prefix={prefix}',f'--host={target}','--disable-shared','--enable-static','--disable-pie'],cwd=build,env=env,check=True)
        subprocess.run(['make','-j4','install'],cwd=build,env=env,check=True)
    out=DEPS/f'toxcore-android-{abi}'
    env['PKG_CONFIG_LIBDIR']=str(prefix/'lib/pkgconfig')
    subprocess.run([CMAKE,'-S',str(tox),'-B',str(DEPS/f'tox-build-0.2.23-sodium1.0.22-{abi}'),f'-DANDROID_ABI={abi}',f'-DCMAKE_TOOLCHAIN_FILE={NDK}/build/cmake/android.toolchain.cmake','-DANDROID_PLATFORM=android-26','-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON','-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384',f'-DCMAKE_INSTALL_PREFIX={out}',f'-DCMAKE_PREFIX_PATH={prefix}','-DENABLE_STATIC=OFF','-DENABLE_SHARED=ON','-DBUILD_TOXAV=OFF','-DDHT_BOOTSTRAP=OFF','-DBOOTSTRAP_DAEMON=OFF','-DUNITTEST=OFF','-DAUTOTEST=OFF','-DSTRICT_ABI=ON','-DMIN_LOGGER_LEVEL=ERROR','-DCMAKE_BUILD_TYPE=Release'],env=env,check=True)
    subprocess.run([CMAKE,'--build',str(DEPS/f'tox-build-0.2.23-sodium1.0.22-{abi}'),'-j4'],env=env,check=True)
    subprocess.run([CMAKE,'--install',str(DEPS/f'tox-build-0.2.23-sodium1.0.22-{abi}')],env=env,check=True)
    print(f'Built minimal c-toxcore 0.2.23 for {abi}',flush=True)
