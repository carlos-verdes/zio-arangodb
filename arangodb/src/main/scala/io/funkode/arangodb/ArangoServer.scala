/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import io.lemonlabs.uri.UrlPath
import zio.*

import model.*
import protocol.*

class ArangoServer[Encoder[_], Decoder[_]](using arangoClient: ArangoClient[Encoder, Decoder]):

  import ArangoMessage.*
  import ArangoServer.*

  def databases(using Decoder[ArangoResult[List[DatabaseName]]]): AIO[List[DatabaseName]] =
    GET(DatabaseName.system, ApiDatabase.addPart("user")).executeIgnoreResult

  def version(details: Boolean = false)(using Decoder[ServerVersion]): AIO[ServerVersion] =
    GET(DatabaseName.system, ApiVersionPath, parameters = Map(Details -> details.toString)).execute

// def engine(): F[ArangoResponse[Engine]]
// def role(): F[ArangoResponse[ServerRole]]
// def logLevel(): F[ArangoResponse[AdminLog.Levels]]
// def logLevel(levels: AdminLog.Levels): F[ArangoResponse[AdminLog.Levels]]

object ArangoServer:

  import ArangoMessage.*

  val VersionString = "version"

  val Details = "details"

  extension [R, Encoder[_], Decoder[_]](serverService: ZIO[R, ArangoError, ArangoServer[Encoder, Decoder]])
    def version(details: Boolean = false)(using Decoder[ServerVersion]): ZIO[R, ArangoError, ServerVersion] =
      serverService.flatMap(_.version(details))

    def databases(using Decoder[ArangoResult[List[DatabaseName]]]): ZIO[R, ArangoError, List[DatabaseName]] =
      serverService.flatMap(_.databases)
