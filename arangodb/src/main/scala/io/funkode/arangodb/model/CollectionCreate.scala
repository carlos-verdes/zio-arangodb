/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

final case class CollectionCreate(
    name: CollectionName,
    distributeShardsLike: String = "",
    doCompact: Boolean = true,
    indexBuckets: Option[Long] = None,
    isSystem: Boolean = false,
    isVolatile: Boolean = false,
    journalSize: Option[Long] = None,
    keyOptions: Option[CollectionCreate.KeyOptions] = None,
    numberOfShards: Long = 1,
    replicationFactor: Long = 1,
    shardKeys: List[String] = List(DocumentKey.Key),
    shardingStrategy: Option[String] = None,
    smartJoinAttribute: Option[String] = None,
    `type`: CollectionType = CollectionType.Document,
    waitForSync: Boolean = false,
    waitForSyncReplication: Int = 1,
    enforceReplicationFactor: Int = 1
//  schema: Option[CollectionSchema] = None,
)

object CollectionCreate:

  extension (create: CollectionCreate)
    def parameters = Map(
      "waitForSyncReplication" -> create.waitForSyncReplication.toString,
      "enforceReplicationFactor" -> create.enforceReplicationFactor.toString
    )

  final case class KeyOptions(
      allowUserKeys: Option[Boolean] = None,
      increment: Option[Long] = None,
      offset: Option[Long] = None,
      `type`: Option[String] = None
  )
