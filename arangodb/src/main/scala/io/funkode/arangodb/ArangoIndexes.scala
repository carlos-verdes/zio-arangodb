/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import model.*
import protocol.*

class ArangoIndexes[Encoder[_], Decoder[_]](
    databaseName: DatabaseName,
    collectionName: CollectionName
)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  import ArangoMessage.*

  val database: DatabaseName = databaseName
  val collection: CollectionName = collectionName

  private val collectionParamName = "collection"

  def infos(using Decoder[IndexesInfo]): AIO[List[IndexInfo]] =
    GET(database, ApiIndexPath, Map(collectionParamName -> collectionName.unwrap)).execute.map(_.indexes)

  def create(options: IndexCreate)(using
      Encoder[IndexCreate],
      Decoder[IndexInfo]
  ): AIO[IndexInfo] =
    POST(
      database,
      ApiIndexPath,
      Map(
        collectionParamName -> collection.unwrap
      )
    ).withBody(options).execute
