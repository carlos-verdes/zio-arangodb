/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.velocypack

import scala.math.ScalaNumericAnyConversions

import scodec.bits.ByteVector

enum VPack(name: String, isEmpty: Boolean):
  case VNone extends VPack("none", true)
  case VIllegal extends VPack("illegal", false)
  case VNull extends VPack("null", true)
  case VBoolean(value: Boolean) extends VPack(if value then "true" else "false", false)
  case VTrue extends VPack("true", false)
  case VFalse extends VPack("false", false)
  case VDouble(value: Double) extends VPack("double", false)
  case VDate(value: Long) extends VPack("date", false)
  // case VMinKey extends VPack("min-key", false)
  // case VMaxKey extends VPack("max-key", false)
  case VSmallint(value: Byte) extends VPack("smallint", false)
  case VLong(value: Long) extends VPack("int", false)
  case VString(value: String) extends VPack("string", value.isEmpty)
  case VBinary(value: ByteVector) extends VPack("binary", value.isEmpty)
  case VArray(values: List[VPack]) extends VPack("array", values.isEmpty)
  case VObject(values: Map[String, VPack]) extends VPack("object", values.isEmpty)

object VPack:

  object VSmallint:

    def isValid[T](arg: T)(using num: Numeric[T]): Boolean =
      num.lt(arg, num.fromInt(10)) && num.gt(arg, num.fromInt(-7))

    extension (n: ScalaNumericAnyConversions) def isValidByte: Boolean = n.isValidByte && isValid(n.toByte)

  object VObject:

    val KeyKey = "_key"
    val IdKey = "_id"
    val RevKey = "_rev"

    import VPack.VObject

    def apply(values: (String, VPack)*): VObject = new VObject(values.toMap)
    val empty: VObject = new VObject(Map.empty[String, VPack])

    extension (obj: VObject)
      def updated(key: String, value: VPack): VObject =
        obj.copy(values = obj.values.updated(key, value))

      def filter(p: ((String, VPack)) => Boolean): VObject = obj.copy(values = obj.values.filter(p))

      def without(keys: String*): VObject = VObject(obj.values.filter((key, _) => !keys.contains(key)))
      def withoutKey: VObject = obj.without(KeyKey)
      def withoutId: VObject = obj.without(IdKey)
      def withoutRev: VObject = obj.without(RevKey)
      def pure: VObject = obj.without(KeyKey, IdKey, RevKey)
