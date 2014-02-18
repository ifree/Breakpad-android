#!/bin/bash
# https://vilimpoc.org/blog/2013/10/08/ndk-configure-sh/
# Go to the directory containing the configure script.
#
# Then just run something like:
#
EXAMPLE="~/android-ndk-r8d ./ndk-configure.sh android-14 arch-arm"
NDK="d:/NVPACK/android-ndk-r8d"
if [ -z "${NDK}" ]; then
    echo "Need to specify NDK environment variable when calling."
    echo "  -- e.g. ${EXAMPLE}"
    exit 1
fi

if [ "$#" -lt "2" ]; then
    echo "Usage: ./ndk-configure.sh <platform> <architecture>"
    echo "  -- e.g. ${EXAMPLE}"
    exit 1
else
    PLATFORM=$1
    shift

    ARCHITECTURE=$1
    shift

    COMPILER_VERSION="4.7"

    export NDK_TOOLCHAIN=${NDK}/toolchains/arm-linux-androideabi-${COMPILER_VERSION}/prebuilt/windows/bin
    export CROSS_COMPILE=arm-linux-androideabi

    export AR=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-ar
    export CC=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-gcc
    export CXX=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-g++
    export CXXCPP=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-cpp
    export LD=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-ld
    export NM=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-nm
    export OBJDUMP=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-objdump
    export RANLIB=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-ranlib
    export STRIP=${NDK_TOOLCHAIN}/${CROSS_COMPILE}-strip

    export SYSROOT=${NDK}/platforms/${PLATFORM}/${ARCHITECTURE}                  # Needed for the Android-specific headers and libs.
    export CXX_SYSROOT=${NDK}/sources/cxx-stl/gnu-libstdc++/${COMPILER_VERSION}  # Needed for the STL headers and libs.
    export CXX_BITS_INCLUDE=${CXX_SYSROOT}/libs/armeabi/include                  # Needed for the <bits/c++config.h> and other headers.
                                                                                 # Certain STL classes, like <unordered_map> won't be 
                                                                                 # usable otherwise.

    export CPPFLAGS="-I${SYSROOT}/usr/include"
    export CFLAGS="--sysroot=${SYSROOT} -O2 -frtti -DANDROID"
    export CXXFLAGS="--sysroot=${SYSROOT} -I${CXX_SYSROOT}/include -I${CXX_BITS_INCLUDE} -O2 -frtti -DANDROID"

    export LIBS="-lc -lstdc++ -lgnustl_static -llog"
    export LDFLAGS="-Wl,-rpath-link=${SYSROOT}/usr/lib -L${SYSROOT}/usr/lib -L${CXX_SYSROOT}/lib -L${CXX_SYSROOT}/libs/armeabi"

    MACHINE=$( ${CXX} -dumpmachine )

    echo "Machine:           ${MACHINE}"
    echo "Sysroot (Android): ${SYSROOT}"
    echo "Sysroot (cxx):     ${CXX_SYSROOT}"
    echo "Bits include:      ${CXX_BITS_INCLUDE}"

    argString=""
    
    while [ "$1" != "" ]; do
    argString="${argString} $1"
    shift
    done

    ./configure --host="arm-linux-androideab" $argString

fi
