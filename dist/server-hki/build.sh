#!/bin/bash

set -Eeuo pipefail

API_LOCATION=/api
FRONTEND_LOCATION=/
BUILD_NUMBER="0"

if [ "$1" != "" ]; then
  echo "-> Using build number $1"
  BUILD_NUMBER=$1
fi
if [ "$2" != "" ]; then
  echo "-> Using custom frontend location $2"
  FRONTEND_LOCATION=$2
fi
if [ "$3" != "" ]; then
  echo "-> Using custom API location $3"
  API_LOCATION=$3
fi

# JIPipe downloads
JIPIPE_LINUX="https://github.com/applied-systems-biology/jipipe/releases/download/pom-jipipe-5.3.0/JIPipe-5.3.0-Prepackaged-Linux64.tar.gz"

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

JAST_VERSION="$JAST_BASE_VERSION.$BUILD_NUMBER"
echo "-----> Final J-AST version is $JAST_VERSION"

# Delete old packages
rm -v "j-ast-server-${JAST_VERSION}-linux-x64.tar.gz" || true

echo "-----------------------------------------"
echo "Applying API patch for frontend ..."
echo "-----------------------------------------"

pushd "../.." || exit 1
patch frontend/src/types/api.ts < dist/server-hki/frontend-api.patch
popd || exit 1

# Build backend/frontend hybrid
echo "-----------------------------------------"
echo "Building backend/frontend hybrid ..."
echo "-----------------------------------------"

pushd "../.." || exit 1
mvn clean package -DskipTests -Dfrontend.location="$FRONTEND_LOCATION" -Dapi.location="$API_LOCATION"
mkdir -p "$TMP_DIR/j-ast-server-linux-x64"
cp -v backend/target/j-ast-backend*.jar "$TMP_DIR/j-ast-server-linux-x64/server.jar"
popd || exit 1

# Build backend directories
echo "-----------------------------------------"
echo "Building backend dirs ..."
echo "-----------------------------------------"

# Copy shared directory
cp -rv "../../share" "$TMP_DIR/j-ast-server-linux-x64/share"

# Replace artifact with system version if available (needed for more stability)
pushd "$TMP_DIR/j-ast-server-linux-x64/share" || exit 1

for f in *-system.jip; do
  base="${f%-system.jip}.jip"
  mv -vf -- "$f" "$base"
done

popd || exit 1

pushd "$TMP_DIR/j-ast-server-linux-x64/" || exit 1

# Install JIPipe
wget -qO jipipe.tar.gz "$JIPIPE_LINUX"
tar -xf jipipe.tar.gz
mkdir -p bin
mv JIPipe*/bin bin/jipipe-linux
rm -r JIPipe*
rm jipipe.tar.gz

popd || exit 1

# Build backend directories
echo "-----------------------------------------"
echo "Creating packages ..."
echo "-----------------------------------------"

pushd "$TMP_DIR" || exit 1

tar -cvzf "../j-ast-server-${JAST_VERSION}-linux-x64.tar.gz" j-ast-server-linux-x64

popd || exit 1