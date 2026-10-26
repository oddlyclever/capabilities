# Static H3 build used to produce the iOS cinterop libraries (libh3.a, one per Apple target).
# Driven by native/build-natives.sh, not by Gradle directly. See native/PROVENANCE for the
# vendored upstream tag. Android does not compile H3 at all: it uses com.uber:h3-android.
cmake_minimum_required(VERSION 3.22.1)
project(h3 C)

set(H3LIB ${CMAKE_CURRENT_SOURCE_DIR}/native/h3lib)

file(READ ${H3LIB}/VERSION H3_VERSION LIMIT_COUNT 1)
string(REPLACE "\n" "" H3_VERSION "${H3_VERSION}")
string(REGEX REPLACE "-.*$" "" H3_VERSION "${H3_VERSION}")
string(REPLACE "." ";" H3_VERSION_LIST "${H3_VERSION}")
list(GET H3_VERSION_LIST 0 H3_VERSION_MAJOR)
list(GET H3_VERSION_LIST 1 H3_VERSION_MINOR)
list(GET H3_VERSION_LIST 2 H3_VERSION_PATCH)
configure_file(${H3LIB}/include/h3api.h.in ${CMAKE_CURRENT_BINARY_DIR}/h3lib/include/h3api.h)

file(GLOB H3_SOURCES ${H3LIB}/lib/*.c)

add_library(h3 STATIC ${H3_SOURCES})
target_include_directories(h3 PUBLIC
        $<BUILD_INTERFACE:${H3LIB}/include>
        $<BUILD_INTERFACE:${CMAKE_CURRENT_BINARY_DIR}/h3lib/include>)
