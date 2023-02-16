/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package docker

import scala.language.adhocExtensions
import scala.util.Random

import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.GenericContainer.FileSystemBind
import io.netty.handler.codec.http.HttpHeaderNames
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import zio.*
import zio.http.*

import io.funkode.arangodb.model.*

class ArangoContainer(
    val password: String,
    val port: Int,
    val version: String,
    underlying: GenericContainer
) extends GenericContainer(underlying)

object ArangoContainer:

  def waitStrategy: HttpWaitStrategy =
    (new HttpWaitStrategy).nn.forPath("/_db/test/_api/collection/countries").nn

  def waitStrategyWithCredentials(password: String): HttpWaitStrategy =
    waitStrategy.withBasicCredentials("root", password).nn

  object Defaults:
    val port: Int = 8529
    val version: String = java.lang.System.getProperty("test.arangodb.version", "3.10.1").nn
    val password: String = Random.nextLong().toHexString

  // In the container definition you need to describe, how your container will be constructed:
  case class Def(
      password: String = Defaults.password,
      port: Int = Defaults.port,
      version: String = Defaults.version
  ) extends GenericContainer.Def[ArangoContainer](
        new ArangoContainer(
          password,
          port,
          version,
          GenericContainer(
            dockerImage = s"arangodb:$version",
            env = Map("ARANGO_ROOT_PASSWORD" -> password),
            exposedPorts = Seq(port),
            classpathResourceMapping = Seq(
              ("docker-initdb.d/", "/docker-entrypoint-initdb.d/", BindMode.READ_ONLY)
            ).map(FileSystemBind.apply),
            waitStrategy = waitStrategyWithCredentials(password)
          )
        )
      )

  def makeScopedContainer(config: ArangoConfiguration) =
    ZIO.acquireRelease(
      ZIO.attemptBlocking {
        val containerDef = ArangoContainer.Def(
          port = config.port,
          password = config.password
        )
        val container: ArangoContainer = containerDef.start()
        container
      }.orDie
    )(container =>
      ZIO.attemptBlocking {
        container.stop()
      }.ignoreLogged
    )

  def live[Encoder[_]: TagK, Decoder[_]: TagK]: ZLayer[
    ArangoConfiguration,
    Nothing,
    ArangoContainer
  ] =
    ZLayer.scopedEnvironment {
      for
        config <- ZIO.service[ArangoConfiguration]
        container <- makeScopedContainer(config)
      yield ZEnvironment(container)
    }
