/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import zio.*
import zio.stream.*

import io.funkode.arangodb.protocol.ArangoMessage.POST
import model.*
import protocol.*
import ArangoMessage.*

class ArangoQuery[Encoder[_], Decoder[_]](
    databaseName: DatabaseName,
    query: Query,
    options: ArangoQuery.Options = ArangoQuery.Options()
)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  val database: DatabaseName = databaseName

  def withQuery(f: Query => Query): ArangoQuery[Encoder, Decoder] =
    new ArangoQuery(database, f(query), options)

  def batchSize(value: Long): ArangoQuery[Encoder, Decoder] = withQuery(_.copy(batchSize = Some(value)))

  def count(value: Boolean): ArangoQuery[Encoder, Decoder] = withQuery(_.copy(count = Some(value)))

  def transaction(id: TransactionId): ArangoQuery[Encoder, Decoder] =
    new ArangoQuery(database, query, options.copy(transaction = Some(id)))

  def execute[T](using Encoder[Query], Decoder[QueryResults[T]]): AIO[QueryResults[T]] =
    POST(
      database,
      ApiCursorPath,
      meta = Map(
        Transaction.Key -> options.transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(query).execute[QueryResults[T], Encoder, Decoder]

  def cursor[T](using
      Encoder[Query],
      Decoder[QueryResults[T]]
  ): AIO[ArangoCursor[Decoder, T]] =
    execute.map { resp =>
      ArangoCursor.apply[Encoder, Decoder, T](database, resp, options)
    }

  def stream[T](using Encoder[Query], Decoder[QueryResults[T]]): ZStream[Any, ArangoError, T] =
    ZStream.unwrap(
      for cursorResults <- cursor[T]
      yield ZStream.paginateChunkZIO(cursorResults) { cursor =>
        val results = Chunk.fromIterable(cursor.body.result)

        if cursor.body.hasMore then cursor.next.map(nextCursor => (results, Some(nextCursor)))
        else ZIO.succeed(results -> None)
      }
    )

object ArangoQuery:

  case class Options(transaction: Option[TransactionId] = None)

  extension [R, Enc[_], Dec[_]](queryService: ZIO[R, ArangoError, ArangoQuery[Enc, Dec]])
    def execute[T](using Enc[Query], Dec[QueryResults[T]]): ZIO[R, ArangoError, QueryResults[T]] =
      queryService.flatMap(_.execute[T])

    def cursor[T](using Enc[Query], Dec[QueryResults[T]]): ZIO[R, ArangoError, ArangoCursor[Dec, T]] =
      queryService.flatMap(_.cursor[T])

    def stream[T](using Enc[Query], Dec[QueryResults[T]]): ZStream[R, ArangoError, T] =
      ZStream.unwrap(queryService.map(_.stream[T]))
