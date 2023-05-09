/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class VertexDocumentDeleted[T](error: Boolean, code: Long, removed: Boolean, old: Option[Document[T]])
