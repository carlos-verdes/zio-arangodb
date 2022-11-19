package io.funkode.arangodb

import java.util.concurrent.TimeUnit.*

import scala.concurrent.duration.*

import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

final case class ArangoConfiguration(
    host: String,
    port: Int = 8529,
    username: String,
    password: String,
    chunkLength: Long = ArangoConfiguration.CHUNK_LENGTH_DEFAULT,
    readBufferSize: Int = ArangoConfiguration.READ_BUFFER_SIZE_DEFAULT,
    connectTimeout: Duration = ArangoConfiguration.CONNECT_TIMEOUT_DEFAULT,
    replyTimeout: Duration = ArangoConfiguration.REPLY_TIMEOUT_DEFAULT,
    database: model.DatabaseName = model.DatabaseName.system
)

object ArangoConfiguration:

  import ConfigDescriptor.nested

  val CHUNK_LENGTH_DEFAULT: Long = 30000L
  val READ_BUFFER_SIZE_DEFAULT: Int = 256 * 1024
  val CONNECT_TIMEOUT_DEFAULT: Duration = 10.seconds
  val REPLY_TIMEOUT_DEFAULT: Duration = 30.seconds

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val DefaultPath = "arangodb"

  val arangoConfigDescriptor = descriptor[ArangoConfiguration].mapKey(toKebabCase)

  def fromPath(path: String) = TypesafeConfig.fromResourcePath(nested(path)(arangoConfigDescriptor))
  val default = fromPath(DefaultPath)
