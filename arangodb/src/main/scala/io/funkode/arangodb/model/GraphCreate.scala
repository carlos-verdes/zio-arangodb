package io.funkode.arangodb.model

case class GraphCreate(
    name: GraphName,
    edgeDefinitions: List[GraphEdgeDefinition],
    orphanCollections: List[String]
)
