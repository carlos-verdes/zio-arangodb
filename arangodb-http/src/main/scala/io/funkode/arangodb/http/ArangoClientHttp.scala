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
import zio.json.*
import zio.json.ast.*
import zio.prelude.*
import zio.schema.*
import zio.schema.codec.*
import zio.stream.*

import io.funkode.arangodb.docker.*
import io.funkode.arangodb.http.ArangoClientJson.arangoClientJson
import io.funkode.arangodb.model.*
import io.funkode.arangodb.protocol.*

type ArangoClientJson = ArangoClient[JsonEncoder, JsonDecoder]
type ArangoServerJson = ArangoServer[JsonEncoder, JsonDecoder]
type ArangoDatabaseJson = ArangoDatabase[JsonEncoder, JsonDecoder]
type ArangoCollectionJson = ArangoCollection[JsonEncoder, JsonDecoder]
type ArangoDocumentsJson = ArangoCollection[JsonEncoder, JsonDecoder]
type ArangoGraphJson = ArangoGraph[JsonEncoder, JsonDecoder]
type WithJsonClient[O] = WithClient[JsonEncoder, JsonDecoder, O]

type ArangoClientSchema = ArangoClient[Schema, Schema]
type ArangoServerSchema = ArangoServer[Schema, Schema]
type ArangoDatabaseSchema = ArangoDatabase[Schema, Schema]
type ArangoCollectionSchema = ArangoCollection[Schema, Schema]
type ArangoDocumentsSchema = ArangoCollection[Schema, Schema]
type ArangoGraphSchema = ArangoGraph[Schema, Schema]
type WithSchemaClient[O] = WithClient[Schema, Schema, O]

trait HttpEncoder[Encoder[_]]:
  def encode[R](r: R)(using Encoder[R]): Body

trait HttpDecoder[Decoder[_]]:
  def decode[R](body: Body)(using Decoder[R]): AIO[R]

