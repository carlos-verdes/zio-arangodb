import Dependencies._
import Libraries._

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / homepage := Some(url("https://github.com/carlos-verdes/zio-arangodb"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/carlos-verdes/zio-arangodb"), "git@github.com:carlos-verdes/zio-arangodb.git"))
ThisBuild / developers := List(Developer("carlos-verdes", "Carlos Verdes", "cverdes@gmail.com", url("https://github.com/carlos-verdes")))
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("releases")

// release plugin
//import ReleaseTransformations._
//import xerial.sbt.Sonatype._

ThisBuild / releaseCrossBuild := true
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value

Test / fork := true
IntegrationTest / fork := true

inThisBuild(
  Seq(
    organization := "io.funkode",
    scalaVersion :="3.3.3",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0",
    publishTo := sonatypePublishToBundle.value,
    sonatypeCredentialHost :="s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    startYear := Some(2022),
    licenses += ("MIT", new URL("https://opensource.org/licenses/MIT")),
))

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
lazy val zioSchemaLibs = Seq(zioSchemaJson, zioSchemaDeriv)
lazy val zioLibs = Seq(zio, zioHttp, zioJson, zioConcurrent, zioConfMagnolia, zioConfTypesafe) ++ zioSchemaLibs
lazy val testLibs = Seq(zioTest, zioTestSbt, zioJGolden, zioHttpTestKit).map(_ % "it, test")

lazy val velocypack =
  project
    .in(file("velocypack"))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(headerSettings(Test, IntegrationTest))
    .settings(Seq(
      name := "zio-velocypack",
      libraryDependencies ++= commonLibs ++ Seq(scodecCore) ++ zioLibs ++ testLibs,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")),
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      headerLicense := Some(HeaderLicense.MIT("2022", "Carlos Verdes", HeaderLicenseStyle.SpdxSyntax)),
      headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax)
    .enablePlugins(AutomateHeaderPlugin)

lazy val arango =
  project
    .in(file("arangodb"))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(headerSettings(Test, IntegrationTest))
    .settings(Seq(
      name := "zio-arangodb",
      libraryDependencies ++= commonLibs ++ zioLibs ++ testLibs,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      headerLicense := Some(HeaderLicense.MIT("2022", "Carlos Verdes", HeaderLicenseStyle.SpdxSyntax)),
      headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax))
    .dependsOn(velocypack)
    .enablePlugins(AutomateHeaderPlugin)

lazy val docker =
  project
    .in(file("arangodb-docker"))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(headerSettings(Test, IntegrationTest))
    .settings(Seq(
      name := "arangodb-docker",
      libraryDependencies ++= Seq(testContainers, logBack) ++ zioLibs ++ testLibs,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      headerLicense := Some(HeaderLicense.MIT("2022", "Carlos Verdes", HeaderLicenseStyle.SpdxSyntax)),
      headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax))
    .dependsOn(arango)
    .enablePlugins(AutomateHeaderPlugin)

lazy val http =
  project
    .in(file("arangodb-http"))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(headerSettings(Test, IntegrationTest))
    .settings(Seq(
      name := "zio-arangodb-http",
      libraryDependencies ++= commonLibs ++ zioLibs ++ testLibs,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      coverageExcludedPackages := """io.funkode.arangodb.http.Main; io.funkode.*.autoDerive; zio.json.*;""",
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      headerLicense := Some(HeaderLicense.MIT("2022", "Carlos Verdes", HeaderLicenseStyle.SpdxSyntax)),
      headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax))
    .dependsOn(arango, docker)
    .enablePlugins(AutomateHeaderPlugin)

lazy val root =
  project
    .in(file("."))
    .aggregate(arango, http, docker, velocypack)
    .settings(
      publishArtifact := false,
      publish / skip := true)

ThisBuild / coverageExcludedFiles := ".*Main.*;zio\\.json\\.*"

addCommandAlias("ll", "projects")
addCommandAlias("checkFmtAll", ";scalafmtSbtCheck;scalafmtCheckAll")
addCommandAlias("testAll", ";compile;test;stryker")
//addCommandAlias("sanity", ";compile;scalafmtAll;test;stryker")
addCommandAlias("sanity", ";clean;coverage;compile;headerCreate;scalafixAll;scalafmtAll;test;it:test;coverageAggregate;coverageOff")

ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
