import sbt.Keys._

import scala.io.Source
import scala.language.postfixOps
import scala.util.Try

lazy val commonSettings =
  Seq(
    organization := "org.mockito",
    //Load version from the file so that Gradle/Shipkit and SBT use the same version
    version := {
      val pattern = """^version=(.+)$""".r
      val source  = Source.fromFile("version.properties")
      val version = Try(source.getLines.collectFirst {
        case pattern(v) => v
      }.get)
      source.close
      version.get
    },
    crossScalaVersions := Seq("2.11.12", "2.12.6"),
    scalacOptions ++= Seq(
      "-unchecked",
      "-feature",
      "-deprecation:false",
      "-encoding", "UTF-8",
      "-Ypartial-unification",
      "-language:higherKinds",
      "-Xfatal-warnings",
//      "-Xmacro-settings:mockito-print-when,mockito-print-do-something,mockito-print-verify,mockito-print-captor,mockito-print-matcher,mockito-print-extractor"
    ),
  )

lazy val commonLibraries = Seq(
  "org.mockito"   % "mockito-core" % "2.23.4",
  "org.scalactic" %% "scalactic"   % "3.0.5",
  "org.scalatest" %% "scalatest"   % "3.0.5" % "provided",
)

lazy val common = (project in file("common"))
  .dependsOn(macroCommon)
  .settings(
    commonSettings,
    libraryDependencies ++= commonLibraries,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val core = (project in file("core"))
  .dependsOn(macroSub % "compile-internal, test-internal")
  .dependsOn(common % "compile-internal, test-internal")
  .settings(
    commonSettings,
    name := "mockito-scala",
    libraryDependencies ++= commonLibraries,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    // include the macro classes and resources in the main jar
    mappings in (Compile, packageBin) ++= mappings
      .in(macroSub, Compile, packageBin)
      .value,
    // include the macro sources in the main source jar
    mappings in (Compile, packageSrc) ++= mappings
      .in(macroSub, Compile, packageSrc)
      .value,
    // include the common classes and resources in the main jar
    mappings in (Compile, packageBin) ++= mappings
      .in(common, Compile, packageBin)
      .value,
    // include the common sources in the main source jar
    mappings in (Compile, packageSrc) ++= mappings
      .in(common, Compile, packageSrc)
      .value,
    // include the common classes and resources in the main jar
    mappings in (Compile, packageBin) ++= mappings
      .in(macroCommon, Compile, packageBin)
      .value,
    // include the common sources in the main source jar
    mappings in (Compile, packageSrc) ++= mappings
      .in(macroCommon, Compile, packageSrc)
      .value,
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/mockito/mockito-scala")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/mockito/mockito-scala"),
        "git@github.com:mockito/mockito-scala.git"
      )
    ),
    developers := List(
      Developer(
        "bbonanno",
        "Bruno Bonanno",
        "bbonanno@gmail.com",
        url("https://github.com/bbonanno")
      )
    )
  )

lazy val macroSub = (project in file("macro"))
  .dependsOn(common)
  .settings(
    commonSettings,
    libraryDependencies ++= commonLibraries,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val macroCommon = (project in file("macro-common"))
  .settings(
    commonSettings,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val root = (project in file("."))
  .settings(
    publish := {},
    publishLocal := {}
  ) aggregate core
