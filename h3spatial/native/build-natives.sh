#!/bin/bash
# Build the prebuilt iOS static libraries for :h3spatial from the vendored H3 source.
#
#   libh3.a (H3 only, for cinterop), one per Apple target -> src/iosMain/cinterop/h3/libs/<target>/
#
# iOS is the only target that compiles H3 here. Android uses Uber's own com.uber:h3-android, the JVM
# uses com.uber:h3, and the web uses h3-js. The libraries are committed so consumers need no CMake,
# and must be rebuilt and committed together with any change to the vendored source.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE="$(dirname "$DIR")"
BUILD="$MODULE/build/native"

APPLE_TARGETS=(iosArm64 iosSimulatorArm64)

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
CMAKE_BIN="${CMAKE:-$(ls -d "$SDK_DIR"/cmake/*/bin/cmake 2>/dev/null | sort -V | tail -1)}"
[ -x "$CMAKE_BIN" ] || CMAKE_BIN="$(command -v cmake || true)"
[ -x "$CMAKE_BIN" ] || { echo "ERROR: cmake not found (on PATH or in the Android SDK)" >&2; exit 1; }

# The oldest iOS the objects may run on. Left unset, CMake takes the SDK's own version, and an app supporting older
# releases links code built for a newer one.
IOS_DEPLOYMENT_TARGET=14.0

# A clean build every time: a CMake cache remembers the path it was made at and refuses to build from another.
rm -rf "$BUILD"

echo "=== h3spatial native build ==="
sed -n 's/^upstream_tag *= *//p' "$DIR/PROVENANCE" | sed 's/^/H3 tag: /'
echo "CMake:  $CMAKE_BIN"

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
        -DCMAKE_OSX_DEPLOYMENT_TARGET="$IOS_DEPLOYMENT_TARGET" \
        -DCMAKE_BUILD_TYPE=Release >/dev/null
    "$CMAKE_BIN" --build "$BUILD/$target" --parallel >/dev/null
    install -d "$MODULE/src/iosMain/cinterop/h3/libs/$target"
    install -m 644 "$BUILD/$target/libh3.a" "$MODULE/src/iosMain/cinterop/h3/libs/$target/libh3.a"
done

# cinterop parses this header, but links the .a above. They must come from the same version -- a
# stale header against a newer library compiles cleanly and fails at runtime. Copy the generated
# header rather than committing a hand-kept one.
install -d "$MODULE/src/iosMain/cinterop/h3/headers"
install -m 644 "$BUILD/${APPLE_TARGETS[0]}/h3lib/include/h3api.h" \
               "$MODULE/src/iosMain/cinterop/h3/headers/h3api.h"

echo
echo "Done. Binaries installed:"
find "$MODULE/src/iosMain/cinterop/h3/libs" -name '*.a' | sed "s|$MODULE/||;s|^|  |"
