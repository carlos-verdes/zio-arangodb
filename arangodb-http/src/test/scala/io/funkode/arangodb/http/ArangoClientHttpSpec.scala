/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.http

import zio.*
import zio.http.{Body, Request, Response, TestClient}
import zio.http.model.*
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.test.*
import zio.test.Assertion.*
import io.funkode.arangodb.{AIO, ArangoConfiguration}
import io.funkode.arangodb.http.JsonCodecs.{given_JsonCodec_CollectionName, given_JsonCodec_CollectionStatus, given_JsonCodec_CollectionType}
import io.funkode.arangodb.model.*

object ArangoClientHttpSpec extends ZIOSpecDefault:

  given JsonCodec[CollectionInfo] = DeriveJsonCodec.gen[CollectionInfo]

  override def spec: Spec[TestEnvironment, Any] =
    suite("Arango http client should")(
      test("Correctly handle empty response from the server instead of the expected one") {
        val response: PartialFunction[Request, ZIO[Any, Throwable, Response]] = { case _ =>
          ZIO.succeed(Response.ok)
        }
        val assertion = assertError(
          ArangoError(
            500,
            true,
            "Empty body in Arango response",
            -1
          )
        )
        errorTestCase(response, assertion)
      },
      test("Correctly handle error response from the server that matches ArangoError structure") {
        val response: PartialFunction[Request, ZIO[Any, Throwable, Response]] = { case _ =>
           ZIO.succeed(Response(Status.BadRequest, body = Body.fromString("{\"code\": 400, \"error\": true, \"errorMessage\": \"Error happened\", \"errorNum\": -5}")))
        }
        val assertion = assertError(
          ArangoError(
            400,
            true,
            "Error happened",
            -5
          )
        )
        errorTestCase(response, assertion)
      },
      test("Correctly handle unexpected error response from the server instead of the expected one") {
        val response: PartialFunction[Request, ZIO[Any, Throwable, Response]] = {
          case _ =>
            ZIO.succeed(Response(Status.Conflict, body = Body.fromString("{\"error\": true}")))
        }
        val assertion = assertError(
          ArangoError(
            409,
            true,
            s"Incorrect status from server: 409. Body: {\"error\": true}",
            -1
          )
        )
        errorTestCase(response, assertion)
      },
      test("Correctly handle unexpected Json response from the server") {
        val response: PartialFunction[Request, ZIO[Any, Throwable, Response]] = { case _ =>
          ZIO.succeed(Response.json("""{ "test": "value" }"""))
        }
        val assertion = assertError(
          ArangoError(
            500,
            true,
            "Error parsing JSON Arango response: .id(missing)\nBody: { \"test\": \"value\" }",
            -1
          )
        )
        errorTestCase(response, assertion)
      }
    )

  private def assertError(arangoError: ArangoError)(collectionInfo: AIO[CollectionInfo]) =
    assertZIO(collectionInfo.exit)(fails[ArangoError](equalTo(arangoError)))

  private def errorTestCase(
      response: PartialFunction[Request, ZIO[Any, Throwable, Response]],
      assertion: AIO[CollectionInfo] => UIO[TestResult]
  ): UIO[TestResult] =
    val authPartialFunction: PartialFunction[Request, ZIO[Any, Throwable, Response]] =
      case request if request.url.encode.contains("auth") =>
        ZIO.succeed(Response.json("""{ "jwt": "eyJhbGciOiJIUzI1NiIx6EfI" }"""))
    val httpClientWithAuth =
      ZIO
        .service[TestClient]
        .tap(c => c.addHandler(authPartialFunction.orElse(response)))
        .provide(TestClient.layer)

    val request: AIO[CollectionInfo] =
      ZIO
        .service[ArangoClientJson]
        .flatMap(client => client.database(DatabaseName("test")).collection(CollectionName("coll")).info)
        .provide(
          ZLayer(httpClientWithAuth),
          ArangoClientJson.live,
          ZLayer.succeed(ArangoConfiguration(host = "localhost", password = "", username = ""))
        )
    assertion(request)
