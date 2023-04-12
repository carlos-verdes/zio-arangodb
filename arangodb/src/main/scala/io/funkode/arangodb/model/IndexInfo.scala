/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

import zio.json.{jsonDiscriminator, jsonField, jsonHint}

@jsonDiscriminator("type") enum IndexInfo(val handle: IndexHandle, val name: IndexName):

  @jsonHint("edge") case Edge(
      @jsonField("id") override val handle: IndexHandle,
      override val name: IndexName
  ) extends IndexInfo(handle, name)

  @jsonHint("fulltext") case Fulltext(
      @jsonField("id") override val handle: IndexHandle,
      override val name: IndexName
  ) extends IndexInfo(handle, name)

  @jsonHint("geo") case Geo(
      @jsonField("id") override val handle: IndexHandle,
      override val name: IndexName,
      fields: IndexGeoFields,
      geoJson: Boolean
  ) extends IndexInfo(handle, name)

  @jsonHint("inverted") case Inverted(
      @jsonField("id") override val handle: IndexHandle,
      override val name: IndexName
  ) extends IndexInfo(handle, name)

  @jsonHint("zkd") case MultiDimensional(
      @jsonField("id") override val handle: IndexHandle,
      override val name: IndexName
  ) extends IndexInfo(handle, name)

  @jsonHint("persistent") case Persistent(
      @jsonField("id") override val handle: IndexHandle,
      override val name: IndexName
  ) extends IndexInfo(handle, name)

  @jsonHint("primary") case Primary(
      @jsonField("id") override val handle: IndexHandle,
      override val name: IndexName
  ) extends IndexInfo(handle, name)

  @jsonHint("ttl") case TimeToLive(
      @jsonField("id") override val handle: IndexHandle,
      override val name: IndexName
  ) extends IndexInfo(handle, name)
