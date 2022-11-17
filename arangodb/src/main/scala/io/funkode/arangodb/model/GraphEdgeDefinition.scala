package io.funkode.arangodb.model

case class GraphEdgeDefinition(
    collection: CollectionName,
    from: List[CollectionName],
    to: List[CollectionName]
)
