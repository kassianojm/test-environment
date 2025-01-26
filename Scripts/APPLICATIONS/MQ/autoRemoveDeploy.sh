#!/bin/bash

rm -rf CMakeFiles CMakeCache.txt cmake_install.cmake Makefile message_queue message_queue_dynamic message_queue_static
cmake demo CMakeLists.txt
make
