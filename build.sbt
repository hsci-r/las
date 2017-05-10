name := """las"""

version := "1.5.8"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
    "fi.seco" % "lexicalanalysis" % "1.5.8",
    "com.optimaize.languagedetector" % "language-detector" % "0.6",
    "com.github.scopt" %% "scopt" % "3.5.0",
    "com.typesafe.play" %% "play-json" % "2.6.0-M3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.2.2" % "runtime"
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

assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(Seq("#!/usr/bin/env sh", """exec java -jar -Xmx4G "$0" "$@"""")))

assemblyJarName in assembly := "las"
