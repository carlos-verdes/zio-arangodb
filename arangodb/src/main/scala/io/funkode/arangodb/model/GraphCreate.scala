/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class GraphCreate(
    name: GraphName,
    edgeDefinitions: List[GraphEdgeDefinition],
    orphanCollections: List[String]
)
