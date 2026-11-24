#!/bin/bash
# Build :media-webp's prebuilt native libraries from the vendored libwebp.
#
#   libwebp_jni.so, the encoder and its JNI binding, one per Android ABI -> src/androidMain/jniLibs/<abi>/
#   libwebp.a, the encoder for cinterop, one per Kotlin/Native target   -> src/nativeInterop/cinterop/libwebp/libs/<target>/
#
#   native/build-natives.sh                   every target
#   native/build-natives.sh android ios       only those groups; desktop is macosArm64, linuxX64, linuxArm64, mingwX64
#
# The JVM and the web encode through Skia, which carries a libwebp of its own, so nothing is built for them. The
# libraries are committed so consumers need no NDK or CMake, and must be rebuilt and committed together with any
# change to the vendored source. Needs macOS with Xcode, the Android NDK for Android, and Kotlin/Native's toolchains in
# ~/.konan for the desktop targets, which Gradle downloads the first time it builds one of them.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE="$(dirname "$DIR")"
BUILD="$MODULE/build/native"

WANTED=("${@:-android ios desktop}")
WANTED=(${WANTED[*]})
ANDROID_ABIS=(arm64-v8a armeabi-v7a x86_64 x86)
IOS_TARGETS=(iosArm64 iosSimulatorArm64)
DESKTOP_TARGETS=(macosArm64 linuxX64 linuxArm64 mingwX64)
wants() { [[ " ${WANTED[*]} " == *" $1 "* ]]; }
# The module's minSdk; libwebp needs nothing newer.
ANDROID_PLATFORM=android-24

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
NDK_DIR="${ANDROID_NDK_HOME:-$(ls -d "$SDK_DIR"/ndk/* 2>/dev/null | sort -V | tail -1)}"
if wants android; then
    [ -f "$NDK_DIR/build/cmake/android.toolchain.cmake" ] || { echo "ERROR: Android NDK not found; set ANDROID_NDK_HOME" >&2; exit 1; }
fi
CMAKE_BIN="${CMAKE:-$(ls -d "$SDK_DIR"/cmake/*/bin/cmake 2>/dev/null | sort -V | tail -1)}"
[ -x "$CMAKE_BIN" ] || CMAKE_BIN="$(command -v cmake || true)"
[ -x "$CMAKE_BIN" ] || { echo "ERROR: cmake not found (on PATH or in the Android SDK)" >&2; exit 1; }
# A clean build every time: a CMake cache remembers the path it was made at and refuses to build from another.
rm -rf "$BUILD"

STRIP="$(ls -d "$NDK_DIR"/toolchains/llvm/prebuilt/*/bin/llvm-strip 2>/dev/null | head -1 || true)"

# Linux and Windows are built with Kotlin/Native's own clang against the sysroots it links with, so the library needs
# no newer glibc or MinGW runtime than the Kotlin code beside it. A library built with a distribution's compiler can.
KONAN_DEPS="${KONAN_DATA_DIR:-$HOME/.konan}/dependencies"

echo "=== media-webp native build: ${WANTED[*]} ==="
sed -n 's/^upstream_tag *= *//p' "$DIR/PROVENANCE" | sed 's/^/libwebp: /'
echo "NDK:     $NDK_DIR"
echo "CMake:   $CMAKE_BIN"

JNI_LIBS="$MODULE/src/androidMain/jniLibs"
if wants android; then
rm -rf "$JNI_LIBS"
for abi in "${ANDROID_ABIS[@]}"; do
    echo "--- android/$abi"
    "$CMAKE_BIN" -S "$MODULE" -B "$BUILD/android-$abi" \
        -DCMAKE_TOOLCHAIN_FILE="$NDK_DIR/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$abi" \
        -DANDROID_PLATFORM="$ANDROID_PLATFORM" \
        -DCMAKE_BUILD_TYPE=Release >/dev/null
    "$CMAKE_BIN" --build "$BUILD/android-$abi" --parallel >/dev/null
    install -d "$JNI_LIBS/$abi"
    "$STRIP" --strip-unneeded -o "$JNI_LIBS/$abi/libwebp_jni.so" "$BUILD/android-$abi/libwebp_jni.so"
done

fi

# The CMake arguments that cross-compile for one Kotlin/Native target.
cmake_args() {
    local target=$1 triple sysroot llvm
    case "$target" in
        iosArm64)          echo "-DCMAKE_SYSTEM_NAME=iOS -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_SYSROOT=iphoneos -DCMAKE_OSX_DEPLOYMENT_TARGET=14.0"; return ;;
        iosSimulatorArm64) echo "-DCMAKE_SYSTEM_NAME=iOS -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_SYSROOT=iphonesimulator -DCMAKE_OSX_DEPLOYMENT_TARGET=14.0"; return ;;
        # Kotlin/Native's own minimum macOS.
        macosArm64)        echo "-DCMAKE_SYSTEM_NAME=Darwin -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_SYSROOT=macosx -DCMAKE_OSX_DEPLOYMENT_TARGET=12.0"; return ;;
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

CINTEROP="$MODULE/src/nativeInterop/cinterop/libwebp"
NATIVE_TARGETS=()
wants ios && NATIVE_TARGETS+=("${IOS_TARGETS[@]}")
wants desktop && NATIVE_TARGETS+=("${DESKTOP_TARGETS[@]}")
for target in ${NATIVE_TARGETS[@]+"${NATIVE_TARGETS[@]}"}; do
    echo "--- $target"
    # shellcheck disable=SC2046
    "$CMAKE_BIN" -S "$MODULE" -B "$BUILD/$target" $(cmake_args "$target") -DCMAKE_BUILD_TYPE=Release >/dev/null
    "$CMAKE_BIN" --build "$BUILD/$target" --parallel >/dev/null
    install -d "$CINTEROP/libs/$target"
    install -m 644 "$BUILD/$target/libwebp.a" "$CINTEROP/libs/$target/libwebp.a"
done

# cinterop parses these headers but links the libraries above; both must come from the same release, so the
# headers are copied from the vendored source rather than kept by hand.
install -d "$CINTEROP/headers/webp"
install -m 644 "$DIR/libwebp/src/webp/encode.h" "$DIR/libwebp/src/webp/types.h" "$CINTEROP/headers/webp/"

echo
echo "Done. Libraries installed:"
find "$JNI_LIBS" "$CINTEROP/libs" \( -name '*.so' -o -name '*.a' \) -exec ls -l {} \; | awk '{print "  " $5 "\t" $NF}' | sed "s|$MODULE/||"
