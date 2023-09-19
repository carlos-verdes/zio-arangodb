/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import zio.stream.*

import io.funkode.velocypack.{VPack, VPackEncoder}
import io.funkode.velocypack.VPack.VObject
import model.*
import protocol.*
import ArangoMessage.*

class ArangoTransaction[Encoder[_], Decoder[_]](databaseName: DatabaseName, transactionId: TransactionId)(
    using arangoClient: ArangoClient[Encoder, Decoder]
):

  def database: DatabaseName = databaseName

  def id: TransactionId = transactionId

  private val path = ApiTransactionPath.addPart(id.unwrap)

  /** Fetch status of a server-side transaction
    */
  def status(using Decoder[ArangoResult[Transaction]]): AIO[Transaction] =
    GET(database, path).execute[ArangoResult[Transaction], Encoder, Decoder].map(_.result)

  /** Commit a running server-side transaction. Committing is an idempotent operation. It is not an error to
    * commit a transaction more than once.
    */
  def commit(using Decoder[ArangoResult[Transaction]]): AIO[Transaction] =
    PUT(database, path).execute[ArangoResult[Transaction], Encoder, Decoder].map(_.result)

  /** Abort a running server-side transaction. Aborting is an idempotent operation. It is not an error to
    * abort a transaction more than once.
    *
    * @return
    */
  def abort(using Decoder[ArangoResult[Transaction]]): AIO[Transaction] =
    DELETE(database, path).execute[ArangoResult[Transaction], Encoder, Decoder].map(_.result)
