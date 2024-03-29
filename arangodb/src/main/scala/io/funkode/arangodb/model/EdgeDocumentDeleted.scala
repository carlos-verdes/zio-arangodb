/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class EdgeDocumentDeleted[T](
    error: Boolean,
    code: Long,
    old: Option[Document[T]]
)
