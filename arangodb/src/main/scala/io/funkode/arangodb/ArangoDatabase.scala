/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import io.lemonlabs.uri.UrlPath
import io.lemonlabs.uri.typesafe.dsl.*
import zio.*

import io.funkode.arangodb.protocol.ArangoMessage.{GET, POST}
import io.funkode.velocypack.VPack
import io.funkode.velocypack.VPack.*
import model.*
import protocol.*
import ArangoMessage.*

class ArangoDatabase[Encoder[_], Decoder[_]](databaseName: DatabaseName)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  val name: DatabaseName = databaseName

  def info(using Decoder[ArangoResult[DatabaseInfo]]): AIO[DatabaseInfo] =
    GET(name, ApiDatabase.addPart("current")).executeIgnoreResult

  def create(users: List[DatabaseCreate.User] = List.empty)(using
      Encoder[DatabaseCreate],
      Decoder[ArangoResult[Boolean]]
  ): AIO[ArangoDatabase[Encoder, Decoder]] =
    POST(DatabaseName.system, ApiDatabase)
      .withBody(DatabaseCreate(name, users))
      .executeIgnoreResult
      .map(_ => this)

  def createIfNotExist(users: List[DatabaseCreate.User] = List.empty)(using
      Encoder[DatabaseCreate],
      Decoder[ArangoResult[Boolean]]
  ): AIO[ArangoDatabase[Encoder, Decoder]] =
    create(users).ifConflict(ZIO.succeed(this))

  def drop(using Decoder[ArangoResult[Boolean]]): AIO[Boolean] =
    DELETE(DatabaseName.system, ApiDatabase.addPart(name.unwrap)).executeIgnoreResult

  def collections(excludeSystem: Boolean = false)(using
      Decoder[ArangoResult[List[CollectionInfo]]]
  ): AIO[List[CollectionInfo]] =
    GET(name, ApiCollectionPath, Map("excludeSystem" -> excludeSystem.toString)).executeIgnoreResult

  def graphs(using Decoder[ArangoResult[GraphList]]): AIO[List[GraphInfo]] =
    GET(name, ApiGharialPath).executeIgnoreResult[GraphList, Encoder, Decoder].map(_.graphs)

  def collection(name: CollectionName): ArangoCollection[Encoder, Decoder] =
    new ArangoCollection[Encoder, Decoder](this.name, name)

  def document(handle: DocumentHandle): ArangoDocument[Encoder, Decoder] =
    new ArangoDocument[Encoder, Decoder](name, handle)

  def graph(graphName: GraphName): ArangoGraph[Encoder, Decoder] =
    new ArangoGraph[Encoder, Decoder](this.name, graphName)

  def transactions: ArangoTransactions[Encoder, Decoder] = new ArangoTransactions[Encoder, Decoder](this.name)

  def query(query: Query): ArangoQuery[Encoder, Decoder] = new ArangoQuery(name, query)

  def query(qs: String, bindVars: VObject): ArangoQuery[Encoder, Decoder] = query(Query(qs, bindVars))

  def query(qs: String): ArangoQuery[Encoder, Decoder] = query(qs, VObject.empty)

/*
def wal: ArangoWal[F]
 */

object ArangoDatabase:

  extension [R, Enc[_], Dec[_]](dbService: ZIO[R, ArangoError, ArangoDatabase[Enc, Dec]])

    def create(users: List[DatabaseCreate.User] = List.empty)(using
        Enc[DatabaseCreate],
        Dec[ArangoResult[Boolean]]
    ): ZIO[R, ArangoError, ArangoDatabase[Enc, Dec]] =
      dbService.flatMap(_.create(users))

    def createIfNotExist(users: List[DatabaseCreate.User] = List.empty)(using
        Enc[DatabaseCreate],
        Dec[ArangoResult[Boolean]]
    ): ZIO[R, ArangoError, ArangoDatabase[Enc, Dec]] =
      dbService.flatMap(_.createIfNotExist(users))

    def drop(using Dec[ArangoResult[Boolean]]): ZIO[R, ArangoError, Boolean] =
      dbService.flatMap(_.drop)

    def collection(collectionName: CollectionName): ZIO[R, ArangoError, ArangoCollection[Enc, Dec]] =
      dbService.map(_.collection(collectionName))

    def query(query: Query): ZIO[R, ArangoError, ArangoQuery[Enc, Dec]] = dbService.map(_.query(query))
