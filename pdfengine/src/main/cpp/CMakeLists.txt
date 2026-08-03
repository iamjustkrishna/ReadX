cmake_minimum_required(VERSION 3.22.1)

project("readx_pdf_engine")

set(PDFIUM_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/pdfium)

add_library(pdfium SHARED IMPORTED)
set_target_properties(pdfium PROPERTIES
        IMPORTED_LOCATION ${PDFIUM_DIR}/libs/${ANDROID_ABI}/libpdfium.so)

add_library(readx_pdf_engine SHARED
        pdfium_bridge.cpp)

target_include_directories(readx_pdf_engine PRIVATE ${PDFIUM_DIR}/include)

# 16 KB page-size alignment (NDK r27 defaults to this; keep explicit for safety)
target_link_options(readx_pdf_engine PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(readx_pdf_engine
        pdfium
        jnigraphics
        android
        log)
