/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.velocypack

import java.net.{URI, URL}
import java.time.{Instant, LocalDate}
import java.util.{Date, UUID}

import scodec.bits.ByteVector
import zio.prelude.*

trait VPackEncoder[-T]:

  def encode(t: T): VPack

  extension (t: T) def toVpack: VPack = encode(t)

object VPackEncoder:

  import VPack.*
  import VPackKeyEncoder.given
  import zio.prelude.ContravariantOps

  given Contravariant[VPackEncoder] = new Contravariant[VPackEncoder]:
    override def contramap[A, B](f: B => A): VPackEncoder[A] => VPackEncoder[B] =
      (fa: VPackEncoder[A]) => (b: B) => fa.encode(f(b))

  given encodedType[T](using VPackEncoder[T]): Conversion[T, VPack] with
    def apply(t: T): VPack = t.toVpack

  // scala encoders

  given VPackEncoder[Boolean] with
    def encode(b: Boolean) = if b then VTrue else VFalse

  given VPackEncoder[Byte] with
    def encode(b: Byte) = if VSmallint.isValid(b) then VSmallint(b) else VLong(b.toLong)

  given VPackEncoder[Short] with
    def encode(i: Short) = if VSmallint.isValid(i) then VSmallint(i.toByte) else VLong(i.toLong)

  given VPackEncoder[Int] with
    def encode(i: Int) = if VSmallint.isValid(i) then VSmallint(i.toByte) else VLong(i.toLong)

  given VPackEncoder[Long] with
    def encode(l: Long) = if VSmallint.isValid(l) then VSmallint(l.toByte) else VLong(l)

  given VPackEncoder[BigInt] with
    def encode(i: BigInt) =
      if VSmallint.isValidByte(i) then VSmallint(i.toByte)
      else if i.isValidLong then VLong(i.toLong)
      else VBinary(ByteVector(i.toByteArray))

  given VPackEncoder[Float] with
    def encode(f: Float) =
      if VSmallint.isValidByte(f) then VSmallint(f.toByte)
      else if f.toLong.toFloat == f then VLong(f.toLong)
      else VDouble(f.toDouble)

  given VPackEncoder[Double] with
    def encode(d: Double) =
      if VSmallint.isValidByte(d) then VSmallint(d.toByte)
      else if d.toLong.toDouble == d then VLong(d.toLong)
      else VDouble(d)

  given bigDecimalEncoder: VPackEncoder[BigDecimal] with
    def encode(d: BigDecimal) =
      if VSmallint.isValidByte(d) then VSmallint(d.toByte)
      else if d.isValidLong then VLong(d.toLongExact)
      else if d.isDecimalDouble then VDouble(d.toDouble)
      else VBinary(ByteVector.fromInt(d.scale) ++ ByteVector(d.underlying().unscaledValue().toByteArray))

  given stringEncoder: VPackEncoder[String] = VString(_)

  given VPackEncoder[Instant] with
    def encode(i: Instant) = VDate(i.toEpochMilli)

  given VPackEncoder[Date] with
    def encode(d: Date) = VDate(d.getTime)

  given byteVectorEncoder: VPackEncoder[ByteVector] with
    def encode(value: ByteVector) = VBinary(value)

  given VPackEncoder[Array[Byte]] = byteVectorEncoder.contramap(ByteVector.apply)

  given VPackEncoder[UUID] = byteVectorEncoder.contramap(ByteVector.fromUUID)

  given optionEncoder[T](using e: VPackEncoder[T]): VPackEncoder[Option[T]] =
    _.fold[VPack](VNull)(e.encode)
  given vectorEncoder[T](using e: VPackEncoder[T]): VPackEncoder[Vector[T]] = a =>
    VArray(a.toList.map(e.encode))

  given listEncoder[T](using e: VPackEncoder[T]): VPackEncoder[List[T]] = a => VArray(a.map(e.encode))
  given seqEncoder[T](using e: VPackEncoder[T]): VPackEncoder[Seq[T]] = a => VArray(a.toList.map(e.encode))
  given setEncoder[T](using e: VPackEncoder[T]): VPackEncoder[Set[T]] = a => VArray(a.toList.map(e.encode))
  given arrayEncoder[T](using e: VPackEncoder[T]): VPackEncoder[Array[T]] = a =>
    VArray(a.toList.map(e.encode))
  given iterableEncoder[T](using e: VPackEncoder[T]): VPackEncoder[Iterable[T]] = a =>
    VArray(a.toList.map(e.encode))

  given mapEncoder[K, T](using
      ke: VPackKeyEncoder[K],
      te: VPackEncoder[T]
  ): VPackEncoder[Map[K, T]] =
    a => VObject(a.map { case (k, t) => (ke.encode(k), te.encode(t)) })

  given unitEncoder: VPackEncoder[Unit] = _ => VNone
  given vPackEncoder: VPackEncoder[VPack] = identity(_)
  given vArrayEncoder: VPackEncoder[VArray] = identity(_)
  given vObjectEncoder: VPackEncoder[VObject] = identity(_)
  given localDateEncoder: VPackEncoder[LocalDate] = stringEncoder.contramap(_.toString)
  given uriEncoder: VPackEncoder[URI] = stringEncoder.contramap(_.toString)
  given urlEncoder: VPackEncoder[URL] = stringEncoder.contramap(_.toString)
