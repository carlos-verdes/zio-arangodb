/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import zio.schema.*
import io.funkode.velocypack.{VPack, *}
import zio.Chunk
import zio.schema.Schema.CaseClass1

object SchemaCodecs:

  import model.*
  import VPack.*

  given Schema[ArangoError] = DeriveSchema.gen[ArangoError]

  given arangoResultSchema[O](using S: Schema[O]): Schema[ArangoResult[O]] =
    Schema.CaseClass3[Boolean, Int, O, ArangoResult[O]](
      TypeId.parse("io.funkode.arangodb.model.ArangoResult"),
      Schema.Field("error", Schema[Boolean]),
      Schema.Field("code", Schema[Int]),
      Schema.Field("result", S),
      (error: Boolean, code: Int, result: O) => ArangoResult(error, code, result),
      _.error,
      _.code,
      _.result
    )

  given Schema[CollectionChecksum] = DeriveSchema.gen[CollectionChecksum]
  given Schema[CollectionCount] = DeriveSchema.gen[CollectionCount]
  given Schema[CollectionCreate.KeyOptions] = DeriveSchema.gen[CollectionCreate.KeyOptions]
  given Schema[CollectionCreate] = DeriveSchema.gen[CollectionCreate]
  given Schema[CollectionInfo] = DeriveSchema.gen[CollectionInfo]
  given Schema[QueryResults.Extra] = DeriveSchema.gen[QueryResults.Extra]
  given Schema[QueryResults.ExtraStats] = DeriveSchema.gen[QueryResults.ExtraStats]
  given queryResultsSchema[O](using S: Schema[O]): Schema[QueryResults[O]] =
    Schema.CaseClass6[Boolean, Option[Long], Option[QueryResults.Extra], Boolean, Option[String], List[
      O
    ], QueryResults[O]](
      TypeId.parse("io.funkode.arangodb.model.QueryResults"),
      Schema.Field("cached", Schema[Boolean]),
      Schema.Field("count", Schema[Option[Long]]),
      Schema.Field("extra", Schema[Option[QueryResults.Extra]]),
      Schema.Field("hasMore", Schema[Boolean]),
      Schema.Field("id", Schema[Option[String]]),
      Schema.Field("result", Schema[List[O]]),
      (
          cached: Boolean,
          count: Option[Long],
          extra: Option[QueryResults.Extra],
          hasMore: Boolean,
          id: Option[String],
          result: List[O]
      ) => QueryResults(cached, count, extra, hasMore, id, result),
      _.cached,
      _.count,
      _.extra,
      _.hasMore,
      _.id,
      _.result
    )

  given Schema[DatabaseCreate.User] = DeriveSchema.gen[DatabaseCreate.User]
  given Schema[DatabaseCreate] = DeriveSchema.gen[DatabaseCreate]
  given Schema[DatabaseInfo] = DeriveSchema.gen[DatabaseInfo]
  given Schema[DeleteResult] = DeriveSchema.gen[DeleteResult]
  given Schema[GraphCollections] = DeriveSchema.gen[GraphCollections]
  given Schema[GraphCreate] = DeriveSchema.gen[GraphCreate]
  given edge[T](using S: Schema[T]): Schema[GraphEdge[T]] =
    Schema.CaseClass1[T, GraphEdge[T]](
      TypeId.parse("io.funkode.arangodb.model.GraphEdge"),
      Schema.Field("edge", S),
      (edge: T) => GraphEdge(edge),
      _.edge
    )

  given Schema[GraphEdgeDefinition] = DeriveSchema.gen[GraphEdgeDefinition]
  given Schema[GraphInfo] = DeriveSchema.gen[GraphInfo]
  given Schema[GraphInfo.Response] = DeriveSchema.gen[GraphInfo.Response]
  given Schema[GraphList] = DeriveSchema.gen[GraphList]
  given vertex[T](using S: Schema[T]): Schema[GraphVertex[T]] =
    Schema.CaseClass1[T, GraphVertex[T]](
      TypeId.parse("io.funkode.arangodb.model.GraphVertex"),
      Schema.Field("vertex", S),
      (vertex: T) => GraphVertex(vertex),
      _.vertex
    )

  given doc[T: Schema](using S: Schema[T]): Schema[Document[T]] =
    Schema.CaseClass6[DocumentHandle, DocumentKey, DocumentRevision, Option[T], Option[T], Option[
      DocumentRevision
    ], Document[T]](
      TypeId.parse("io.funkode.arangodb.model.Document"),
      Schema.Field("_id", Schema[DocumentHandle]),
      Schema.Field("_key", Schema[DocumentKey]),
      Schema.Field("_rev", Schema[DocumentRevision]),
      Schema.Field("`new`", Schema.Optional(S)),
      Schema.Field("old", Schema.Optional(S)),
      Schema.Field("_oldRev", Schema[Option[DocumentRevision]]),
      (
          _id: DocumentHandle,
          _key: DocumentKey,
          _rev: DocumentRevision,
          `new`: Option[T],
          old: Option[T],
          _oldRev: Option[DocumentRevision]
      ) => Document(_id, _key, _rev, `new`, old, _oldRev),
      _._id,
      _._key,
      _._rev,
      _.`new`,
      _.old,
      _._oldRev
    )

  given Schema[Query.Options] = DeriveSchema.gen[Query.Options]
//  given Schema[Query] = DeriveSchema.gen[Query]

  given Schema[ServerVersion] = DeriveSchema.gen[ServerVersion]
  given Schema[Token] = DeriveSchema.gen[Token]
  given Schema[UserPassword] = DeriveSchema.gen[UserPassword]
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

// given Schema[VNone | VNone | VNull | VIllegal]

//  given vobjectEncoder: Schema[VObject] =
//    DeriveSchema.gen[VObject] // Schema[Map[String, VPack]].contramap(_.values)

  import VPack.*

  given vbooleanSchema: Schema[VBoolean] =
    CaseClass1[Boolean, VPack.VBoolean](
      TypeId.parse("io.funkode.velocypack.VPack.VBoolean"),
      Schema.Field[Boolean]("value", Schema[Boolean]),
      (b: Boolean) => VPack.VBoolean(b),
      _.value
    )

  given vpackSchema: Schema[VPack] = Schema.Enum1(
    TypeId.parse("io.funkode.velocypack.VPack"),
    /*
    Schema.Case[VPack.VNone, VPack](
      "VNone",
      Schema.singleton(VPack.VNone),
      _.asInstanceOf[VPack.VNone],
      Chunk.empty
    ),*/
    Schema.Case[VPack.VBoolean, VPack](
      "VBoolean",
      vbooleanSchema,
      _.asInstanceOf[VPack.VBoolean],
      Chunk.empty
    )
  )

/*
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
