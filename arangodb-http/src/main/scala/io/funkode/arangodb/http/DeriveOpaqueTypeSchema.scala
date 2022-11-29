/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.http

import zio.schema.*

object DeriveOpaqueTypeSchema:

  inline def gen[T, S](inline create: S => T, inline unwrap: T => S)(using Schema[S]): Schema[T] =
    createOpaqueSchema(create, unwrap)

  def createOpaqueSchema[T, S](create: S => T, unwrap: T => S)(using S: Schema[S]) =
    S.transform(create, unwrap)
