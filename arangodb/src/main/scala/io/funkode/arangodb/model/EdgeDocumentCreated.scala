/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class EdgeDocumentCreated[T](
    error: Boolean,
    code: Long,
    edge: DocumentMetadata,
    `new`: Option[Document[T]]
)
