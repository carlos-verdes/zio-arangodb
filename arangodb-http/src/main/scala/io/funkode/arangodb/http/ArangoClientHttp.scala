/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import java.net.{MalformedURLException, URISyntaxException}

import io.lemonlabs.uri.*
import io.netty.handler.codec.http.HttpHeaderNames
import zio.*
import zio.http.*
import zio.http.model.*
import zio.http.model.Headers.BearerSchemeName
import zio.http.model.Status.*
import zio.json.*
import zio.prelude.*

import io.funkode.arangodb.docker.*
import io.funkode.arangodb.model.*
import io.funkode.arangodb.protocol.*

type ArangoClientJson = ArangoClient[JsonEncoder, JsonDecoder]
type ArangoServerJson = ArangoServer[JsonEncoder, JsonDecoder]
type ArangoDatabaseJson = ArangoDatabase[JsonEncoder, JsonDecoder]
type ArangoCollectionJson = ArangoCollection[JsonEncoder, JsonDecoder]
type ArangoDocumentsJson = ArangoCollection[JsonEncoder, JsonDecoder]
type ArangoGraphJson = ArangoGraph[JsonEncoder, JsonDecoder]
type WithJsonClient[O] = WithClient[JsonEncoder, JsonDecoder, O]

trait HttpEncoder[Encoder[_]]:
  def encode[R](r: R)(using Encoder[R]): Body

trait HttpDecoder[Decoder[_]]:
  def decode[R](body: Body)(using Decoder[R]): AIO[R]

class ArangoClientHttp[Encoder[_], Decoder[_]](
    config: ArangoConfiguration,
    httpClient: Client,
    token: Option[Token] = None
)(using
    httpEncoder: HttpEncoder[Encoder],
    httpDecoder: HttpDecoder[Decoder],
    tokenEncoder: Encoder[Token],
    tokenDecoder: Decoder[Token],
    userEncoder: Encoder[UserPassword],
    userDecodeR: Decoder[UserPassword],
    errorDecoder: Decoder[ArangoError]
) extends io.funkode.arangodb.ArangoClient[Encoder, Decoder]:

  import constants.*
  import conversions.given
  import extensions.*

  private val BaseUrl = URL(!!).setScheme(Scheme.HTTP).setHost(config.host).setPort(config.port)

  private val headers =
    token.map(_.jwt).map(Headers.bearerAuthorizationHeader).getOrElse(Headers.empty)

  private lazy val _db = new ArangoDatabase[Encoder, Decoder](config.database)(using this)

  def db: ArangoDatabase[Encoder, Decoder] = _db

  def head(header: ArangoMessage.Header): AIO[ArangoMessage.Header] =
    for response <- httpClient.request(header.emptyRequest(BaseUrl, headers)).handleErrors
    yield response

  def get[O: Decoder](header: ArangoMessage.Header): AIO[ArangoMessage[O]] =
    for
      response <- httpClient.request(header.emptyRequest(BaseUrl, headers)).handleErrors
      body <- parseResponseBody(response)
    yield ArangoMessage(response, body)

  def command[I: Encoder, O: Decoder](message: ArangoMessage[I]): AIO[ArangoMessage[O]] =

    val header = message.header.emptyRequest(BaseUrl, headers)
    val request = header.copy(body = httpEncoder.encode(message.body))
    for
      response <- httpClient.request(request).handleErrors
      body <- parseResponseBody(response)
    yield ArangoMessage(response, body)

  def login(username: String, password: String): AIO[Token] =
    for token <- getBody[Token](ArangoMessage.login(username, password))
    yield token

  def withConfiguration(newConfig: ArangoConfiguration): ArangoClient[Encoder, Decoder] =
    new ArangoClientHttp[Encoder, Decoder](newConfig, httpClient, token)

  private def parseResponseBody[O: Decoder](response: Response)(using Decoder[ArangoError]): AIO[O] =
    for body <-
        if response.status.isError
        then httpDecoder.decode[ArangoError](response.body).flatMap[Any, ArangoError, O](r => ZIO.fail(r))
        else httpDecoder.decode[O](response.body)
    yield body

  def currentDatabase: DatabaseName = (config.database)

