/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import model.*
import protocol.*
import ArangoMessage.*

class ArangoGraphVertex[Encoder[_], Decoder[_]](
    database: DatabaseName,
    graph: GraphName,
    documentHandle: DocumentHandle
)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  def handle: DocumentHandle = documentHandle

  private val path = ApiGharialPath.addPart(graph.unwrap).addPart("vertex").addParts(handle.path.parts)

  def read[T: Decoder](
      ifNoneMatch: Option[String] = None,
      ifMatch: Option[String] = None
  )(using Decoder[ArangoResult[GraphVertex[T]]]): AIO[T] =
    GET(
      database,
      path,
      meta = Map(
        "If-None-Match" -> ifNoneMatch,
        "If-Match" -> ifMatch
      ).collectDefined
    ).executeIgnoreResult[GraphVertex[T], Encoder, Decoder]
      .map(_.vertex)
