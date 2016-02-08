name := """las-cl"""

version := "1.0"

scalaVersion := "2.11.5"

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

libraryDependencies ++= Seq(
    "fi.seco" % "lexicalanalysis" % "1.1.2",
    "com.cybozu.labs" % "langdetect" % "1.2.2" exclude("net.arnx.jsonic", "jsonic"),
    "net.arnx" % "jsonic" % "1.3.0", //langdetect pulls in ancient unavailable version
    "com.github.scopt" %% "scopt" % "3.3.0",
    "com.typesafe.play" %% "play-json" % "2.3.4",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime"
)
resolvers ++= Seq(
    "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository")

fork in run := true

assemblyMergeStrategy in assembly := {
  case "is2/util/DB.class" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(Seq("#!/usr/bin/env sh", """exec java -jar "$0" "$@"""")))

assemblyJarName in assembly := "las"

scalacOptions += "-target:jvm-1.7"
