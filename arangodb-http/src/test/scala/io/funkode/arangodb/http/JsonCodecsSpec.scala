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
import zio.test.Assertion.equalTo

import io.funkode.arangodb.model.{ArangoError, ArangoResponse}
import model.{ArangoRequestStatus, ArangoResponse, CollectionName, CollectionType, GraphCollections}
import JsonCodecs.given

object JsonCodecsSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Any] =
    suite("Json codecs should")(
      test("Encode/Decode collection type from json") {
        for
          unknownType <- ZIO.fromEither("0".fromJson[CollectionType])
          documentType <- ZIO.fromEither("2".fromJson[CollectionType])
          edgeType <- ZIO.fromEither("3".fromJson[CollectionType])
        yield assertTrue(unknownType == CollectionType.Unknown) &&
          assertTrue(documentType == CollectionType.Document) &&
          assertTrue(edgeType == CollectionType.Edge)
      },
      test("decode ArangoResponse") {
        import JsonCodecs.given_JsonCodec_GraphCollections
        import JsonCodecs.given_JsonCodec_ArangoRequestStatus
        import JsonCodecs.arangoResponse

        arangoResponse[ArangoResponse[GraphCollections]]

        val requestStatus = ArangoRequestStatus(false, 200)
        val collections = GraphCollections(List(CollectionName("hobbies"), CollectionName("tags")))
        val expected = ArangoResponse(requestStatus, collections)
        val result = """{"error":false,"code":200,"collections":["hobbies","tags"]}"""
          .fromJson[ArangoResponse[GraphCollections]]
        val requestStatusRes =
          """{"error":false,"code":200,"collections":["hobbies","tags"]}""".fromJson[ArangoRequestStatus]
        val collectionsRes =
          """{"error":false,"code":200,"collections":["hobbies","tags"]}""".fromJson[GraphCollections]
        assertTrue(collectionsRes == Right(collections)) && assertTrue(
          requestStatusRes == Right(requestStatus)
        )
        && assertTrue(result == Right(expected))
      },
      test("decode ArangoError") {
        val expected = ArangoError(400, true, "Error happened", -5)
        val result =
          "{\"code\": 400, \"error\": true, \"errorMessage\": \"Error happened\", \"errorNum\": -5}"
            .fromJson[ArangoError]
        assertTrue(result == Right(expected))
      }
    )
