name := "unibz.shapes.convert"

version := "1.0"

scalaVersion := "2.12.8"


libraryDependencies += "es.weso" %% "schema" % "0.1.20"

// https://mvnrepository.com/artifact/org.scalatest/scalatest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "28.0-jre"
// https://mvnrepository.com/artifact/org.eclipse.rdf4j/rdf4j-runtime
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-runtime" % "2.5.3"
// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.26"
// https://mvnrepository.com/artifact/com.google.code.gson/gson
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.5"
// https://mvnrepository.com/artifact/junit/junit
libraryDependencies += "junit" % "junit" % "4.10" % Test


addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
resolvers in Global ++= Seq(
  "Sbt plugins"                   at "https://dl.bintray.com/sbt/sbt-plugin-releases"
//  "Maven Central Server"          at "http://repo1.maven.org/maven2",
//  "TypeSafe Repository Releases"  at "http://repo.typesafe.com/typesafe/releases/",
//  "TypeSafe Repository Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}