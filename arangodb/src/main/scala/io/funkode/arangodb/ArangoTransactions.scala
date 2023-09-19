/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import zio.stream.*

import io.funkode.arangodb.model.*
import io.funkode.arangodb.protocol.*
import io.funkode.arangodb.protocol.ArangoMessage.*
import io.funkode.velocypack.{VPack, VPackEncoder}
import io.funkode.velocypack.VPack.VObject

class ArangoTransactions[Encoder[_], Decoder[_]](databaseName: DatabaseName)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  def database: DatabaseName = databaseName

  private val path = ApiTransactionPath
  private val beginPath = ApiBeginTransactionPath

  /** Return the currently running server-side transactions
    *
    * @return
    *   an object with the attribute transactions, which contains an array of transactions. In a cluster the
    *   array will contain the transactions from all Coordinators.
    */
  def list(using Decoder[TransactionList]): AIO[TransactionList] =
    GET(database, path).execute[TransactionList, Encoder, Decoder]

  /** begin a server-side transaction
    *
    * Collections that will be written to in the transaction must be declared with the write or exclusive
    * attribute or it will fail, whereas non-declared collections from which is solely read will be added
    * lazily. See locking and isolation for more information.
    *
    * @param read
    *   collections read
    * @param write
    *   collections write
    * @param exclusive
    *   collections exclusive
    * @param waitForSync
    *   an optional boolean flag that, if set, will force the transaction to write all data to disk before
    *   returning
    * @param allowImplicit
    *   Allow reading from undeclared collections.
    * @param lockTimeout
    *   an optional numeric value that can be used to set a timeout for waiting on collection locks. If not
    *   specified, a default value will be used. Setting lockTimeout to 0 will make ArangoDB not time out
    *   waiting for a lock.
    * @param maxTransactionSize
    *   Transaction size limit in bytes. Honored by the RocksDB storage engine only.
    * @return
    *   transaction api
    */
  def begin(
      read: List[CollectionName] = List.empty,
      write: List[CollectionName] = List.empty,
      exclusive: List[CollectionName] = List.empty,
      waitForSync: Boolean = false,
      allowImplicit: Option[Boolean] = None,
      lockTimeout: Option[Int] = None,
      maxTransactionSize: Option[Long] = None
  )(using
      Encoder[TransactionCreate],
      Decoder[ArangoResult[Transaction]]
  ): AIO[ArangoTransaction[Encoder, Decoder]] =
    POST(database, beginPath)
      .withBody(
        TransactionCreate(
          TransactionCreateCollections(
            read = if read.isEmpty then None else Some(read),
            write = if write.isEmpty then None else Some(write),
            exclusive = if exclusive.isEmpty then None else Some(exclusive)
          ),
          waitForSync,
          allowImplicit,
          lockTimeout,
          maxTransactionSize
        )
      )
      .execute[ArangoResult[Transaction], Encoder, Decoder]
      .map { t =>
        ArangoTransaction(database, t.result.id)
      }
