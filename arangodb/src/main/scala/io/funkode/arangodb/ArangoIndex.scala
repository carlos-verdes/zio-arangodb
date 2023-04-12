/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import model.*
import protocol.*

class ArangoIndex[Encoder[_], Decoder[_]](
    databaseName: DatabaseName,
    collectionName: CollectionName,
    indexId: IndexId
)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  import ArangoMessage.*

  val database: DatabaseName = databaseName
  val collection: CollectionName = collectionName
  val id: IndexId = indexId
  val handle: IndexHandle = IndexHandle(collectionName, indexId)

  def info(using Decoder[IndexInfo]): AIO[IndexInfo] =
    GET(database, ApiIndexPath.addPart(handle.unwrap)).execute

  def drop(using Decoder[DeleteResult]): AIO[IndexHandle] =
    DELETE(database, ApiIndexPath.addPart(handle.unwrap)).execute.map(r => IndexHandle.parse(r.id).get)
