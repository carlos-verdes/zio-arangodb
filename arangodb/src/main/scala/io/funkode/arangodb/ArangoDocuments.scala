/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import zio.ZIO
import zio.stream.*

import model.*
import protocol.*
import ArangoMessage.*

class ArangoDocuments[Encoder[_], Decoder[_]](databaseName: DatabaseName, collectionName: CollectionName)(
    using arangoClient: ArangoClient[Encoder, Decoder]
):

  def database: DatabaseName = databaseName

  def collection = collectionName

  val path = ApiDocumentPath.addPart(collection.unwrap)

  def count(transactionId: Option[TransactionId] = None)(using
      Decoder[CollectionCount]
  ): AIO[Long] =
    GET(
      database,
      ApiCollectionPath.addPart(collection.unwrap).addPart("count"),
      meta = Map(
        Transaction.Key -> transactionId.map(_.unwrap)
      ).collectDefined
    ).execute.map(_.count)

  def insertRaw(
      documentStream: Stream[Throwable, Byte],
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      returnOld: Boolean = false,
      silent: Boolean = false,
      overwrite: Boolean = false,
      transaction: Option[TransactionId] = None
  ): AIO[Stream[Throwable, Byte]] =
    POST(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "returnNew" -> returnNew.toString,
        "returnOld" -> returnOld.toString,
        "silent" -> silent.toString,
        "overwrite" -> overwrite.toString
      ),
      Map(
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(documentStream).executeRaw[Encoder, Decoder]

  def insert[T](
      document: T,
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      returnOld: Boolean = false,
      silent: Boolean = false,
      overwrite: Boolean = false,
      transaction: Option[TransactionId] = None
  )(using
      Encoder[T],
      Decoder[Document[T]]
  ): AIO[Document[T]] =
    POST(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "returnNew" -> returnNew.toString,
        "returnOld" -> returnOld.toString,
        "silent" -> silent.toString,
        "overwrite" -> overwrite.toString
      ),
      Map(
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(document).execute[Document[T], Encoder, Decoder]

  def create[T](
      documents: List[T],
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      returnOld: Boolean = false,
      silent: Boolean = false,
      overwrite: Boolean = false,
      transaction: Option[TransactionId] = None
  )(using
      Encoder[List[T]],
      Decoder[List[Document[T]]]
  ): AIO[List[Document[T]]] =
    POST(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "returnNew" -> returnNew.toString,
        "returnOld" -> returnOld.toString,
        "silent" -> silent.toString,
        "overwrite" -> overwrite.toString
      ),
      Map(
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(documents).execute

  def replace[T](
      documents: List[T],
      waitForSync: Boolean = false,
      ignoreRevs: Boolean = true,
      returnOld: Boolean = false,
      returnNew: Boolean = false,
      transaction: Option[TransactionId] = None
  )(using
      Encoder[List[T]],
      Decoder[List[Document[T]]]
  ): AIO[List[Document[T]]] =
    PUT(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "ignoreRevs" -> ignoreRevs.toString,
        "returnOld" -> returnOld.toString,
        "returnNew" -> returnNew.toString
      ),
      Map(
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(documents).execute

  def update[T, P](
      patch: List[P],
      keepNull: Boolean = false,
      mergeObjects: Boolean = true,
      waitForSync: Boolean = false,
      ignoreRevs: Boolean = true,
      returnOld: Boolean = false,
      returnNew: Boolean = false,
      transaction: Option[TransactionId] = None
  )(using
      Encoder[List[P]],
      Decoder[List[Document[T]]]
  ): AIO[List[Document[T]]] =
    PATCH(
      database,
      path,
      Map(
        "keepNull" -> keepNull.toString,
        "mergeObjects" -> mergeObjects.toString,
        "waitForSync" -> waitForSync.toString,
        "ignoreRevs" -> ignoreRevs.toString,
        "returnOld" -> returnOld.toString,
        "returnNew" -> returnNew.toString
      ),
      Map(
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(patch).execute

  def remove[T, K](
      keys: List[K],
      waitForSync: Boolean = false,
      returnOld: Boolean = false,
      ignoreRevs: Boolean = true,
      transaction: Option[TransactionId] = None
  )(using
      Encoder[List[K]],
      Decoder[List[Document[T]]]
  ): AIO[List[Document[T]]] =
    DELETE(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "returnOld" -> returnOld.toString,
        "ignoreRevs" -> ignoreRevs.toString
      ),
      Map(
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(keys).execute

object ArangoDocuments:

  extension [R, Enc[_], Dec[_]](docsService: ZIO[R, ArangoError, ArangoDocuments[Enc, Dec]])
    def create[T](
        documents: List[T],
        waitForSync: Boolean = false,
        returnNew: Boolean = false,
        returnOld: Boolean = false,
        silent: Boolean = false,
        overwrite: Boolean = false,
        transaction: Option[TransactionId] = None
    )(using
        Enc[List[T]],
        Dec[List[Document[T]]]
    ): ZIO[R, ArangoError, List[Document[T]]] =
      docsService.flatMap(
        _.create(documents, waitForSync, returnNew, returnOld, silent, overwrite, transaction)
      )
