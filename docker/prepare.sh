#!/bin/bash

pushd .. || exit 1
mvn clean || exit 1
mvn package || exit 1
popd || exit 1

rm -rvf target
mkdir target

cp -v ../target/j-ast-*.jar target/webapp.jar
cp -rv ../fiji-bin/linux target/fiji-bin-linux

rm -rv target/fiji-bin-linux/jipipe/backups

ZIP_FILE="webapp-docker-$(date +%d%m%Y).zip"
rm $ZIP_FILE
zip -rv $ZIP_FILE target Dockerfile README.md docker-compose.yml
