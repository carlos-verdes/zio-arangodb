/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

enum IndexGeoFields:
  case Location(val location: String) extends IndexGeoFields
  case LatLong(val latitude: String, val longitude: String) extends IndexGeoFields
