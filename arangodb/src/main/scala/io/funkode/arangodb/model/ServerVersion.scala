/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class ServerVersion(
    server: String,
    license: String,
    version: String,
    details: Map[String, String] = Map.empty
)
