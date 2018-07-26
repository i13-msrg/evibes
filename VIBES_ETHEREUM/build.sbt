name := "VIBES_ETHEREUM"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.16"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies ++= Seq("net.debasishg" %% "redisclient" % "3.7")
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.14" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"