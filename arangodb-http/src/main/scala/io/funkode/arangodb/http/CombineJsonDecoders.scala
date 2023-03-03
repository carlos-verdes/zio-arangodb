/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.http

import zio.json.{JsonDecoder, JsonError, JsonRewindDecoder}
import zio.json.internal.RetractReader

object CombineJsonDecoders:
  extension [A](decoder: JsonDecoder[A])
    def combine[B, O](other: JsonDecoder[B])(fn: (A, B) => O): JsonDecoder[O] =
      new JsonDecoder[O]:
        override def unsafeDecode(trace: List[JsonError], in: RetractReader): O =
          val aDecoder: JsonRewindDecoder[A] = new JsonRewindDecoder[A](decoder)
          val a = aDecoder.unsafeDecode(trace, in)
          aDecoder.reader.rewind()
          val b = other.unsafeDecode(trace, aDecoder.reader)
          fn(a, b)
