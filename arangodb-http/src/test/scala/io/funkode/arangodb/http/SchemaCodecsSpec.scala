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
      test("Encode/Decode ArangoResult from json using schema") {
        for
          _ <- ZIO.unit
          json = """{"error": true, "code": 300, "result": { "name": "Peter", "age": 19}}"""
          chunks = Chunk.fromArray(json.getBytes)
          arangoResultPerson <- ZIO.fromEither(
            JsonCodec.decode[ArangoResult[Person]](arangoResultSchema[Person])(chunks)
          )
        yield assertTrue(
          arangoResultPerson == ArangoResult(true, 300, Person("Peter", 19))
        )
      },
      test("Encode/Decode ServerInfo from json using schema") {
        for
          _ <- ZIO.unit
          json = """{ "server": "arango", "license": "community", "version": "3.10.1"}"""
          chunks = Chunk.fromArray(json.getBytes)
          serverInfo <- ZIO.fromEither(
            JsonCodec.decode[ServerVersion](given_Schema_ServerVersion)(chunks)
          )
        yield assertTrue(
          serverInfo == ServerVersion("arango", "community", "3.10.1")
        )
      }
    ) @@ TestAspect.ignore
