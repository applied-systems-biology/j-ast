#!/bin/bash

pushd ../.. || exit 1
mvn clean || exit 1
mvn package || exit 1
popd || exit 1

rm -rvf target
mkdir target

cp -v ../../backend/target/j-ast-backend-*.jar target/webapp.jar
cp -rv ../../bin target/bin
cp -rv ../../share target/share

ZIP_FILE="j-ast-docker-$(date +%d%m%Y).zip"
rm $ZIP_FILE
zip -rv $ZIP_FILE target Dockerfile README.md docker-compose.yml

ZIP_FILE="j-ast-docker-update-$(date +%d%m%Y).zip"
rm $ZIP_FILE
zip -rv $ZIP_FILE target