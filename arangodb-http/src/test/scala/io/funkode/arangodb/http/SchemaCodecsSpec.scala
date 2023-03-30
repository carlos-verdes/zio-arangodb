/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import zio.*
import zio.schema.*
import zio.schema.codec.*
import zio.test.*

import model.*

trait SchemaExamples:

  case class Person(name: String, age: Int)

  given Schema[Person] = DeriveSchema.gen[Person]

object SchemaCodecsSpec extends ZIOSpecDefault with SchemaExamples:

  import SchemaCodecs.given

  override def spec: Spec[TestEnvironment, Any] =
    suite("Schema codecs should")(
      test("Decode ArangoResult from json using schema") {
        for
          _ <- ZIO.unit
          json = """{"error": true, "code": 300, "result": { "name": "Peter", "age": 19}}"""
          arangoResultPerson <- ZIO.fromEither(
            JsonCodec.jsonDecoder[ArangoResult[Person]](arangoResultSchema[Person]).decodeJson(json)
          )
        yield assertTrue(
          arangoResultPerson == ArangoResult(true, 300, Person("Peter", 19))
        )
      },
      test("Decode ServerInfo from json using schema") {
        for
          _ <- ZIO.unit
          simpleServerInfoJson = """{ "server": "arango", "license": "community", "version": "3.10.1"}"""
          fullServerInfoJson =
            """{
              |"server": "arango",
              |"license": "community",
              |"version": "3.10.1",
              |"details": {
              |  "architecture": "64bit",
              |  "mode": "server"
              |}}""".stripMargin
          simpleServerInfo <- ZIO.fromEither(
            JsonCodec.jsonDecoder[ServerVersion](given_Schema_ServerVersion).decodeJson(simpleServerInfoJson)
          ) /*
          TODO waiting for PR to be merged https://github.com/zio/zio-schema/pull/534
          fullServerInfo <- ZIO.fromEither(
            JsonCodec.jsonDecoder[ServerVersion](given_Schema_ServerVersion).decodeJson(fullServerInfoJson)
          )*/
        yield assertTrue(
          simpleServerInfo == ServerVersion("arango", "community", "3.10.1")
        ) /*
         TODO waiting for PR to be merged https://github.com/zio/zio-schema/pull/534
        &&
          assertTrue(
            fullServerInfo ==
              ServerVersion(
                  "arango",
                  "community",
                  "3.10.1",
                  Map("architecture" -> "64bit", "mode" -> "server"))
        )*/
      }
    )
