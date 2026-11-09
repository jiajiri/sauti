cmake_minimum_required(VERSION 3.22.1)
project("sauti_native_engine")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for ARM mobile architectures (NEON SIMD)
if(CMAKE_SYSTEM_PROCESSOR MATCHES "arm|aarch64")
    add_compile_options(-O3 -ffast-math)
endif()

add_library(
    sauti_native_engine
    SHARED
    voice_engine_jni.cpp
)

target_include_directories(
    sauti_native_engine
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

find_library(
    log-lib
    log
)

target_link_libraries(
    sauti_native_engine
    ${log-lib}
)
