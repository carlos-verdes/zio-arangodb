/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package docker

import zio.*
import zio.test.*

object ArangoDbClientIT extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Any] =
    suite("ArangoDB container should")(test("Start container with ArangoDB") {
      for container <- ZIO.service[ArangoContainer]
      yield assertTrue(container.port == ArangoConfiguration.DefaultPort)
    }).provideSome(ArangoConfiguration.default, ArangoContainer.live)
