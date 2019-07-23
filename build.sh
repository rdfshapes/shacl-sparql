#!/bin/bash
mvn package
cp target/valid-1.0-SNAPSHOT.jar lib
sbt package
