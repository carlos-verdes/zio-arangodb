/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

enum CollectionType(value: Int):
  case Unknown extends CollectionType(0)
  case Document extends CollectionType(2)
  case Edge extends CollectionType(3)
