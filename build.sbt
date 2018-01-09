lazy val commonSettings = Seq(
  organization := "fi.seco",
  version := "1.5.14",
  scalaVersion := "2.12.4",
  libraryDependencies ++= Seq(
    "fi.seco" % "lexicalanalysis" % "1.5.14",
    "com.optimaize.languagedetector" % "language-detector" % "0.6",
    "com.github.scopt" %% "scopt" % "3.5.0",
    "com.typesafe.play" %% "play-json" % "2.6.0-M3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.2.2" % "runtime"
  ),
  resolvers ++= Seq(
    "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"),
  fork in run := true,
)

lazy val rootSettings = Seq(
  publishArtifact := false,
  publishArtifact in Test := false,
)

lazy val assemblySettings = Seq(
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case "is2/util/DB.class" => MergeStrategy.first
    case "fi/seco/lexical/hfst/resources.lst" => MergeStrategy.filterDistinctLines
    case other: Any => MergeStrategy.defaultMergeStrategy(other)
  },
  mainClass in assembly := Some("LASCommandLineTool"),
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(Seq("#!/usr/bin/env sh", """exec java -jar "$0" "$@"""" + "\n")))
)

lazy val main = project.in(file("build/main"))
  .settings(commonSettings:_*)
  .settings(scalaSource in Compile := baseDirectory.value / "../../src/main/scala")
  .disablePlugins(AssemblyPlugin)

lazy val fiComplete = (project in file("build/las-fi"))
  .settings(commonSettings:_*)
  .settings(assemblySettings:_*)
  .dependsOn(main)
  .settings(
    name := "las-fi",
    libraryDependencies += "fi.seco" % "lexicalanalysis-resources-fi-complete" % "1.5.14",
    assemblyOutputPath in assembly := file("dist/las-fi")
  )

lazy val fiSmall = (project in file("build/las-fi-small"))
  .settings(commonSettings:_*)
  .settings(assemblySettings:_*)
  .dependsOn(main)
  .settings(
    name := "las-fi-small",
    libraryDependencies += "fi.seco" % "lexicalanalysis-resources-fi-core" % "1.5.14",
    assemblyOutputPath in assembly := file("dist/las-fi-small")
  )

lazy val other = (project in file("build/las-non-fi"))
  .settings(commonSettings:_*)
  .settings(assemblySettings:_*)
  .dependsOn(main)
  .settings(
    name := "las-non-fi",
    libraryDependencies += "fi.seco" % "lexicalanalysis-resources-other" % "1.5.14",
    assemblyOutputPath in assembly := file("dist/las-non-fi")
  )

lazy val smallComplete = (project in file("build/las-small"))
  .settings(commonSettings:_*)
  .settings(assemblySettings:_*)
  .dependsOn(main)
  .settings(
    name := "las-complete",
    libraryDependencies ++= Seq(
      "fi.seco" % "lexicalanalysis-resources-fi-core" % "1.5.14",
      "fi.seco" % "lexicalanalysis-resources-other" % "1.5.14"
    ),
    assemblyOutputPath in assembly := file("dist/las-small")
  )


lazy val complete = (project in file("build/las-complete"))
  .settings(commonSettings:_*)
  .settings(assemblySettings:_*)
  .dependsOn(main)
  .settings(
    name := "las-complete",
    libraryDependencies ++= Seq(
      "fi.seco" % "lexicalanalysis-resources-fi-complete" % "1.5.14",
      "fi.seco" % "lexicalanalysis-resources-other" % "1.5.14"
    ),
    assemblyOutputPath in assembly := file("dist/las")
  )

lazy val las = project.in(file("."))
  .settings(commonSettings:_*)
  .settings(rootSettings:_*)
  .disablePlugins(AssemblyPlugin)
  .aggregate(complete,fiSmall,fiComplete,other,smallComplete)

