#!/bin/bash
# Build the prebuilt native static libraries for :h3spatial from the vendored H3 source.
#
#   libh3.a (H3 only, for cinterop), one per Kotlin/Native target -> src/nativeInterop/cinterop/h3/libs/<target>/
#
#   native/build-natives.sh            every target
#   native/build-natives.sh desktop    only macosArm64, linuxX64, linuxArm64 and mingwX64
#   native/build-natives.sh ios        only iosArm64 and iosSimulatorArm64
#
# Kotlin/Native is the only place H3 is compiled here. Android uses Uber's own com.uber:h3-android, the JVM uses
# com.uber:h3, and the web uses h3-js. The libraries are committed so consumers need no CMake, and must be rebuilt and
# committed together with any change to the vendored source. Needs macOS with Xcode, and Kotlin/Native's toolchains
# in ~/.konan, which Gradle downloads the first time it builds one of these targets.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE="$(dirname "$DIR")"
BUILD="$MODULE/build/native"
CINTEROP="$MODULE/src/nativeInterop/cinterop/h3"

WANTED=("${@:-ios desktop}")
WANTED=(${WANTED[*]})
IOS_TARGETS=(iosArm64 iosSimulatorArm64)
DESKTOP_TARGETS=(macosArm64 linuxX64 linuxArm64 mingwX64)

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
CMAKE_BIN="${CMAKE:-$(ls -d "$SDK_DIR"/cmake/*/bin/cmake 2>/dev/null | sort -V | tail -1)}"
[ -x "$CMAKE_BIN" ] || CMAKE_BIN="$(command -v cmake || true)"
[ -x "$CMAKE_BIN" ] || { echo "ERROR: cmake not found (on PATH or in the Android SDK)" >&2; exit 1; }

# The oldest iOS and macOS the objects may run on. Left unset, CMake takes the SDK's own version, and an app supporting
# older releases links code built for a newer one. macOS is Kotlin/Native's own minimum.
IOS_DEPLOYMENT_TARGET=14.0
MACOS_DEPLOYMENT_TARGET=12.0

# Linux and Windows are built with Kotlin/Native's own clang against the sysroots it links with, so the library needs
# no newer glibc or MinGW runtime than the Kotlin code beside it. A library built with a distribution's compiler can.
KONAN_DEPS="${KONAN_DATA_DIR:-$HOME/.konan}/dependencies"

# A clean build every time: a CMake cache remembers the path it was made at and refuses to build from another.
rm -rf "$BUILD"

echo "=== h3spatial native build: ${WANTED[*]} ==="
sed -n 's/^upstream_tag *= *//p' "$DIR/PROVENANCE" | sed 's/^/H3 tag: /'
echo "CMake:  $CMAKE_BIN"

# The CMake arguments that cross-compile for one Kotlin/Native target.
cmake_args() {
    local target=$1 triple sysroot llvm
    case "$target" in
        iosArm64)          echo "-DCMAKE_SYSTEM_NAME=iOS -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_SYSROOT=iphoneos -DCMAKE_OSX_DEPLOYMENT_TARGET=$IOS_DEPLOYMENT_TARGET"; return ;;
        iosSimulatorArm64) echo "-DCMAKE_SYSTEM_NAME=iOS -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_SYSROOT=iphonesimulator -DCMAKE_OSX_DEPLOYMENT_TARGET=$IOS_DEPLOYMENT_TARGET"; return ;;
        macosArm64)        echo "-DCMAKE_SYSTEM_NAME=Darwin -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_SYSROOT=macosx -DCMAKE_OSX_DEPLOYMENT_TARGET=$MACOS_DEPLOYMENT_TARGET"; return ;;
        linuxX64)   triple=x86_64-unknown-linux-gnu;  sysroot=$(ls -d "$KONAN_DEPS"/x86_64-unknown-linux-gnu-gcc-*/x86_64-unknown-linux-gnu/sysroot 2>/dev/null | sort -V | tail -1) ;;
        linuxArm64) triple=aarch64-unknown-linux-gnu; sysroot=$(ls -d "$KONAN_DEPS"/aarch64-unknown-linux-gnu-gcc-*/aarch64-unknown-linux-gnu/sysroot 2>/dev/null | sort -V | tail -1) ;;
        mingwX64)   triple=x86_64-w64-mingw32;        sysroot=$(ls -d "$KONAN_DEPS"/msys2-mingw-w64-x86_64-* 2>/dev/null | sort -V | tail -1) ;;
        *) echo "unknown target $target" >&2; exit 1 ;;
    esac
    llvm=$(ls -d "$KONAN_DEPS"/llvm-*-essentials-*/bin 2>/dev/null | sort -V | tail -1)
    [ -n "$sysroot" ] && [ -n "$llvm" ] || { echo "ERROR: Kotlin/Native's toolchain for $target is not in $KONAN_DEPS; build the target with Gradle once" >&2; exit 1; }
    # Only a static library is made, so CMake's compiler check must not try to link an executable. Kotlin/Native ships
    # no ranlib, and needs none: llvm-ar writes the symbol index itself.
    echo "-DCMAKE_SYSTEM_NAME=$([ "$target" = mingwX64 ] && echo Windows || echo Linux)" \
         "-DCMAKE_C_COMPILER=$llvm/clang -DCMAKE_C_COMPILER_TARGET=$triple -DCMAKE_SYSROOT=$sysroot" \
         "-DCMAKE_AR=$llvm/llvm-ar -DCMAKE_RANLIB=/usr/bin/true -DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY"
}

TARGETS=()
for group in "${WANTED[@]}"; do
    case "$group" in
        ios)     TARGETS+=("${IOS_TARGETS[@]}") ;;
        desktop) TARGETS+=("${DESKTOP_TARGETS[@]}") ;;
        *) echo "unknown group $group: ios or desktop" >&2; exit 1 ;;
    esac
done

for target in "${TARGETS[@]}"; do
    echo "--- $target"
    # shellcheck disable=SC2046
    "$CMAKE_BIN" -S "$MODULE" -B "$BUILD/$target" $(cmake_args "$target") -DCMAKE_BUILD_TYPE=Release >/dev/null
    "$CMAKE_BIN" --build "$BUILD/$target" --parallel >/dev/null
    install -d "$CINTEROP/libs/$target"
    install -m 644 "$BUILD/$target/libh3.a" "$CINTEROP/libs/$target/libh3.a"
done

# cinterop parses this header, but links the .a above. They must come from the same version -- a
# stale header against a newer library compiles cleanly and fails at runtime. Copy the generated
# header rather than committing a hand-kept one.
install -d "$CINTEROP/headers"
install -m 644 "$BUILD/${TARGETS[0]}/h3lib/include/h3api.h" "$CINTEROP/headers/h3api.h"

echo
echo "Done. Binaries installed:"
find "$CINTEROP/libs" -name '*.a' | sed "s|$MODULE/||;s|^|  |"
