/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

import zio.schema.annotation.{fieldDefaultValue, optionalField}

case class ServerVersion(
    server: String,
    license: String,
    version: String,
    @optionalField
    @fieldDefaultValue[Map[String, String]](Map.empty)
    details: Map[String, String] = Map.empty
)
