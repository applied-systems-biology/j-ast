#!/bin/bash

pushd .. || exit 1
mvn clean || exit 1
mvn package || exit 1
popd || exit 1

rm -rvf app
mkdir app

cp -v ../target/jipipe-webapp-growth-assay-analyzer-*.jar app/webapp.jar
cp -rv ../fiji-bin/windows app/fiji-bin-windows
cp -rv jre app/jre

rm -rv app/fiji-bin-windows/jipipe/backups

ZIP_FILE="webapp-windows-$(date +%d%m%Y).zip"
rm $ZIP_FILE

zip -rv $ZIP_FILE app webapp.bat README.txt
