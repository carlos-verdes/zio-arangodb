/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import zio.*

import io.funkode.arangodb.model.*
import io.funkode.arangodb.protocol.*

class ArangoEdgeCollection[Encoder[_], Decoder[_]](
    databaseName: DatabaseName,
    graphName: GraphName,
    collectionName: CollectionName
)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  import ArangoMessage.*

  val database: DatabaseName = databaseName
  val graph: GraphName = graphName
  val name: CollectionName = collectionName
  val path = ApiGharialPath.addPart(graph.unwrap).addPart("edge").addPart(name.unwrap)

  def createEdgeDocument[T](
      edgeDocument: T,
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      transaction: Option[TransactionId] = None
  )(using
      Encoder[T],
      Decoder[EdgeDocumentCreated[T]]
  ): AIO[EdgeDocumentCreated[T]] =
    POST(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "returnNew" -> returnNew.toString
      ),
      Map(
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(edgeDocument).execute[EdgeDocumentCreated[T], Encoder, Decoder]
