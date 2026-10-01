cmake_minimum_required(VERSION 3.22.1)
project("eggyhub_native")

add_library(
        eggyhub_native
        SHARED
        native-lib.cpp
        resource_utils.cpp
        anti_debug.cpp
        signature_verification.cpp
        emulator_detection.cpp)

find_library(log-lib log)

target_link_libraries(
        eggyhub_native
        ${log-lib})
