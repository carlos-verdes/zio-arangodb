/*
 * TODO: License goes here!
 */
package io.funkode.arangodb

import protocol.*
import model.*

trait ArangoCursor[Decoder[_], T]:
  // def header: ArangoMessage.Header
  def body: QueryResults[T]
  def next(using Decoder[QueryResults[T]]): AIO[ArangoCursor[Decoder, T]]
  def delete(using Decoder[DeleteResult]): AIO[DeleteResult]

object ArangoCursor:

  import ArangoMessage.*

  def apply[Encoder[_], Decoder[_], T](
      database: DatabaseName,
      cursor: QueryResults[T],
      options: ArangoQuery.Options
  )(using
      ArangoClient[Encoder, Decoder]
  ): ArangoCursor[Decoder, T] = new ArangoCursor[Decoder, T]:
    // def header: ArangoMessage.Header

    def body: QueryResults[T] = cursor

    def next(using Decoder[QueryResults[T]]): AIO[ArangoCursor[Decoder, T]] =
      val op = PUT(
        database,
        ApiCursorPath.addPart(body.id.get),
        meta = Map(
          Transaction.Key -> options.transaction.map(_.unwrap)
        ).collectDefined
      )

      op
        .execute[QueryResults[T], Encoder, Decoder]
        .map(cursor => apply(database, cursor, options))

    def delete(using Decoder[DeleteResult]): AIO[DeleteResult] =
      DELETE(
        database,
        ApiCursorPath.addPart(body.id.get),
        meta = Map(
          Transaction.Key -> options.transaction.map(_.unwrap)
        ).collectDefined
      ).execute[DeleteResult, Encoder, Decoder]
