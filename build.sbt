name := "test"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"


// scalaz-bintray resolver needed for specs2 library
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies += ws

libraryDependencies ++= Seq( "org.webjars" % "flot" % "0.8.3",
                             "com.typesafe.akka" %% "akka-persistence" % "2.4.11",
                             "org.iq80.leveldb" % "leveldb" % "0.7",
                             "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
                             "org.typelevel" %% "cats" % "0.9.0")

lazy val akkaVersion = "2.4.11"

fork := true

