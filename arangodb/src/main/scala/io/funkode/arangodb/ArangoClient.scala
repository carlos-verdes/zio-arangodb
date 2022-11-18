package io.funkode.arangodb

import zio.*

import model.*
import protocol.*

trait ArangoClient[Encoder[_], Decoder[_]]:

  def head(header: ArangoMessage.Header): AIO[ArangoMessage.Header]
  def get[O: Decoder](header: ArangoMessage.Header): AIO[ArangoMessage[O]]
  def command[I: Encoder, O: Decoder](message: ArangoMessage[I]): AIO[ArangoMessage[O]]

  def login(username: String, password: String): AIO[Token]

  def getBody[O: Decoder](header: ArangoMessage.Header): AIO[O] = get(header).map(_.body)
  def commandBody[I: Encoder, O: Decoder](message: ArangoMessage[I]): AIO[O] =
    command(message).map(_.body)

  def database(name: DatabaseName): ArangoDatabase[Encoder, Decoder]

  def system: ArangoDatabase[Encoder, Decoder]

  def db: ArangoDatabase[Encoder, Decoder]

  extension (serviceWithClient: AIO[ArangoClient[Encoder, Decoder]])
    def withClient[O](f: ArangoClient[Encoder, Decoder] => O) =
      serviceWithClient.map(f)

    def database(name: DatabaseName): AIO[ArangoDatabase[Encoder, Decoder]] =
      withClient(_.database(name))

    def system: AIO[ArangoDatabase[Encoder, Decoder]] = withClient(_.system)

    def db: AIO[ArangoDatabase[Encoder, Decoder]] = withClient(_.db)
