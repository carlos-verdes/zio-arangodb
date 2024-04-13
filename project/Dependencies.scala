import sbt._

object Dependencies {

  object Versions {

    val zioV = "2.0.22"
    val zioConfigV = "3.0.2"
    val zioHttpV = "3.0.0-RC2"
    val zioLoggingV = "2.1.0"
    val zioJsonV = "0.6.2"
    val zioConfMagnoliaV = "3.0.7"
    val zioConfTypesafeV = "3.0.7"
    val zioCryptoV = "0.0.1"
    val zioPreludeV = "1.0.0-RC21"
    val zioSchemaV = "0.4.14"

    val logBackV = "1.4.11"
    val scalaUriV = "4.0.3"
    val scodecV = "2.2.1"
    val testContainersV = "0.41.0"
    val jansiV = "2.4.0"
  }

  object Libraries {

    import Versions._

    val zio             = "dev.zio" %% "zio"                   % zioV
    val zioConcurrent   = "dev.zio" %% "zio-concurrent"        % zioV
    val zioConfMagnolia = "dev.zio" %% "zio-config-magnolia"   % zioConfMagnoliaV
    val zioConfTypesafe = "dev.zio" %% "zio-config-typesafe"   % zioConfTypesafeV
    val zioCrypto       = "dev.zio" %% "zio-crypto"            % zioCryptoV
    val zioHttp         = "dev.zio" %% "zio-http"              % zioHttpV
    val zioHttpTestKit  = "dev.zio" %% "zio-http-testkit"      % zioHttpV
    val zioJson         = "dev.zio" %% "zio-json"              % zioJsonV
    val zioJGolden      = "dev.zio" %% "zio-json-golden"       % zioJsonV
    val zioPrelude      = "dev.zio" %% "zio-prelude"           % zioPreludeV
    val zioSchema       = "dev.zio" %% "zio-schema"            % zioSchemaV
    val zioSchemaJson   = "dev.zio" %% "zio-schema-json"       % zioSchemaV
    val zioSchemaDeriv  = "dev.zio" %% "zio-schema-derivation" % zioSchemaV
    val zioStreams      = "dev.zio" %% "zio-streams"           % zioV
    val zioTest         = "dev.zio" %% "zio-test"              % zioV
    val zioTestSbt      = "dev.zio" %% "zio-test-sbt"          % zioV

    val logBack        = "ch.qos.logback"       % "logback-classic"            % logBackV
    val jansi          = "org.fusesource.jansi" % "jansi"                      % jansiV
    val testContainers = "com.dimafeng"         %% "testcontainers-scala-core" % testContainersV
    val scalaUri       = "io.lemonlabs"         %% "scala-uri"                 % scalaUriV
    val scodecBits     = "org.scodec"           %% "scodec-bits"               % scodecV
    val scodecCore     = "org.scodec"           %% "scodec-core"               % scodecV
  }
}
