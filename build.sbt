val scala3Version = "3.2.0"
val zioVersion = "2.0.2"

ThisBuild / name := "pps-project-zio"
ThisBuild / scalaVersion := scala3Version
ThisBuild / version := "0.1.0-SNAPSHOT"

run / fork := false
Global / cancelable := false

lazy val root = project
  .in(file("."))
  .aggregate(
    common,
    backend,
    frontend,
  )


lazy val common = project
  .in(file("common"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-json" % "0.3.0",
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-junit" % zioVersion % Test
    )
  )

lazy val backend = project
  .in(file("backend"))
  .dependsOn(common)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-json" % "0.3.0",
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-junit" % zioVersion % Test,
      "io.d11" %% "zhttp" % "2.0.0-RC11",
    )
  )

lazy val frontend = project
  .in(file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(common)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.1.0",
      "dev.zio" %%% "zio-json" % "0.3.0",
      "com.lihaoyi" %%% "scalatags" % "0.12.0",
    )
  )
