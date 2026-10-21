#!/bin/bash
# Build :webp's prebuilt native libraries from the vendored libwebp.
#
#   libwebp_jni.so, the encoder and its JNI binding, one per Android ABI -> src/androidMain/jniLibs/<abi>/
#   libwebp.a, the encoder for cinterop, one per Apple target           -> src/iosMain/cinterop/libwebp/libs/<target>/
#
# The JVM and the web encode through Skia, which carries a libwebp of its own, so nothing is built for them. The
# libraries are committed so consumers need no NDK or CMake, and must be rebuilt and committed together with any
# change to the vendored source. Needs macOS with Xcode, and the Android NDK.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE="$(dirname "$DIR")"
BUILD="$MODULE/build/native"

ANDROID_ABIS=(arm64-v8a armeabi-v7a x86_64 x86)
APPLE_TARGETS=(iosArm64 iosSimulatorArm64)
# The module's minSdk; libwebp needs nothing newer.
ANDROID_PLATFORM=android-24

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
NDK_DIR="${ANDROID_NDK_HOME:-$(ls -d "$SDK_DIR"/ndk/* 2>/dev/null | sort -V | tail -1)}"
[ -f "$NDK_DIR/build/cmake/android.toolchain.cmake" ] || { echo "ERROR: Android NDK not found; set ANDROID_NDK_HOME" >&2; exit 1; }
CMAKE_BIN="${CMAKE:-$(ls -d "$SDK_DIR"/cmake/*/bin/cmake 2>/dev/null | sort -V | tail -1)}"
[ -x "$CMAKE_BIN" ] || CMAKE_BIN="$(command -v cmake || true)"
[ -x "$CMAKE_BIN" ] || { echo "ERROR: cmake not found (on PATH or in the Android SDK)" >&2; exit 1; }
STRIP="$(ls -d "$NDK_DIR"/toolchains/llvm/prebuilt/*/bin/llvm-strip | head -1)"

echo "=== webp native build ==="
sed -n 's/^upstream_tag *= *//p' "$DIR/PROVENANCE" | sed 's/^/libwebp: /'
echo "NDK:     $NDK_DIR"
echo "CMake:   $CMAKE_BIN"

JNI_LIBS="$MODULE/src/androidMain/jniLibs"
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

CINTEROP="$MODULE/src/iosMain/cinterop/libwebp"
for target in "${APPLE_TARGETS[@]}"; do
    case "$target" in
        iosArm64)           sysroot=iphoneos ;;
        iosSimulatorArm64)  sysroot=iphonesimulator ;;
        *) echo "unknown target $target" >&2; exit 1 ;;
    esac
    echo "--- apple/$target"
    "$CMAKE_BIN" -S "$MODULE" -B "$BUILD/$target" \
        -DCMAKE_SYSTEM_NAME=iOS \
        -DCMAKE_OSX_ARCHITECTURES=arm64 \
        -DCMAKE_OSX_SYSROOT="$sysroot" \
        -DCMAKE_OSX_DEPLOYMENT_TARGET=14.0 \
        -DCMAKE_BUILD_TYPE=Release >/dev/null
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
