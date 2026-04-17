# CMakeLists.txt for Breezy Assistant's JNI
cmake_minimum_required(VERSION 3.22.1)

project("breezy_native")

# Build the native library
add_library(
    # Sets the name of the library.
    llama

    # Sets the library as a shared library.
    SHARED

    # Provides a relative path to your source file(s).
    native-lib.cpp
)

# Search for NDK's log library
find_library(
    log-lib
    log
)

# Link the native library with log-lib
target_link_libraries(
    llama
    ${log-lib}
)
