/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

import zio.json.{jsonDiscriminator, jsonHint}

import IndexGeoFields.*

@jsonDiscriminator("type") sealed trait IndexCreate:
  val name: IndexName

object IndexCreate:
  @jsonHint("geo") final case class Geo private (
      name: IndexName,
      fields: IndexGeoFields,
      geoJson: Option[Boolean],
      inBackground: Option[Boolean]
  ) extends IndexCreate

  object Geo:
    def location(
        name: IndexName,
        location: String,
        geoJson: Option[Boolean] = None,
        inBackground: Option[Boolean] = None
    ): Geo =
      IndexCreate.Geo(name, Location(location), geoJson, inBackground)

    def latLong(
        name: IndexName,
        latitude: String,
        longitude: String,
        inBackground: Option[Boolean] = None
    ): Geo =
      IndexCreate.Geo(name, LatLong(latitude, longitude), None, inBackground)