object constants:

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val Open = "_open"
  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val Auth = "auth"
  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val Api = "_api"
  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val Job = "job"

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val EmptyString = ""

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val RuntimeError = "Runtime error "

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val ArangoAsyncId = "x-arango-async-id"

  val ApiPath = zio.http.Path.root / Api
  val LoginPath = zio.http.Path.root / Open / Auth

  def asyncResponsePath(jobId: String) = ApiPath / Job / jobId

object conversions:

  import constants.*

  given Conversion[RequestType, Method] = _ match
    case RequestType.DELETE  => Method.DELETE
    case RequestType.GET     => Method.GET
    case RequestType.POST    => Method.POST
    case RequestType.PUT     => Method.PUT
    case RequestType.HEAD    => Method.HEAD
    case RequestType.PATCH   => Method.PATCH
    case RequestType.OPTIONS => Method.OPTIONS

  given Conversion[Response, ArangoMessage.Header] = resp =>
    ArangoMessage.responseFinal(
      resp.status.code,
      resp.headers.iterator.map(h => (h.key.toString, h.value.toString)).toMap
    )

  given Conversion[Vector[String], zio.http.Path] = parts =>
    zio.http.Path(parts.map(zio.http.Path.Segment.apply))

  given Conversion[io.lemonlabs.uri.UrlPath, zio.http.Path] = _ match
    case path: AbsoluteOrEmptyPath =>
      path match
        case EmptyPath           => zio.http.Path.empty
        case AbsolutePath(parts) => zio.http.Path.root ++ parts
    case RootlessPath(parts) => parts

  given Conversion[Map[String, String], Headers] =
    _.map((k, v) => Headers(k, v)).fold(Headers.empty)(_ ++ _)

  given Conversion[Map[String, String], QueryParams] = params =>
    if params.isEmpty then QueryParams.empty
    else
      val values = params.view.iterator.toList
      QueryParams(values.head, values.tail*)

object extensions:

  import constants.*
  import conversions.given

  def requestWithBody(body: Body, headers: Headers, method: Method, url: URL): Request =
    Request(body, headers, method, url, Version.Http_1_1, Option.empty)

  def requestHeader(headers: Headers, method: Method, url: URL): Request =
    Request(Body.empty, headers, method, url, Version.Http_1_1, Option.empty)

  extension [Encoder[_]](header: ArangoMessage.Header)
    def emptyRequest(baseUrl: URL, extraHeaders: Headers = Headers.empty)(using
        httpEncoder: HttpEncoder[Encoder],
        tokenEncoder: Encoder[Token],
        userPassEncoder: Encoder[UserPassword]
    ) = header match
      case ArangoMessage.Header.Request(_, database, requestType, requestPath, parameters, meta) =>
        val requestUrl = baseUrl.setPath(apiDatabasePrefixPath(database).addParts(requestPath.parts))
        val headers: Headers = meta
        requestHeader(headers ++ extraHeaders, requestType, requestUrl.setQueryParams(parameters))

      // support for async responses https://www.arangodb.com/docs/stable/http/async-results-management.html#managing-async-results-via-http
      case ArangoMessage.Header.Response(_, _, _, meta) =>
        val headers: Headers = meta
        requestHeader(
          headers ++ extraHeaders,
          Method.PUT,
          baseUrl.setPath(asyncResponsePath(meta.get(ArangoAsyncId).getOrElse(EmptyString)))
        )

      case ArangoMessage.Header.Authentication(_, credentials) =>
        val body = credentials match
          case userPassword: UserPassword => httpEncoder.encode(userPassword)
          case token: Token               => httpEncoder.encode(token)
        requestWithBody(body, Map.empty, Method.POST, baseUrl.setPath(LoginPath))

  extension [Encoder[_], T: Encoder](arangoMessage: ArangoMessage[T])
    def httpRequest(baseUrl: URL, extraHeaders: Headers = Headers.empty)(using
        httpEncoder: HttpEncoder[Encoder],
        tokenEncoder: Encoder[Token],
        userPassEncoder: Encoder[UserPassword]
    ) =
      val header = arangoMessage.header.emptyRequest(baseUrl, extraHeaders)
      header.copy(body = httpEncoder.encode(arangoMessage.body))

  extension (s: String | Null) def getOrEmpty: String = if s != null then s else ""

  extension [A](call: IO[Throwable, A])
    def handleErrors: IO[ArangoError, A] =
      call.catchAll {
        case e: MalformedURLException =>
          ZIO.fail(ArangoMessage.error(Status.BadRequest.code, e.getMessage.getOrEmpty))
        case t: Throwable =>
          ZIO.fail(
            ArangoMessage.error(Status.InternalServerError.code, RuntimeError + t.getMessage.getOrEmpty)
          )
      }