class ArangoClientHttp[Encoder[_], Decoder[_]](
    config: ArangoConfiguration,
    httpClient: Client
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

  private val BaseUrl = URL(Root).withScheme(Scheme.HTTP).withHost(config.host).withPort(config.port)

  private val headers = Headers.apply(Header.Authorization.Basic(config.username, config.password))

  private lazy val _db = new ArangoDatabase[Encoder, Decoder](config.database)(using this)

  def db: ArangoDatabase[Encoder, Decoder] = _db

  def head(header: ArangoMessage.Header): AIO[ArangoMessage.Header] =
    for
      response <- httpClient.request(header.emptyRequest(BaseUrl, headers)).handleErrors
      validResponse <- handleResponseErrors(response)
    yield validResponse

  def getRaw(header: ArangoMessage.Header): ArangoStreamRaw =
    for
      response <- ZStream.fromZIO(httpClient.request(header.emptyRequest(BaseUrl, headers)).handleErrors)
      parsedResponse <- parseResponseBodyRaw(response)
    yield parsedResponse

  def get[O: Decoder](header: ArangoMessage.Header): AIO[ArangoMessage[O]] =
    for
      response <- httpClient.request(header.emptyRequest(BaseUrl, headers)).handleErrors
      body <- parseResponseBody(response)
    yield ArangoMessage(response, body)

  def commandRaw[Encoder[_], Decoder[_]](message: ArangoMessage[ArangoStreamRaw]): ArangoStreamRaw =
    val header = message.header.emptyRequest(BaseUrl, headers)
    val request: Request = header.copy(body = Body.fromStream(message.body))
    for
      response <- ZStream.fromZIO(httpClient.request(request).handleErrors)
      parsedResponse <- parseResponseBodyRaw(response)
    yield parsedResponse

  def command[I: Encoder, O: Decoder](message: ArangoMessage[I]): AIO[ArangoMessage[O]] =
    val header = message.header.emptyRequest(BaseUrl, headers)
    val request: Request = header.copy(body = httpEncoder.encode(message.body))
    for
      response <- httpClient.request(request).handleErrors
      body <- parseResponseBody(response).catchAll { case e: ArangoError =>
        for
          bodyString <- request.body.asString.catchAll(e =>
            ZIO.succeed(s"[error reading body: ${e.getMessage}]")
          )
          _ <- ZIO.logError(s"HTTP Command error")
          _ <- ZIO.logError(s"Request: ${request.method} ${request.url.path}")
          _ <- ZIO.logError(bodyString)
          error <- ZIO.fail(e)
        yield error
      }
    yield ArangoMessage(response, body)

  def login(username: String, password: String): AIO[Token] =
    for token <- getBody[Token](ArangoMessage.login(username, password))
    yield token

  def withConfiguration(newConfig: ArangoConfiguration): ArangoClient[Encoder, Decoder] =
    new ArangoClientHttp[Encoder, Decoder](newConfig, httpClient)

  private def parseResponseBody[O: Decoder](response: Response): AIO[O] =
    handleResponseErrors(response).flatMap(response => httpDecoder.decode[O](response.body))

  private def parseResponseBodyRaw(response: Response): ArangoStreamRaw =
    for
      response <- ZStream.fromZIO(handleResponseErrors(response))
      stream <- response.body.asStream.handleStreamErrors
    yield stream

  private def handleResponseErrors(response: Response): AIO[Response] =
    if response.status.isError
    then
      httpDecoder
        .decode[ArangoError](response.body)
        .foldCauseZIO(
          { case e =>
            response.body.asString
              .foldZIO(
                e =>
                  ZIO.fail(
                    ArangoError(500, true, "Can't get body of response from Arango: " + e.getMessage, -1)
                  ),
                body =>
                  ZIO.fail(
                    ArangoError(
                      response.status.code,
                      true,
                      s"Incorrect status from server: ${response.status.code}. Body: $body",
                      -1
                    )
                  )
              )
          },
          arangoError => ZIO.fail(arangoError)
        )
    else ZIO.succeed(response)

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
      resp.headers.iterator.map(h => (h.headerName, h.renderedValue)).toMap
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
        val requestUrl = baseUrl.withPath(apiDatabasePrefixPath(database).addParts(requestPath.parts))
        val headers: Headers = meta
        requestHeader(headers ++ extraHeaders, requestType, requestUrl.withQueryParams(parameters))

      // support for async responses https://www.arangodb.com/docs/stable/http/async-results-management.html#managing-async-results-via-http
      case ArangoMessage.Header.Response(_, _, _, meta) =>
        val headers: Headers = meta
        requestHeader(
          headers ++ extraHeaders,
          Method.PUT,
          baseUrl.withPath(asyncResponsePath(meta.get(ArangoAsyncId).getOrElse(EmptyString)))
        )

      case ArangoMessage.Header.Authentication(_, credentials) =>
        val body = credentials match
          case userPassword: UserPassword => httpEncoder.encode(userPassword)
          case token: Token               => httpEncoder.encode(token)
        requestWithBody(body, Map.empty, Method.POST, baseUrl.withPath(LoginPath))

  extension [Encoder[_], T: Encoder](arangoMessage: ArangoMessage[T])
    def httpRequest(baseUrl: URL, extraHeaders: Headers = Headers.empty)(using
        httpEncoder: HttpEncoder[Encoder],
        tokenEncoder: Encoder[Token],
        userPassEncoder: Encoder[UserPassword]
    ) =
      val header = arangoMessage.header.emptyRequest(baseUrl, extraHeaders)
      header.copy(body = httpEncoder.encode(arangoMessage.body))

  extension (s: String | Null) def getOrEmpty: String = if s != null then s else ""

  def throwableToArangoError(t: Throwable): ArangoError = t match
    case e: MalformedURLException =>
      ArangoMessage.error(Status.BadRequest.code, e.getMessage.getOrEmpty)
    case t: Throwable =>
      ArangoMessage.error(Status.InternalServerError.code, RuntimeError + t.getMessage.getOrEmpty)

  extension [A](call: IO[Throwable, A])
    def handleErrors: IO[ArangoError, A] = call.catchAll(e => ZIO.fail(throwableToArangoError(e)))

  extension (stream: Stream[Throwable, Byte])
    def handleStreamErrors: ArangoStreamRaw = stream.catchAll(e => ZStream.fail(throwableToArangoError(e)))

object ArangoClientSchema:

  import SchemaCodecs.given

  def withClient[O](
      f: ArangoClient[Schema, Schema] => O
  ): WithSchemaClient[O] =
    ZIO.service[ArangoClientSchema].map(f)

  def serverInfo(): WithSchemaClient[ArangoServerSchema] =
    withClient(_.serverInfo)

  def database(
      name: DatabaseName
  ): WithSchemaClient[ArangoDatabaseSchema] =
    withClient(_.database(name))

  def system: WithSchemaClient[ArangoDatabaseSchema] =
    withClient(_.system)

  def db: WithSchemaClient[ArangoDatabaseSchema] =
    withClient(_.db)

  def collection(collectionName: CollectionName): WithSchemaClient[ArangoCollectionSchema] =
    withClient(_.collection(collectionName))

  def graph(graphName: GraphName): WithSchemaClient[ArangoGraphSchema] =
    withClient(_.graph(graphName))

  val schemaEncoderForHttp: HttpEncoder[Schema] = new HttpEncoder[Schema]:
    override def encode[R](r: R)(using S: Schema[R]) =
      Body.fromCharSequence(zio.schema.codec.JsonCodec.jsonEncoder(S).encodeJson(r, None))

  val schemaDecoderForHttp: HttpDecoder[Schema] = new HttpDecoder[Schema]:
    override def decode[R](body: Body)(using S: Schema[R]): AIO[R] =
      zio.schema.codec.JsonCodec
        .jsonDecoder(S)
        .decodeJsonStreamInput(body.asStream)
        .catchAll { case t: Throwable =>
          for
            failedString <- body.asString.catchAll(e => ZIO.succeed("not able to read body: " + e.getMessage))
            errorMessage =
              s"Error parsing JSON Arango response: " + t.getMessage + s"\nBody: ${failedString} \nSchema: ${S.toString}"
            _ <- ZIO.logErrorCause(errorMessage, Cause.fail(t))
            zioError <- ZIO.fail(ArangoError(500, true, errorMessage, -1))
          yield zioError
        }

  def arangoClientSchema(
      config: ArangoConfiguration,
      httpClient: Client,
      token: Option[Token] = None
  ): ArangoClientSchema =
    new ArangoClientHttp[Schema, Schema](
      config,
      httpClient
    )(using
      schemaEncoderForHttp,
      schemaDecoderForHttp,
      given_Schema_Token,
      given_Schema_Token,
      given_Schema_UserPassword,
      given_Schema_UserPassword,
      given_Schema_ArangoError
    )

  val live: ZLayer[ArangoConfiguration & Client, ArangoError, ArangoClientSchema] =
    ZLayer(for
      config <- ZIO.service[ArangoConfiguration]
      httpClient <- ZIO.service[Client]
      token <- arangoClientSchema(config, httpClient).login(config.username, config.password)
      arangoClient = arangoClientSchema(config, httpClient, Some(token))
    yield arangoClient)

  val testContainers
      : ZLayer[ArangoConfiguration & Client, ArangoError, ArangoClientSchema & ArangoContainer] =
    ZLayer.scopedEnvironment(
      for
        aconfig <- ZIO.service[ArangoConfiguration]
        container <- ArangoContainer.makeScopedContainer(aconfig)
        newConfig = aconfig.copy(
          port = container.container.getFirstMappedPort.nn,
          host = container.container.getHost.nn
        )
        httpClient <- ZIO.service[Client]
        token <- arangoClientSchema(newConfig, httpClient).login(newConfig.username, newConfig.password)
      yield ZEnvironment(arangoClientSchema(newConfig, httpClient, Some(token)), container)
    )

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
      for
        body <- body.asString
          .catchAll { case t: Throwable =>
            ZIO.fail(ArangoError(500, true, "Error getting body from Arango response" + t.getMessage, -1))
          }
        nonEmptyBody <-
          if body.nonEmpty then ZIO.succeed(body)
          else
            ZIO.fail(
              ArangoError(
                500,
                true,
                s"Empty body in Arango response",
                -1
              )
            )
        result <- ZIO
          .fromEither(D.decodeJson(nonEmptyBody))
          .mapError(decodeError =>
            ArangoError(
              500,
              true,
              s"Error parsing JSON Arango response: " + decodeError + s"\nBody: $body",
              -1
            )
          )
      yield result

  def arangoClientJson(
      config: ArangoConfiguration,
      httpClient: Client
  ): ArangoClient[JsonEncoder, JsonDecoder] =
    new ArangoClientHttp[JsonEncoder, JsonDecoder](
      config,
      httpClient
    )(using
      jsonEncoderForHttp,
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
      arangoClient = arangoClientJson(config, httpClient)
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
      yield ZEnvironment(
        arangoClientJson(
          newConfig,
          httpClient
        ),
        container
      )
    )
