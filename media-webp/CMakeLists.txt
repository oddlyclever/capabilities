# libwebp's encoder for :webp, built by native/build-natives.sh, not by Gradle.
#
#   iOS:     libwebp.a, a static library cinterop embeds in the published klib.
#   Android: libwebp_jni.so, the encoder and its JNI binding (native/jni/webp_jni.c), one per ABI.
#
# The decoder is compiled too: parts of the encoder call into it. The container libraries (mux, demux) are not
# vendored; metadata is written in Kotlin. See native/PROVENANCE for the upstream release.
cmake_minimum_required(VERSION 3.22.1)
project(webp C)

set(LIBWEBP ${CMAKE_CURRENT_SOURCE_DIR}/native/libwebp)
file(GLOB_RECURSE WEBP_SOURCES
        ${LIBWEBP}/src/dec/*.c
        ${LIBWEBP}/src/enc/*.c
        ${LIBWEBP}/src/dsp/*.c
        ${LIBWEBP}/src/utils/*.c
        ${LIBWEBP}/sharpyuv/*.c)

if(ANDROID)
    add_library(webp_jni SHARED native/jni/webp_jni.c ${WEBP_SOURCES})
    target_include_directories(webp_jni PRIVATE ${LIBWEBP} ${LIBWEBP}/src)
    # Only JNI_OnLoad is exported. libwebp marks its API for export, and another library in the same app (an image
    # loader with a libwebp of its own) exporting the same names could be bound in place of these.
    target_compile_definitions(webp_jni PRIVATE WEBP_USE_THREAD "WEBP_EXTERN=extern")
    # 16 KB pages, which Android 15 devices may use; every symbol resolved at build time, not on a device.
    target_link_options(webp_jni PRIVATE -Wl,-z,max-page-size=16384 -Wl,--no-undefined -Wl,--gc-sections)
    target_compile_options(webp_jni PRIVATE -ffunction-sections -fdata-sections -fvisibility=hidden)
    find_library(log-lib log)
    target_link_libraries(webp_jni ${log-lib})
else()
    add_library(webp STATIC ${WEBP_SOURCES})
    target_include_directories(webp PRIVATE ${LIBWEBP} ${LIBWEBP}/src)
    target_compile_definitions(webp PRIVATE WEBP_USE_THREAD)
endif()
