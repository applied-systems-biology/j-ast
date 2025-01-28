#!/bin/bash

export API_LOCATION=/j-ast-new/api/
export FRONTEND_LOCATION=/j-ast-new/

pushd ../.. || exit 1
mvn clean || exit 1
mvn -Dapi.location=${API_LOCATION} -Dfrontend.location=${FRONTEND_LOCATION} package || exit 1
popd || exit 1

rm -rvf package
mkdir -p package/target

cp -v ../../backend/target/j-ast-backend-*.jar package/target/webapp.jar
cp -rv ../../bin package/target/bin
cp -rv ../../share package/target/share
cp Dockerfile package
cp docker-compose.yml package

ZIP_FILE="j-ast-docker-$(date +%d%m%Y).zip"
rm $ZIP_FILE
pushd package || exit 1
zip -rv ../$ZIP_FILE *
popd || exit 1
