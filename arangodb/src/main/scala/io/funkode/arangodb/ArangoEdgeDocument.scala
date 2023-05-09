/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import model.*
import protocol.*
import ArangoMessage.*

class ArangoEdgeDocument[Encoder[_], Decoder[_]](
    database: DatabaseName,
    graph: GraphName,
    documentHandle: DocumentHandle
)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):
  def handle: DocumentHandle = documentHandle

  private val path = ApiGharialPath.addPart(graph.unwrap).addPart("edge").addParts(handle.path.parts)

  def read[T: Decoder](
      ifNoneMatch: Option[String] = None,
      ifMatch: Option[String] = None
  )(using Decoder[ArangoResult[GraphEdge[T]]]): AIO[T] =
    GET(
      database,
      path,
      meta = Map(
        "If-None-Match" -> ifNoneMatch,
        "If-Match" -> ifMatch
      ).collectDefined
    ).executeIgnoreResult[GraphEdge[T], Encoder, Decoder]
      .map(_.edge)

  def remove[T](
      waitForSync: Boolean = false,
      returnOld: Boolean = false,
      ifMatch: Option[String] = None,
      transaction: Option[TransactionId] = None
  )(using Decoder[EdgeDocumentDeleted[T]]): AIO[EdgeDocumentDeleted[T]] =
    DELETE(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "returnOld" -> returnOld.toString
      ),
      Map(
        "If-Match" -> ifMatch,
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).execute
