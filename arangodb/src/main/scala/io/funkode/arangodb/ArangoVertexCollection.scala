/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import zio.*

import io.funkode.arangodb.model.*
import io.funkode.arangodb.protocol.*

class ArangoVertexCollection[Encoder[_], Decoder[_]](
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
  val path = ApiGharialPath.addPart(graph.unwrap).addPart("vertex").addPart(name.unwrap)

  def createVertexDocument[T](
      document: T,
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      transaction: Option[TransactionId] = None
  )(using
      Encoder[T],
      Decoder[VertexDocumentCreated[T]]
  ): AIO[VertexDocumentCreated[T]] =
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
    ).withBody(document).execute[VertexDocumentCreated[T], Encoder, Decoder]