object ArangoClientJson:

  import zio.json.*
  import JsonCodecs.given

  def withClient[O](
      f: ArangoClient[JsonEncoder, JsonDecoder] => O
  ): WithJsonClient[O] =
    ZIO.service[ArangoClientJson].map(f)

  def serverInfo(): WithJsonClient[ArangoServerJson] =
    withClient(_.serverInfo)

  def database(
      name: DatabaseName
  ): WithJsonClient[ArangoDatabaseJson] =
    withClient(_.database(name))

  def system: WithJsonClient[ArangoDatabaseJson] =
    withClient(_.system)

  def db: WithJsonClient[ArangoDatabaseJson] =
    withClient(_.db)

  def collection(collectionName: CollectionName): WithJsonClient[ArangoCollectionJson] =
    withClient(_.collection(collectionName))

  def graph(graphName: GraphName): WithJsonClient[ArangoGraphJson] =
    withClient(_.graph(graphName))

  val jsonEncoderForHttp: HttpEncoder[JsonEncoder] = new HttpEncoder[JsonEncoder]:
    override def encode[R](r: R)(using E: JsonEncoder[R]) =
      Body.fromString(E.encodeJson(r, None).toString)

  val jsonDecoderForHttp: HttpDecoder[JsonDecoder] = new HttpDecoder[JsonDecoder]:
    override def decode[R](body: Body)(using D: JsonDecoder[R]): AIO[R] =
      body.asString
        .catchAll { case t: Throwable =>
          ZIO.fail(ArangoError(500, true, "Error getting body from Arango response" + t.getMessage, -1))
        }
        .flatMap(s => ZIO.fromEither(D.decodeJson(s)))
        .catchAll { case t: Throwable =>
          ZIO.fail(ArangoError(500, true, "Error parsing JSON Arango response" + t.getMessage, -1))
        }

  def arangoClientJson(
      config: ArangoConfiguration,
      httpClient: Client,
      token: Option[Token] = None
  ): ArangoClient[JsonEncoder, JsonDecoder] =
    new ArangoClientHttp[JsonEncoder, JsonDecoder](
      config,
      httpClient,
      token
    )(
      using jsonEncoderForHttp,
      jsonDecoderForHttp,
      given_JsonCodec_Token.encoder,
      given_JsonCodec_Token.decoder,
      given_JsonCodec_UserPassword.encoder,
      given_JsonCodec_UserPassword.decoder,
      given_JsonCodec_ArangoError.decoder
    )

  val live: ZLayer[ArangoConfiguration & Client, ArangoError, ArangoClientJson] =
    ZLayer(for
      config <- ZIO.service[ArangoConfiguration]
      httpClient <- ZIO.service[Client]
      token <- arangoClientJson(config, httpClient).login(config.username, config.password)
      arangoClient = arangoClientJson(config, httpClient, Some(token))
    yield arangoClient)

  val testContainers: ZLayer[ArangoConfiguration & Client, ArangoError, ArangoClientJson & ArangoContainer] =
    ZLayer.scopedEnvironment(
      for
        aconfig <- ZIO.service[ArangoConfiguration]
        container <- ArangoContainer.makeScopedContainer(aconfig)
        newConfig = aconfig.copy(
          port = container.container.getFirstMappedPort.nn,
          host = container.container.getHost.nn
        )
        httpClient <- ZIO.service[Client]
        token <- arangoClientJson(newConfig, httpClient).login(newConfig.username, newConfig.password)
      yield ZEnvironment(arangoClientJson(newConfig, httpClient, Some(token)), container)
    )
