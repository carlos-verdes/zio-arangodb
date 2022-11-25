/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.http

import zio.json.{JsonCodec, JsonDecoder, JsonEncoder}

object DeriveOpaqueTypeCodec:

  inline def gen[T, S](inline create: S => T, inline unwrap: T => S)(using
      JsonCodec[S]
  ): zio.json.JsonCodec[T] =
    createOpaqueCodec(create, unwrap)

  def createOpaqueCodec[T, S](create: S => T, unwrap: T => S)(using
      JsonCodec[S]
  ) =
    val encoder = JsonEncoder[S].contramap(unwrap)
    val decoder = JsonDecoder[S].map(create)

    JsonCodec(encoder, decoder)
