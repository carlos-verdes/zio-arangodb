/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

import io.funkode.velocypack.*

case class Query(
    query: String,
    bindVars: Option[VPack.VObject],
    batchSize: Option[Long] = None,
    cache: Option[Boolean] = None,
    count: Option[Boolean] = None,
    memoryLimit: Option[Long] = None,
    options: Option[Query.Options] = None,
    ttl: Option[Long] = None
)

object Query:

  def apply(query: String): Query = new Query(query, None)
  def apply(query: String, bindVars: VPack.VObject): Query = new Query(query, Some(bindVars))

  extension (q: Query)
    def bindVar(key: String, value: VPack): Query =

      import VPack.*
      import VObject.updated

      val newBindVars: VPack.VObject =
        q.bindVars.map(_.updated(key, value)).getOrElse(VObject(Map(key -> value)))
      q.copy(bindVars = Some(newBindVars))

  final case class Options(
      failOnWarning: Option[Boolean],
      fullCount: Option[Boolean],
      intermediateCommitCount: Option[Long],
      intermediateCommitSize: Option[Long],
      maxPlans: Option[Long],
      maxTransactionSize: Option[Long],
      maxWarningCount: Option[Long],
      optimizerRules: Option[List[String]],
      profile: Option[Int],
      satelliteSyncWait: Option[Boolean],
      skipInaccessibleCollections: Option[Boolean],
      stream: Option[Boolean]
  )
