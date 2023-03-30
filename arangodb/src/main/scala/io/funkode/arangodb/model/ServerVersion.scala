/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

import zio.schema.annotation.*

case class ServerVersion(
    server: String,
    license: String,
    version: String,
    @fieldDefaultValue(Map.empty) details: Map[String, String] = Map.empty
)
