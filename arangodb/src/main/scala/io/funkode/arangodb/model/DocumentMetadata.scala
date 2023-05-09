/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class DocumentMetadata(
    _id: DocumentHandle,
    _key: DocumentKey,
    _rev: DocumentRevision
)
