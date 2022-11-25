/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class DatabaseInfo(name: String, id: String, path: String, isSystem: Boolean)
