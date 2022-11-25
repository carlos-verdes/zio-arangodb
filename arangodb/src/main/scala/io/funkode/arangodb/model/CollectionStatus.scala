/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

enum CollectionStatus(value: Int):
  case Unknown extends CollectionStatus(0)
  case NewBorn extends CollectionStatus(1)
  case Unloaded extends CollectionStatus(2)
  case Loaded extends CollectionStatus(3)
  case Unloading extends CollectionStatus(4)
  case Deleted extends CollectionStatus(5)
  case Loading extends CollectionStatus(6)
