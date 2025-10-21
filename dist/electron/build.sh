#!/bin/bash

set -Eeuo pipefail

# JIPipe downloads
JIPIPE_WINDOWS="https://github.com/applied-systems-biology/jipipe/releases/download/pom-jipipe-5.3.0/JIPipe-5.3.0-Prepackaged-Win64.zip"
JIPIPE_LINUX="https://github.com/applied-systems-biology/jipipe/releases/download/pom-jipipe-5.3.0/JIPipe-5.3.0-Prepackaged-Linux64.tar.gz"
JIPIPE_MACOS="https://github.com/applied-systems-biology/jipipe/releases/download/pom-jipipe-5.3.0/JIPipe-5.3.0-Prepackaged-macos.zip"

# Java downloads
JAVA_WINDOWS="https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse"
JAVA_LINUX="https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jre/hotspot/normal/eclipse"
JAVA_MACOS="https://api.adoptium.net/v3/binary/latest/21/ga/mac/aarch64/jre/hotspot/normal/eclipse"

# Create temporary directories
TMP_DIR="$PWD/tmp"
ROOT_DIR=$(realpath "$PWD/../..")
rm -rvf "$TMP_DIR" || true
mkdir -p "$TMP_DIR"

# Version detection
pushd ../.. || exit 1
echo "Detecting J-AST base version ..."
JAST_BASE_VERSION="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | grep -Po '\d+\.\d+\.\d+')"
popd || exit 1

# Build number: arg2 > CI var > 0
BUILD_NUMBER="${2:-${CI_PIPELINE_IID:-0}}"

JAST_VERSION="$JAST_BASE_VERSION.$BUILD_NUMBER"
echo "-----> Final J-AST version is $JAST_VERSION"

# Delete old packages
rm -v "j-ast-${JAST_VERSION}-linux-x64.tar.gz" || true
rm -v "j-ast-${JAST_VERSION}-windows-x64.zip" || true
rm -v "j-ast-${JAST_VERSION}-macos-arm64.zip" || true
rm -v "j-ast-${JAST_VERSION}-windows-x64-installer.exe" || true

# Build backend
echo "-----------------------------------------"
echo "Building backend ..."
echo "-----------------------------------------"

pushd "../../backend" || exit 1
mvn clean package
cp -v target/j-ast-backend*.jar "$TMP_DIR/backend.jar"
popd || exit 1

# Build frontend electron packages
echo "-----------------------------------------"
echo "Building frontend ..."
echo "-----------------------------------------"

pushd "../../frontend" || exit 1
npm install
quasar build -m electron

cp -rv ./dist/electron/Packaged/J-AST-linux-x64 "$TMP_DIR/j-ast-linux-x64"
cp -rv ./dist/electron/Packaged/J-AST-win32-x64 "$TMP_DIR/j-ast-windows-x64"
cp -rv ./dist/electron/Packaged/J-AST-darwin-arm64 "$TMP_DIR/j-ast-macos-arm64"
popd || exit 1

# Build backend directories
echo "-----------------------------------------"
echo "Building backend dirs ..."
echo "-----------------------------------------"

mkdir -p "$TMP_DIR/j-ast-linux-x64/backend-electron"
mkdir -p "$TMP_DIR/j-ast-windows-x64/backend-electron"
mkdir -p "$TMP_DIR/j-ast-macos-arm64/backend-electron"

# Copy backend jars
cp -v "$TMP_DIR/backend.jar" "$TMP_DIR/j-ast-linux-x64/backend-electron/backend.jar"
cp -v "$TMP_DIR/backend.jar" "$TMP_DIR/j-ast-windows-x64/backend-electron/backend.jar"
cp -v "$TMP_DIR/backend.jar" "$TMP_DIR/j-ast-macos-arm64/backend-electron/backend.jar"

# Copy shared directory
cp -rv "../../share" "$TMP_DIR/j-ast-linux-x64/backend-electron/share"
cp -rv "../../share" "$TMP_DIR/j-ast-windows-x64/backend-electron/share"
cp -rv "../../share" "$TMP_DIR/j-ast-macos-arm64/backend-electron/share"

# Copy icons
cp -v "$ROOT_DIR/j-ast-icon.svg" "$TMP_DIR/j-ast-linux-x64/"
cp -v "$ROOT_DIR/j-ast-icon.ico" "$TMP_DIR/j-ast-windows-x64/"

pushd "$TMP_DIR/j-ast-linux-x64/backend-electron" || exit 1

# Install Java
wget -O jdk.tar.gz "$JAVA_LINUX"
tar -xvf jdk.tar.gz
rm jdk.tar.gz
mv jdk* jdk

# Install JIPipe
wget -O jipipe.tar.gz "$JIPIPE_LINUX"
tar -xvf jipipe.tar.gz
mv JIPipe* jipipe-linux
rm jipipe.tar.gz

popd || exit 1

pushd "$TMP_DIR/j-ast-windows-x64/backend-electron" || exit 1

# Install Java
wget -O jdk.zip "$JAVA_WINDOWS"
unzip jdk.zip
rm jdk.zip
mv jdk* jdk

# Install JIPipe
wget -O jipipe.zip "$JIPIPE_WINDOWS"
unzip jipipe.zip
mv JIPipe* jipipe-windows
rm jipipe.zip

popd || exit 1

pushd "$TMP_DIR/j-ast-macos-arm64/backend-electron" || exit 1

# Install Java
wget -O jdk.tar.gz "$JAVA_MACOS"
tar -xvf jdk.tar.gz
rm jdk.tar.gz
mv jdk* jdk

# Install JIPipe
wget -O jipipe.zip "$JIPIPE_MACOS"
unzip jipipe.zip
mv JIPipe* jipipe-macos
rm jipipe.zip

# Move macos backend dir
mv -v "$TMP_DIR/j-ast-macos-arm64/backend-electron" "$TMP_DIR/j-ast-macos-arm64/J-AST.app/Contents/Resources/backend-electron"

popd || exit 1

# Build backend directories
echo "-----------------------------------------"
echo "Creating packages ..."
echo "-----------------------------------------"

pushd "$TMP_DIR" || exit 1

tar -cvzf "../j-ast-${JAST_VERSION}-linux-x64.tar.gz" j-ast-linux-x64
zip -rv "../j-ast-${JAST_VERSION}-windows-x64.zip" j-ast-windows-x64

pushd j-ast-macos-arm64 || exit 1
zip -rv "../../j-ast-${JAST_VERSION}-macos-arm64.zip" J-AST.app
popd || exit 1

# Windows package
cp -rv ../nsis ./nsis
cp -rv ./j-ast-windows-x64 ./nsis/j-ast-windows-x64
pushd nsis || exit 1
sed -i "s/%%JAST_VERSION%%/${JAST_VERSION}/g" j-ast-installer-win64.nsi
makensis j-ast-installer-win64.nsi
cp -v j-ast-*-windows-x64-installer.exe ../../
popd || exit 1

popd || exit 1