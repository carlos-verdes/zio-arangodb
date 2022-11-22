package io.funkode.arangodb

import java.util.concurrent.TimeUnit.*

import scala.concurrent.duration.*

import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

final case class ArangoConfiguration(
    host: String,
    port: Int = ArangoConfiguration.DefaultPort,
    username: String,
    password: String,
    chunkLength: Long = ArangoConfiguration.ChunkLengthDefault,
    readBufferSize: Int = ArangoConfiguration.ReadBufferSizeDefault,
    connectTimeout: Duration = ArangoConfiguration.ConnectTimeoutDefault,
    replyTimeout: Duration = ArangoConfiguration.ReplyTimeoutDefault,
    database: model.DatabaseName = model.DatabaseName.system
)

object ArangoConfiguration:

  import ConfigDescriptor.nested

  val DefaultPort = 8529
  val ChunkLengthDefault: Long = 30000L
  val ReadBufferSizeDefault: Int = 256 * 1024
  val ConnectTimeoutDefault: Duration = 10.seconds
  val ReplyTimeoutDefault: Duration = 30.seconds

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val DefaultPath = "arangodb"

  val arangoConfigDescriptor = descriptor[ArangoConfiguration].mapKey(toKebabCase)

  def fromPath(path: String) = TypesafeConfig.fromResourcePath(nested(path)(arangoConfigDescriptor))
  val default = fromPath(DefaultPath)
