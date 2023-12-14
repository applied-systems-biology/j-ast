#!/bin/bash

pushd ..
mvn clean
mvn package
popd

rm -rvf target
mkdir target

cp -v ../target/jipipe-webapp-growth-assay-analyzer-*.jar target/webapp.jar
cp -rv ../fiji-bin/linux target/fiji-bin-linux

rm -rv target/fiji-bin-linux/jipipe/backups