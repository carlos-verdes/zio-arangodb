/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import zio.schema.*
import io.funkode.velocypack.*
import zio.Chunk

object SchemaCodecs:

  import model.*
  import VPack.*

  given Schema[ArangoError] = DeriveSchema.gen[ArangoError]
  /*
  given arangoResult[?]: Schema[ArangoResult[?]] = DeriveSchema.gen[ArangoResult[?]]

  given Schema[CollectionChecksum] = DeriveSchema.gen[CollectionChecksum]
  given Schema[CollectionCount] = DeriveSchema.gen[CollectionCount]
  given Schema[CollectionCreate.KeyOptions] = DeriveSchema.gen[CollectionCreate.KeyOptions]
  given Schema[CollectionCreate] = DeriveSchema.gen[CollectionCreate]
  given Schema[CollectionInfo] = DeriveSchema.gen[CollectionInfo]
  given Schema[QueryResults.Extra] = DeriveSchema.gen[QueryResults.Extra]
  given Schema[QueryResults.ExtraStats] = DeriveSchema.gen[QueryResults.ExtraStats]
  given cursor[O](using Schema[O]): Schema[QueryResults[O]] = DeriveSchema.gen[QueryResults[O]]
  given Schema[DatabaseCreate.User] = DeriveSchema.gen[DatabaseCreate.User]
  given Schema[DatabaseCreate] = DeriveSchema.gen[DatabaseCreate]
  given Schema[DatabaseInfo] = DeriveSchema.gen[DatabaseInfo]
  given Schema[DeleteResult] = DeriveSchema.gen[DeleteResult]
  given Schema[GraphCollections] = DeriveSchema.gen[GraphCollections]
  given Schema[GraphCreate] = DeriveSchema.gen[GraphCreate]
  given edge[T](using Schema[T]): Schema[GraphEdge[T]] = DeriveSchema.gen[GraphEdge[T]]
  given Schema[GraphEdgeDefinition] = DeriveSchema.gen[GraphEdgeDefinition]
  given Schema[GraphInfo] = DeriveSchema.gen[GraphInfo]
  given Schema[GraphInfo.Response] = DeriveSchema.gen[GraphInfo.Response]
  given Schema[GraphList] = DeriveSchema.gen[GraphList]
  given vertex[T](using Schema[T]): Schema[GraphVertex[T]] = DeriveSchema.gen[GraphVertex[T]]
  given doc[T: Schema]: Schema[Document[T]] = DeriveSchema.gen[Document[T]]
  given Schema[Query.Options] = DeriveSchema.gen[Query.Options]
  given Schema[Query] = DeriveSchema.gen[Query]
  given Schema[ServerVersion] = DeriveSchema.gen[ServerVersion]
  */
  given Schema[Token] = DeriveSchema.gen[Token]
  given Schema[UserPassword] = DeriveSchema.gen[UserPassword]
/*
  // opaque string based types
  given Schema[DatabaseName] = DeriveOpaqueTypeSchema.gen(DatabaseName.apply, DatabaseName.unwrap)
  given Schema[CollectionName] = DeriveOpaqueTypeSchema.gen(CollectionName.apply, CollectionName.unwrap)
  given Schema[DocumentKey] = DeriveOpaqueTypeSchema.gen(DocumentKey.apply, DocumentKey.unwrap)
  given Schema[DocumentRevision] =
    DeriveOpaqueTypeSchema.gen(DocumentRevision.apply, DocumentRevision.unwrap)
  given Schema[GraphName] = DeriveOpaqueTypeSchema.gen(GraphName.apply, GraphName.unwrap)
  given Schema[TransactionId] = DeriveOpaqueTypeSchema.gen(TransactionId.apply, TransactionId.unwrap)

  // enum based types
  given Schema[CollectionType] =
  DeriveOpaqueTypeSchema.gen(CollectionType.fromOrdinal, CollectionType.ordinal)
  given Schema[CollectionStatus] =
    DeriveOpaqueTypeSchema.gen(CollectionStatus.fromOrdinal, CollectionStatus.ordinal)

  // special types
  given Schema[DocumentHandle] =
  DeriveOpaqueTypeSchema.gen((s: String) => DocumentHandle.parse(s).get, DocumentHandle.unwrap)

  given vobjectEncoder: Schema[VObject] = Schema[Map[String, VPack]].contramap(_.values)

  given vpackEncoder: Schema[VPack] = zio.schema.DeriveSchema

    (vpack: VPack, indent: Option[Int], out: Write) =>
    vpack match
      case VNone | VNone | VNull | VIllegal => JsonEncoder[String].unsafeEncode(null, indent, out)
      case VBoolean(value)                  => JsonEncoder[Boolean].unsafeEncode(value, indent, out)
      case VTrue                            => JsonEncoder[Boolean].unsafeEncode(true, indent, out)
      case VFalse                           => JsonEncoder[Boolean].unsafeEncode(false, indent, out)
      case VDouble(value)                   => JsonEncoder[Double].unsafeEncode(value, indent, out)
      case VDate(value)                     => JsonEncoder[Long].unsafeEncode(value, indent, out)
      case VSmallint(value)                 => JsonEncoder[Int].unsafeEncode(value, indent, out)
      case VLong(value)                     => JsonEncoder[Long].unsafeEncode(value, indent, out)
      case VString(value)                   => JsonEncoder[String].unsafeEncode(value, indent, out)
      case VBinary(value)                   => JsonEncoder[String].unsafeEncode(value.toBase64, indent, out)
      case VArray(values) => JsonEncoder[Array[VPack]].unsafeEncode(values.toArray, indent, out)
      case obj: VPack.VObject   => JsonEncoder[VPack.VObject].unsafeEncode(obj, indent, out)

  def jsonObjectToVObject(jsonObject: Json.Obj): VObject =
    VObject(jsonObject.fields.map((key, value) => (key, jsonToVpack(value))).toList*)

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

  given Schema[VPack] = Schema(vpackEncoder, vpackDecoder)
  given Schema[VObject] = Schema(vobjectEncoder, vobjectDecoder)

*/
