name := "shacl_sparql"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "es.weso" %% "schema" % "0.1.20"
libraryDependencies += "com.google.guava" % "guava" % "12.0"
//libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
