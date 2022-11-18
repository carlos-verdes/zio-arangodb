package io.funkode.arangodb
package protocol

import scala.collection.immutable.Map

import io.lemonlabs.uri.UrlPath
import zio.prelude.Covariant

case class ArangoMessage[+T](header: ArangoMessage.Header, body: T)

object ArangoMessage:

  import model.*

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val Plain = "plain"

  enum Header(version: ArangoVersion, `type`: MessageType):
    case Request(
        version: ArangoVersion = ArangoVersion.Current,
        database: DatabaseName = DatabaseName.system,
        requestType: RequestType,
        request: UrlPath,
        parameters: Map[String, String] = Map.empty,
        meta: Map[String, String] = Map.empty
    ) extends Header(version, MessageType.Request)

    case Response(
        version: ArangoVersion,
        `type`: MessageType,
        responseCode: Long,
        meta: Map[String, String] = Map.empty
    ) extends Header(version, `type`)

    case Authentication(
        encryption: String,
        credentials: UserPassword | Token
    ) extends Header(ArangoVersion.Current, MessageType.Authentication)

  extension (header: Header)
    def withBody[O](o: O): ArangoMessage[O] = ArangoMessage(header, o)

    def head[Encoder[_], Decoder[_]](using
        arangoClient: ArangoClient[Encoder, Decoder]
    ): AIO[ArangoMessage.Header] =
      arangoClient.head(header)

    def execute[O, Encoder[_], Decoder[_]](using
        arangoClient: ArangoClient[Encoder, Decoder],
        D: Decoder[O]
    ): AIO[O] =
      arangoClient.getBody[O](header)

    def executeIgnoreResult[O, Encoder[_], Decoder[_]](using
        arangoClient: ArangoClient[Encoder, Decoder],
        D: Decoder[ArangoResult[O]]
    ): AIO[O] =
      execute[ArangoResult[O], Encoder, Decoder].map(_.result)

  extension [I](arangoMessage: ArangoMessage[I])
    def execute[O, Encoder[_], Decoder[_]](using
        arangoClient: ArangoClient[Encoder, Decoder],
        E: Encoder[I],
        D: Decoder[O]
    ): AIO[O] =
      arangoClient.commandBody[I, O](arangoMessage)

    def executeIgnoreResult[O, Encoder[_], Decoder[_]](using
        arangoClient: ArangoClient[Encoder, Decoder],
        E: Encoder[I],
        D: Decoder[ArangoResult[O]]
    ): AIO[O] = execute[ArangoResult[O], Encoder, Decoder].map(_.result)

  extension (params: Map[String, Option[String]])
    def collectDefined: Map[String, String] =
      params.collect { case (key, Some(value)) =>
        key -> value
      }

  def DELETE(
      database: DatabaseName,
      request: UrlPath,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header.Request(ArangoVersion.Current, database, RequestType.DELETE, request, parameters, meta)

  def GET(
      database: DatabaseName,
      request: UrlPath,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header.Request(ArangoVersion.Current, database, RequestType.GET, request, parameters, meta)

  def POST(
      database: DatabaseName,
      request: UrlPath,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header.Request(ArangoVersion.Current, database, RequestType.POST, request, parameters, meta)

  def PUT(
      database: DatabaseName,
      request: UrlPath,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header.Request(ArangoVersion.Current, database, RequestType.PUT, request, parameters, meta)

  def HEAD[T](
      database: DatabaseName,
      request: UrlPath,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header.Request(ArangoVersion.Current, database, RequestType.HEAD, request, parameters, meta)

  def PATCH(
      database: DatabaseName,
      request: UrlPath,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header.Request(ArangoVersion.Current, database, RequestType.PATCH, request, parameters, meta)

  def OPTIONS(
      database: DatabaseName,
      request: UrlPath,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header.Request(ArangoVersion.Current, database, RequestType.OPTIONS, request, parameters, meta)

  def responseFinal(code: Long, meta: Map[String, String] = Map.empty): Header.Response =
    Header.Response(ArangoVersion.Current, MessageType.ResponseFinal, code, meta)

  def error(code: Long, msg: String = ""): ArangoError = ArangoError(code, true, msg, -1)

  def login(user: String, password: String): Header.Authentication =
    Header.Authentication(Plain, UserPassword(user, password))

  given Covariant[ArangoMessage] = new Covariant[ArangoMessage]:
    override def map[A, B](f: A => B): ArangoMessage[A] => ArangoMessage[B] =
      (a: ArangoMessage[A]) => a.copy(body = f(a.body))
