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

# Copy server
cp ../target/jipipe-webapp-growth-assay-analyzer-*-SNAPSHOT.jar target/ipipe-webapp-growth-assay-analyzer.jar
cp application.yaml target

# Copy fiji
rm -rv Fiji.app
cp -rv ../fiji-bin/Fiji.app target



