/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class VertexDocumentCreated[T](
    error: Boolean,
    code: Long,
    vertex: DocumentMetadata,
    `new`: Option[Document[T]]
)
