cmake_minimum_required(VERSION 3.22.1)
project(rz_audio_engine LANGUAGES CXX)

add_library(rz_audio_engine SHARED
    audio_engine.cpp
)

find_package(oboe REQUIRED CONFIG)

target_compile_features(rz_audio_engine PRIVATE cxx_std_17)
target_compile_options(rz_audio_engine PRIVATE
    -Wall -Wextra -Werror=return-type
    -fno-exceptions
    -fno-rtti
)

target_link_libraries(rz_audio_engine
    PRIVATE
    oboe::oboe
    log
)
