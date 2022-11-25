/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class GraphEdgeDefinition(
    collection: CollectionName,
    from: List[CollectionName],
    to: List[CollectionName]
)
