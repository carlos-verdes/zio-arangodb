/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

final case class CollectionChecksum(
    name: CollectionName,
    checksum: String,
    revision: String
)
