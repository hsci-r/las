name := """las"""

version := "1.5.4"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
    "fi.seco" % "lexicalanalysis" % "1.5.4",
    "com.optimaize.languagedetector" % "language-detector" % "0.5",
    "com.github.scopt" %% "scopt" % "3.4.0",
    "com.typesafe.play" %% "play-json" % "2.5.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7" % "runtime"
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
