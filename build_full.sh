#!/bin/bash
sbt assembly
mvn clean 
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file 
\ -Dfile=target/scala-2.12/unibz.shapes.convert-assembly-1.0.jar
\ -DgroupId=unibz.shapes 
\ -DartifactId=convert 
\ -Dversion=1.0 
\ -Dpackaging=jar 
\ -DlocalRepositoryPath=`pwd`/repo/
mvn package 
