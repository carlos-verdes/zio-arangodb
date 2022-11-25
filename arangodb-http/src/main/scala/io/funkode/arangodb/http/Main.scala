/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import zio.*
import zio.Console.*
import zio.config.ReadError
import zio.http.Client
import zio.http.Middleware.*
import zio.json.*

object Main extends ZIOAppDefault:

  import io.funkode.arangodb.model.*
  import io.funkode.arangodb.http.*
  import JsonCodecs.given

  case class Rel(_rel: String, _from: DocumentHandle, _to: DocumentHandle)
  given JsonCodec[Rel] = DeriveJsonCodec.gen[Rel]

  val testDb = DatabaseName("test")
  val politics = GraphName("politics")
  val allies = CollectionName("allies")
  val countries = CollectionName("countries")
  val graphEdgeDefinitions = List(GraphEdgeDefinition(allies, List(countries), List(countries)))
  val es = DocumentHandle(countries, DocumentKey("ES"))
  val fr = DocumentHandle(countries, DocumentKey("FR"))
  val us = DocumentHandle(countries, DocumentKey("US"))
  val alliesOfEs = List(Rel("ally", es, fr), Rel("ally", es, us), Rel("ally", us, fr))

  def app =
    for
      serverInfo <- ArangoClientJson.serverInfo().version()
      _ <- printLine(s"""Working with arango server"${serverInfo.server}: ${serverInfo.version}""")
      db <- ArangoClientJson.database(testDb).createIfNotExist()
      dbInfo <- db.info
      _ <- printLine(s"""Working with database "${dbInfo.name}" (id: ${dbInfo.id})""")
      alliesCol <- db.collection(allies).createIfNotExist()
      collections <- db.collections(true)
      _ <- printLine(s"""Database collections: ${collections.map(_.name).mkString(", ")}""")
      _ <- printLine(s"""Press any key to exit""")
      _ <- readLine
    yield ()

  def run =
    app.provide(
      Scope.default,
      Client.default,
      ArangoConfiguration.default,
      ArangoClientJson.live
    )
