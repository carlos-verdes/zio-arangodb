import Dependencies._
import Libraries._

Global / onChangedBuildSource := ReloadOnSourceChanges

Test / fork := true
IntegrationTest / fork := true

inThisBuild(
  List(
    organization := "io.funkode",
    scalaVersion := "3.2.2-RC1",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
  )
)

ThisBuild / scalacOptions ++=
  Seq(
    "-deprecation",
    //"-explain",
    "-feature",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
//    "-Yexplicit-nulls", // experimental (I've seen it cause issues with circe)
    "-Ykind-projector",
//    "-Ysafe-init", // experimental (I've seen it cause issues with circe)
    "-Yretain-trees"
  ) ++ Seq("-rewrite", "-indent") ++ Seq("-source", "future-migration")


lazy val commonLibs = Seq(scalaUri, logBack, zioPrelude, jansi, zioConfMagnolia, zioConfTypesafe)
lazy val zioLibs = Seq(zio, zioHttp, zioJson, zioConcurrent, zioConfMagnolia, zioConfTypesafe)
lazy val testLibs = Seq(zioTest, zioTestSbt, zioJGolden).map(_ % "it, test")

lazy val velocypack =
  project
    .in(file("velocypack"))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(Seq(
      name := "zio-velocypack",
      libraryDependencies ++= commonLibs ++ Seq(scodecCore) ++ zioLibs ++ testLibs,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")))

lazy val arango =
  project
    .in(file("arangodb"))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(Seq(
      name := "zio-arangodb",
      libraryDependencies ++= commonLibs ++ zioLibs ++ testLibs,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")))
    .dependsOn(velocypack)

lazy val docker =
  project
    .in(file("arangodb-docker"))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(Seq(
      name := "arangodb-docker",
      libraryDependencies ++= Seq(testContainers, logBack) ++ zioLibs ++ testLibs),
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
    .dependsOn(arango)

lazy val http =
  project
    .in(file("arangodb-http"))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(Seq(
      name := "zio-arangodb-http",
      libraryDependencies ++= commonLibs ++ zioLibs ++ testLibs,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")),
      coverageExcludedFiles := ".*Main.*;zio\\.json\\.*")
    .dependsOn(arango, docker)

ThisBuild / coverageExcludedFiles := ".*Main.*;zio\\.json\\.*"

addCommandAlias("ll", "projects")
addCommandAlias("checkFmtAll", ";scalafmtSbtCheck;scalafmtCheckAll")
addCommandAlias("testAll", ";compile;test;stryker")
//addCommandAlias("sanity", ";compile;scalafmtAll;test;stryker")
addCommandAlias("sanity", ";clean;coverage;compile;scalafixAll;scalafmtAll;test;it:test;coverageAggregate;coverageOff")
