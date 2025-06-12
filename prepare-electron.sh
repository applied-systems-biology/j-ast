#!/bin/bash

# Build backend
pushd backend || exit 1
mvn package || exit 1
popd || exit 1

# Create directory structure for electron
rm -rvf frontend/backend-electron/share frontend/backend-electron/bin frontend/backend-electron/backend.jar
mkdir -p frontend/backend-electron
cp -v backend/target/j-ast-backend*.jar frontend/backend-electron/backend.jar
cp -rv share frontend/backend-electron/share
cp -rv bin frontend/backend-electron/bin

echo "Electron directory structure was created!"


