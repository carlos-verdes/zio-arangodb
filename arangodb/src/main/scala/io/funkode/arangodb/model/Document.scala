package io.funkode.arangodb.model

case class Document[T](
    _id: DocumentHandle,
    _key: DocumentKey,
    _rev: DocumentRevision,
    `new`: Option[T] = None,
    old: Option[T] = None,
    _oldRev: Option[DocumentRevision] = None
)
