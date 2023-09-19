/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

final case class TransactionCreate(
    collections: TransactionCreateCollections,
    waitForSync: Boolean,
    allowImplicit: Option[Boolean],
    lockTimeout: Option[Int],
    maxTransactionSize: Option[Long]
)

final case class TransactionCreateCollections(
    read: Option[List[CollectionName]],
    write: Option[List[CollectionName]],
    exclusive: Option[List[CollectionName]]
)
