/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import java.util.Base64.Decoder

import zio.*
import zio.stream.*

import model.*
import protocol.*

trait ArangoClient[Encoder[_], Decoder[_]]:

  def head(header: ArangoMessage.Header): AIO[ArangoMessage.Header]
  def get[O: Decoder](header: ArangoMessage.Header): AIO[ArangoMessage[O]]
  def getRaw(header: ArangoMessage.Header): AIO[Stream[Throwable, Byte]]
  def command[I: Encoder, O: Decoder](message: ArangoMessage[I]): AIO[ArangoMessage[O]]
  def commandRaw[Encoder[_], Decoder[_]](
      message: ArangoMessage[Stream[Throwable, Byte]]
  ): AIO[Stream[Throwable, Byte]]

  def login(username: String, password: String): AIO[Token]

  def serverInfo: ArangoServer[Encoder, Decoder] = new ArangoServer[Encoder, Decoder](using this)

  def database(name: DatabaseName): ArangoDatabase[Encoder, Decoder] =
    new ArangoDatabase[Encoder, Decoder](name)(using this)

  def system: ArangoDatabase[Encoder, Decoder] =
    this.database(DatabaseName.system)

  def db: ArangoDatabase[Encoder, Decoder]

  def collection(collectionName: CollectionName): ArangoCollection[Encoder, Decoder] =
    new ArangoCollection[Encoder, Decoder](this.db.name, collectionName)(using this)

  def graph(graphName: GraphName): ArangoGraph[Encoder, Decoder] =
    new ArangoGraph[Encoder, Decoder](db.name, graphName)(using this)

  def getBody[O: Decoder](header: ArangoMessage.Header): AIO[O] = get(header).map(_.body)
  def getBodyRaw(header: ArangoMessage.Header): AIO[Stream[Throwable, Byte]] = getRaw(header)

  def commandBody[I: Encoder, O: Decoder](message: ArangoMessage[I]): AIO[O] =
    command(message).map(_.body)

  def commandBodyRaw[Encoder[_], Decoder[_]](
      message: ArangoMessage[Stream[Throwable, Byte]]
  ): AIO[Stream[Throwable, Byte]] =
    commandRaw[Encoder, Decoder](message)

  def withConfiguration(config: ArangoConfiguration): ArangoClient[Encoder, Decoder]

  extension (serviceWithClient: AIO[ArangoClient[Encoder, Decoder]])
    def withClient[O](f: ArangoClient[Encoder, Decoder] => O) =
      serviceWithClient.map(f)

    def database(name: DatabaseName): AIO[ArangoDatabase[Encoder, Decoder]] =
      withClient(_.database(name))

    def system: AIO[ArangoDatabase[Encoder, Decoder]] = withClient(_.system)

    def db: AIO[ArangoDatabase[Encoder, Decoder]] = withClient(_.db)

object ArangoClient:

  def withClient[Encoder[_]: TagK, Decoder[_]: TagK, O](
      f: ArangoClient[Encoder, Decoder] => O
  ): WithClient[Encoder, Decoder, O] =
    ZIO.service[ArangoClient[Encoder, Decoder]].map(f)

  def serverInfo[Encoder[_]: TagK, Decoder[_]: TagK]()
      : WithClient[Encoder, Decoder, ArangoServer[Encoder, Decoder]] =
    withClient(_.serverInfo)

  def database[Encoder[_]: TagK, Decoder[_]: TagK](
      name: DatabaseName
  ): WithClient[Encoder, Decoder, ArangoDatabase[Encoder, Decoder]] =
    withClient(_.database(name))

  def system[Encoder[_]: TagK, Decoder[_]: TagK]
      : WithClient[Encoder, Decoder, ArangoDatabase[Encoder, Decoder]] =
    withClient(_.system)

  def db[Encoder[_]: TagK, Decoder[_]: TagK]: WithClient[Encoder, Decoder, ArangoDatabase[Encoder, Decoder]] =
    withClient(_.db)

  def collection[Encoder[_]: TagK, Decoder[_]: TagK](
      collectionName: CollectionName
  ): WithClient[Encoder, Decoder, ArangoCollection[Encoder, Decoder]] =
    withClient(_.collection(collectionName))

  def graph[Encoder[_]: TagK, Decoder[_]: TagK](
      graphName: GraphName
  ): WithClient[Encoder, Decoder, ArangoGraph[Encoder, Decoder]] =
    withClient(_.graph(graphName))
