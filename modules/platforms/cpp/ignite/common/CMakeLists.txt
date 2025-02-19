#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

project(ignite-common)

set(TARGET ${PROJECT_NAME})

set(PUBLIC_HEADERS
    bits.h
    bytes.h
    bytes_view.h
    config.h
    ignite_error.h
    ignite_result.h
    uuid.h)

add_library(${TARGET} INTERFACE)

target_include_directories(${TARGET} INTERFACE
    $<BUILD_INTERFACE:${CMAKE_SOURCE_DIR}>
    $<INSTALL_INTERFACE:${IGNITE_INCLUDEDIR}>)

ignite_install_headers(FILES ${PUBLIC_HEADERS} DESTINATION ${IGNITE_INCLUDEDIR}/common)

ignite_test(bits_test bits_test.cpp LIBS ${TARGET})
ignite_test(bytes_test bytes_test.cpp LIBS ${TARGET})
ignite_test(uuid_test uuid_test.cpp LIBS ${TARGET})
