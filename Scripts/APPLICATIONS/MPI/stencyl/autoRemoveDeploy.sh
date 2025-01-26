#!/bin/bash

rm -rf CMakeFiles CMakeCache.txt cmake_install.cmake Makefile stencyl
cmake CMakeLists.txt
make
