#!/bin/bash

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
rm -rvf "$TMP_DIR"
mkdir -p "$TMP_DIR"

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
mv -v "$TMP_DIR/j-ast-macos-arm64/backend-electron" "$TMP_DIR/j-ast-macos-arm64/J-AST.app/Content/Resources/backend-electron"

popd || exit 1