/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.velocypack

import scodec.bits.ByteVector
import zio.prelude.*

trait VPackKeyEncoder[-T]:
  def encode(t: T): String

object VPackKeyEncoder:

  import VPack.*
  import zio.prelude.ContravariantOps

  given Contravariant[VPackKeyEncoder] = new Contravariant[VPackKeyEncoder]:
    override def contramap[A, B](f: B => A): VPackKeyEncoder[A] => VPackKeyEncoder[B] =
      (fa: VPackKeyEncoder[A]) => (b: B) => fa.encode(f(b))

  given VPackKeyEncoder[String] = (x) => x
  given VPackKeyEncoder[Symbol] = _.name
