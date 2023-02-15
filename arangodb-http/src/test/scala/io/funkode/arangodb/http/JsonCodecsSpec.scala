/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import zio.*
import zio.json.*
import zio.test.*

import model.CollectionType

object JsonCodecsSpec extends ZIOSpecDefault:

  import JsonCodecs.given

  override def spec: Spec[TestEnvironment, Any] =
    suite("Schema codecs should")(
      test("Encode/Decode collection type from json") {
        for
          unknownType <- ZIO.fromEither("0".fromJson[CollectionType])
          documentType <- ZIO.fromEither("2".fromJson[CollectionType])
          edgeType <- ZIO.fromEither("3".fromJson[CollectionType])
        yield assertTrue(unknownType == CollectionType.Unknown) &&
          assertTrue(documentType == CollectionType.Document) &&
          assertTrue(edgeType == CollectionType.Edge)
      }
    )
