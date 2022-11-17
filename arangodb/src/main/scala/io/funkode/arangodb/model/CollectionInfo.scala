package io.funkode.arangodb.model

final case class CollectionInfo(
    id: String,
    name: CollectionName,
    status: CollectionStatus,
    `type`: CollectionType,
    isSystem: Boolean,
    globallyUniqueId: String
)
