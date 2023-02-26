/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class ArangoRequestStatus(error: Boolean, code: Int)
case class ArangoResponse[T](status: ArangoRequestStatus, result: T)
