/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import zio.json.*
import zio.json.JsonDecoder.{JsonError, UnsafeJson}
import zio.json.ast.*
import zio.json.internal.*

import io.funkode.velocypack.*
import CombineJsonDecoders.combine

object JsonCodecs:

  import VPack.*
  import model.*
  import model.IndexGeoFields.*

  given JsonCodec[ArangoError] = DeriveJsonCodec.gen[ArangoError]
  given JsonCodec[ArangoRequestStatus] = DeriveJsonCodec.gen[ArangoRequestStatus]
  given arangoResult[O](using JsonCodec[O]): JsonCodec[ArangoResult[O]] = DeriveJsonCodec.gen[ArangoResult[O]]
  given arangoResponse[O](using
      a: JsonDecoder[O],
      b: JsonDecoder[ArangoRequestStatus]
  ): JsonDecoder[ArangoResponse[O]] =
    a.combine(b)((a, b) => ArangoResponse(b, a))
  given JsonCodec[CollectionChecksum] = DeriveJsonCodec.gen[CollectionChecksum]
  given JsonCodec[CollectionCount] = DeriveJsonCodec.gen[CollectionCount]
  given JsonCodec[CollectionCreate.KeyOptions] = DeriveJsonCodec.gen[CollectionCreate.KeyOptions]
  given JsonCodec[CollectionCreate] = DeriveJsonCodec.gen[CollectionCreate]
  given JsonCodec[CollectionInfo] = DeriveJsonCodec.gen[CollectionInfo]
  given JsonCodec[IndexCreate] = DeriveJsonCodec.gen[IndexCreate]
  given JsonCodec[IndexInfo] = DeriveJsonCodec.gen[IndexInfo]
  given JsonCodec[IndexesInfo] = DeriveJsonCodec.gen[IndexesInfo]
  given JsonCodec[QueryResults.Extra] = DeriveJsonCodec.gen[QueryResults.Extra]
  given JsonCodec[QueryResults.ExtraStats] = DeriveJsonCodec.gen[QueryResults.ExtraStats]
  given queryResultsJsonCodec[O](using JsonCodec[O]): JsonCodec[QueryResults[O]] =
    DeriveJsonCodec.gen[QueryResults[O]]
  given JsonCodec[DatabaseCreate.User] = DeriveJsonCodec.gen[DatabaseCreate.User]
  given JsonCodec[DatabaseCreate] = DeriveJsonCodec.gen[DatabaseCreate]
  given JsonCodec[DatabaseInfo] = DeriveJsonCodec.gen[DatabaseInfo]
  given JsonCodec[DeleteResult] = DeriveJsonCodec.gen[DeleteResult]
  given JsonCodec[GraphCollections] = DeriveJsonCodec.gen[GraphCollections]
  given JsonCodec[VertexCollectionCreate] = DeriveJsonCodec.gen[VertexCollectionCreate]
  given JsonCodec[GraphCreate] = DeriveJsonCodec.gen[GraphCreate]
  given edge[T](using JsonCodec[T]): JsonCodec[GraphEdge[T]] = DeriveJsonCodec.gen[GraphEdge[T]]
  given edgeDocumentCreated[T](using JsonCodec[T]): JsonCodec[EdgeDocumentCreated[T]] =
    DeriveJsonCodec.gen[EdgeDocumentCreated[T]]
  given edgeDocumentDeleted[T](using JsonCodec[T]): JsonCodec[EdgeDocumentDeleted[T]] =
    DeriveJsonCodec.gen[EdgeDocumentDeleted[T]]
  given JsonCodec[GraphEdgeDefinition] = DeriveJsonCodec.gen[GraphEdgeDefinition]
  given JsonCodec[GraphInfo] = DeriveJsonCodec.gen[GraphInfo]
  given JsonCodec[GraphInfo.Response] = DeriveJsonCodec.gen[GraphInfo.Response]
  given JsonCodec[GraphList] = DeriveJsonCodec.gen[GraphList]
  given vertex[T](using JsonCodec[T]): JsonCodec[GraphVertex[T]] = DeriveJsonCodec.gen[GraphVertex[T]]
  given vertexCreated[T](using JsonCodec[T]): JsonCodec[VertexDocumentCreated[T]] =
    DeriveJsonCodec.gen[VertexDocumentCreated[T]]
  given vertexDeleted[T](using JsonCodec[T]): JsonCodec[VertexDocumentDeleted[T]] =
    DeriveJsonCodec.gen[VertexDocumentDeleted[T]]
  given doc[T: JsonCodec]: JsonCodec[Document[T]] = DeriveJsonCodec.gen[Document[T]]
  given JsonCodec[DocumentMetadata] = DeriveJsonCodec.gen[DocumentMetadata]
  given JsonCodec[Query.Options] = DeriveJsonCodec.gen[Query.Options]
  given JsonEncoder[Query] = DeriveJsonEncoder.gen[Query]
  given JsonCodec[ServerVersion] = DeriveJsonCodec.gen[ServerVersion]
  given JsonCodec[Token] = DeriveJsonCodec.gen[Token]
  given JsonCodec[UserPassword] = DeriveJsonCodec.gen[UserPassword]

  // opaque string based types
  given JsonCodec[DatabaseName] = DeriveOpaqueTypeCodec.gen(DatabaseName.apply, DatabaseName.unwrap)
  given JsonCodec[IndexName] = DeriveOpaqueTypeCodec.gen(IndexName.apply, IndexName.unwrap)
  given JsonCodec[CollectionName] = DeriveOpaqueTypeCodec.gen(CollectionName.apply, CollectionName.unwrap)
  given JsonCodec[DocumentKey] = DeriveOpaqueTypeCodec.gen(DocumentKey.apply, DocumentKey.unwrap)
  given JsonCodec[DocumentRevision] =
    DeriveOpaqueTypeCodec.gen(DocumentRevision.apply, DocumentRevision.unwrap)
  given JsonCodec[GraphName] = DeriveOpaqueTypeCodec.gen(GraphName.apply, GraphName.unwrap)
  given JsonCodec[TransactionId] = DeriveOpaqueTypeCodec.gen(TransactionId.apply, TransactionId.unwrap)

  // enum based types
  given JsonCodec[CollectionType] =
    DeriveOpaqueTypeCodec.gen(CollectionType.fromInt, (ct: CollectionType) => ct.value)
  given JsonCodec[CollectionStatus] =
    DeriveOpaqueTypeCodec.gen(CollectionStatus.fromOrdinal, CollectionStatus.ordinal)

  // special types
  given JsonCodec[DocumentHandle] =
    DeriveOpaqueTypeCodec.gen((s: String) => DocumentHandle.parse(s).get, DocumentHandle.unwrap)

  given JsonCodec[IndexHandle] =
    DeriveOpaqueTypeCodec.gen((s: String) => IndexHandle.parse(s).get, IndexHandle.unwrap)

  given JsonCodec[IndexGeoFields] =
    JsonCodec
      .list[String]
      .transformOrFail(
        {
          case List(locationName) => Right(Location(locationName))
          case List(latitudeName, longitudeName, _*) =>
            Right(LatLong(latitudeName, longitudeName))
          case Nil => Left("Unexpected empty fields list")
        },
        {
          case Location(locationName)               => List(locationName)
          case LatLong(latitudeName, longitudeName) => List(latitudeName, longitudeName)
        }
      )

  given vobjectEncoder: JsonEncoder[VObject] = JsonEncoder[Map[String, VPack]].contramap(_.values)

  given vpackEncoder: JsonEncoder[VPack] = (vpack: VPack, indent: Option[Int], out: Write) =>
    vpack match
      case VNone | VNull | VIllegal => JsonEncoder[String].unsafeEncode(null, indent, out)
      case VBoolean(value)          => JsonEncoder[Boolean].unsafeEncode(value, indent, out)
      case VTrue                    => JsonEncoder[Boolean].unsafeEncode(true, indent, out)
      case VFalse                   => JsonEncoder[Boolean].unsafeEncode(false, indent, out)
      case VDouble(value)           => JsonEncoder[Double].unsafeEncode(value, indent, out)
      case VDate(value)             => JsonEncoder[Long].unsafeEncode(value, indent, out)
      case VSmallint(value)         => JsonEncoder[Int].unsafeEncode(value, indent, out)
      case VLong(value)             => JsonEncoder[Long].unsafeEncode(value, indent, out)
      case VString(value)           => JsonEncoder[String].unsafeEncode(value, indent, out)
      case VBinary(value)           => JsonEncoder[String].unsafeEncode(value.toBase64, indent, out)
      case VArray(values)           => JsonEncoder[Array[VPack]].unsafeEncode(values.toArray, indent, out)
      case obj: VObject             => JsonEncoder[VObject].unsafeEncode(obj, indent, out)

  def jsonObjectToVObject(jsonObject: Json.Obj): VObject = VObject(
    jsonObject.fields.map((key, value) => (key, jsonToVpack(value))).toList*
  )

  def jsonToVpack(json: Json): VPack = json match
    case obj: Json.Obj      => jsonObjectToVObject(obj)
    case Json.Arr(elements) => VPackEncoder.listEncoder[VPack].encode(elements.toList.map(jsonToVpack))
    case Json.Bool(value)   => VBoolean(value)
    case Json.Str(value)    => VPackEncoder.stringEncoder.encode(value)
    case Json.Num(value)    => VPackEncoder.bigDecimalEncoder.encode(value)
    case Json.Null          => VNull

  given vpackDecoder: JsonDecoder[VPack] = JsonDecoder[Json].map(jsonToVpack)

  given vobjectDecoder: JsonDecoder[VObject] = (trace: List[JsonError], in: RetractReader) =>
    vpackDecoder
      .unsafeDecode(trace, in) match
      case obj: VObject => obj
      case other        => throw UnsafeJson(JsonError.Message(s"expected VObject found: ${other}") :: trace)

  given JsonCodec[VPack] = JsonCodec(vpackEncoder, vpackDecoder)
  given JsonCodec[VObject] = JsonCodec(vobjectEncoder, vobjectDecoder)
