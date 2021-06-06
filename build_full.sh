#!/bin/bash
mvn clean 
sbt assembly
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
	-Dfile="`pwd`/target/scala-2.13/unibz.shapes.convert-assembly-1.0.jar" \
	-DgroupId="unibz.shapes" \
	-DartifactId="convert" \
	-Dversion="1.0" \
	-Dpackaging="jar" \
	-DlocalRepositoryPath="`pwd`/build/repo/"
mvn package
mv target/valid-1.0-SNAPSHOT.jar build/
mvn clean
