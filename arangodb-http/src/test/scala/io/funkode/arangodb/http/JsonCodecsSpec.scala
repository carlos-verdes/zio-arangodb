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

import JsonCodecs.given
import model.*

object JsonCodecsSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Any] =
    suite("Json codecs should")(
      test("Decode index info from json") {
        for
          edge <- ZIO.fromEither(
            """{"type":"edge","id":"cName/123","name":"iName"}"""
              .fromJson[IndexInfo]
          )
          fulltext <- ZIO.fromEither(
            """{"type":"fulltext","id":"cName/123","name":"iName"}"""
              .fromJson[IndexInfo]
          )
          geoLocation <- ZIO.fromEither(
            """{"type":"geo","id":"cName/123","name":"iName","fields":["loc"],"geoJson":true}"""
              .fromJson[IndexInfo]
          )
          geoLatLong <- ZIO.fromEither(
            """{"type":"geo","id":"cName/123","name":"iName","fields":["lat","long"],"geoJson":false}"""
              .fromJson[IndexInfo]
          )
          inverted <- ZIO.fromEither(
            """{"type":"inverted","id":"cName/123","name":"iName"}"""
              .fromJson[IndexInfo]
          )
          zkd <- ZIO.fromEither(
            """{"type":"zkd","id":"cName/123","name":"iName"}"""
              .fromJson[IndexInfo]
          )
          persistent <- ZIO.fromEither(
            """{"type":"persistent","id":"cName/123","name":"iName"}"""
              .fromJson[IndexInfo]
          )
          primary <- ZIO.fromEither(
            """{"type":"primary","id":"cName/123","name":"iName"}"""
              .fromJson[IndexInfo]
          )
          ttl <- ZIO.fromEither(
            """{"type":"ttl","id":"cName/123","name":"iName"}"""
              .fromJson[IndexInfo]
          )
        yield assertTrue(
          edge == IndexInfo.Edge(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName")
          )
        ) && assertTrue(
          fulltext == IndexInfo.Fulltext(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName")
          )
        ) && assertTrue(
          geoLocation == IndexInfo.Geo(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName"),
            IndexGeoFields.Location("loc"),
            true
          )
        ) && assertTrue(
          geoLatLong == IndexInfo.Geo(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName"),
            IndexGeoFields.LatLong("lat", "long"),
            false
          )
        ) && assertTrue(
          inverted == IndexInfo.Inverted(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName")
          )
        ) && assertTrue(
          zkd == IndexInfo.MultiDimensional(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName")
          )
        ) && assertTrue(
          persistent == IndexInfo.Persistent(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName")
          )
        ) && assertTrue(
          primary == IndexInfo.Primary(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName")
          )
        ) && assertTrue(
          ttl == IndexInfo.TimeToLive(
            IndexHandle(CollectionName("cName"), IndexId("123")),
            IndexName("iName")
          )
        )
      },
      test("Encode index creation data to json") {
        val geoLocation = IndexCreate.Geo
          .location(IndexName("iName"), "loc", Some(true), Some(true))
          .asInstanceOf[IndexCreate]
          .toJson
        val geoLatLong = IndexCreate.Geo
          .latLong(IndexName("iName"), "lat", "long", Some(false))
          .asInstanceOf[IndexCreate]
          .toJson
        assertTrue(
          geoLocation == """{"type":"geo","name":"iName","fields":["loc"],"geoJson":true,"inBackground":true}"""
        ) &&
        assertTrue(
          geoLatLong == """{"type":"geo","name":"iName","fields":["lat","long"],"inBackground":false}"""
        )
      },
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
