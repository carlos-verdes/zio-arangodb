/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

enum CollectionType(val value: Int):
  case Unknown extends CollectionType(0)
  case Document extends CollectionType(2)
  case Edge extends CollectionType(3)

object CollectionType:

  def fromInt(value: Int): CollectionType =
    if value == 0 then CollectionType.Unknown
    else if value == 2 then CollectionType.Document
    else if value == 3 then CollectionType.Edge
    else throw new Exception(s"Not supported CollectionType($value)")
