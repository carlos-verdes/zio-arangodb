package io.funkode.arangodb.model

final case class CollectionChecksum(
    name: CollectionName,
    checksum: String,
    revision: String
)
