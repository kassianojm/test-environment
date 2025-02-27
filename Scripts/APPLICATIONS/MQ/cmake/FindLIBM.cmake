find_path(LIBM_INCLUDE_DIRS
  NAMES math.h
  PATHS /usr/include /usr/local/include /usr/local/bic/include
  NO_DEFAULT_PATH
)
find_library(LIBM_LIBRARIES m)
include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibM DEFAULT_MSG LIBM_LIBRARIES LIBM_INCLUDE_DIRS)
mark_as_advanced(LIBM_INCLUDE_DIRS LIBM_LIBRARIES)

if(LIBM_FOUND)
    if(NOT TARGET LIBM::LIBM)
      add_library(LIBM::LIBM UNKNOWN IMPORTED)
      set_target_properties(LIBM::LIBM PROPERTIES
        IMPORTED_LOCATION "${LIBM_LIBRARIES}"
        INTERFACE_INCLUDE_DIRECTORIES "${LIBM_INCLUDE_DIRS}")
    endif()
endif()
