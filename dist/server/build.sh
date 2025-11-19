#!/bin/bash

set -Eeuo pipefail

# JIPipe downloads
JIPIPE_WINDOWS="https://github.com/applied-systems-biology/jipipe/releases/download/pom-jipipe-5.3.0/JIPipe-5.3.0-Prepackaged-Win64.zip"
JIPIPE_LINUX="https://github.com/applied-systems-biology/jipipe/releases/download/pom-jipipe-5.3.0/JIPipe-5.3.0-Prepackaged-Linux64.tar.gz"
JIPIPE_MACOS="https://github.com/applied-systems-biology/jipipe/releases/download/pom-jipipe-5.3.0/JIPipe-5.3.0-Prepackaged-macos.zip"

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
rm -v "j-ast-server-${JAST_VERSION}-linux-x64.tar.gz" || true
rm -v "j-ast-server-${JAST_VERSION}-windows-x64.zip" || true
rm -v "j-ast-server-${JAST_VERSION}-macos-arm64.zip" || true

# Build backend/frontend hybrid
echo "-----------------------------------------"
echo "Building backend/frontend hybrid ..."
echo "-----------------------------------------"

pushd "../.." || exit 1
mvn clean package -DskipTests
mkdir -p "$TMP_DIR/j-ast-server-linux-x64" "$TMP_DIR/j-ast-server-windows-x64" "$TMP_DIR/j-ast-server-macos-arm64"
cp -v backend/target/j-ast-backend*.jar "$TMP_DIR/j-ast-server-linux-x64/server.jar"
cp -v backend/target/j-ast-backend*.jar "$TMP_DIR/j-ast-server-windows-x64/server.jar"
cp -v backend/target/j-ast-backend*.jar "$TMP_DIR/j-ast-server-macos-arm64/server.jar"
popd || exit 1

# Build backend directories
echo "-----------------------------------------"
echo "Building backend dirs ..."
echo "-----------------------------------------"

# Copy shared directory
cp -rv "../../share" "$TMP_DIR/j-ast-server-linux-x64/share"
cp -rv "../../share" "$TMP_DIR/j-ast-server-windows-x64/share"
cp -rv "../../share" "$TMP_DIR/j-ast-server-macos-arm64/share"

pushd "$TMP_DIR/j-ast-server-linux-x64/" || exit 1

# Install JIPipe
wget -qO jipipe.tar.gz "$JIPIPE_LINUX"
tar -xf jipipe.tar.gz
mkdir -p bin
mv JIPipe*/bin bin/jipipe-linux
rm -r JIPipe*
rm jipipe.tar.gz

popd || exit 1

pushd "$TMP_DIR/j-ast-server-windows-x64/" || exit 1

# Install JIPipe
wget -qO jipipe.zip "$JIPIPE_WINDOWS"
unzip -q jipipe.zip
mkdir -p bin
mv JIPipe*/bin bin/jipipe-windows
rm -r JIPipe*
rm jipipe.zip

popd || exit 1

pushd "$TMP_DIR/j-ast-server-macos-arm64" || exit 1

# Install JIPipe
wget -qO jipipe.zip "$JIPIPE_MACOS"
unzip -q jipipe.zip
mkdir -p bin
mv JIPipe*/Contents/Resources/bin bin/jipipe-macos
rm -r JIPipe*
rm jipipe.zip

popd || exit 1

# Build backend directories
echo "-----------------------------------------"
echo "Creating packages ..."
echo "-----------------------------------------"

pushd "$TMP_DIR" || exit 1

tar -cvzf "../j-ast-server-${JAST_VERSION}-linux-x64.tar.gz" j-ast-server-linux-x64
zip -rv "../j-ast-server-${JAST_VERSION}-windows-x64.zip" j-ast-server-windows-x64
zip -rv "../j-ast-server-${JAST_VERSION}-macos-arm64.zip" j-ast-server-macos-arm64

popd || exit 1