cmake_minimum_required(VERSION 3.22.1)
project("sauti_native_engine")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for mobile architectures
if(CMAKE_SYSTEM_PROCESSOR MATCHES "arm|aarch64")
    add_compile_options(-O3 -fno-finite-math-only)
endif()

# llama.cpp flags
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build static libraries" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Do not build examples" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Do not build tests" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Do not build server" FORCE)

add_subdirectory(llama.cpp)

# whisper.cpp flags
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "Do not build examples" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "Do not build tests" FORCE)

add_subdirectory(whisper.cpp)

add_library(
    sauti_native_engine
    SHARED
    voice_engine_jni.cpp
)

target_include_directories(
    sauti_native_engine
    PRIVATE
    
    /llama.cpp/include
    /llama.cpp/ggml/include
    /whisper.cpp/include
)

target_link_libraries(
    sauti_native_engine
    llama
    whisper
    log
    android
)
