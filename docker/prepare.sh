#!/bin/bash

if (( $EUID == 0 )); then
    echo "Do not run this script as root"
    exit
fi

# Cleanup target dir
rm -rv target
mkdir target

# Build maven
pushd ..
mvn clean
mvn package
popd

# Copy server jar

# Copy fiji
rm -rv Fiji.app
cp -v ../fiji-bin/Fiji.app target



