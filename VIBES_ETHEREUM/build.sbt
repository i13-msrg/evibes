name := "VIBES_ETHEREUM"

version := "0.1"

scalaVersion := "2.12.6"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.16"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies ++= Seq("net.debasishg" %% "redisclient" % "3.7")
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.14" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.17"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-typed" % "2.5.17"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.5" % Test
)
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.5"
libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.2.2"