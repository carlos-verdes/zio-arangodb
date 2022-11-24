/*
 * TODO: License goes here!
 */
package io.funkode.arangodb

import io.funkode.velocypack.{VPack, VPackEncoder}
import io.funkode.velocypack.VPack.VObject
import model.*
import protocol.*
import ArangoMessage.*

class ArangoDocument[Encoder[_], Decoder[_]](databaseName: DatabaseName, documentHandle: DocumentHandle)(using
  arangoClient: ArangoClient[Encoder, Decoder]
):

  def database: DatabaseName = databaseName
  def handle: DocumentHandle = documentHandle

  private val path = ApiDocumentPath.addParts(handle.path.parts)

  def read[T: Decoder](
    ifNoneMatch: Option[String] = None,
    ifMatch: Option[String] = None,
    transaction: Option[TransactionId] = None
  ): AIO[T] =
    GET(
      database,
      path,
      meta = Map(
        "If-None-Match" -> ifNoneMatch,
        "If-Match" -> ifMatch,
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).execute

  def head(
    ifNoneMatch: Option[String] = None,
    ifMatch: Option[String] = None,
    transaction: Option[TransactionId] = None
  ): AIO[ArangoMessage.Header] =
    HEAD(
      database,
      path,
      meta = Map(
        "If-None-Match" -> ifNoneMatch,
        "If-Match" -> ifMatch,
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).head

  def remove[T](
    waitForSync: Boolean = false,
    returnOld: Boolean = false,
    silent: Boolean = false,
    ifMatch: Option[String] = None,
    transaction: Option[TransactionId] = None
  )(using D: Decoder[Document[T]]): AIO[Document[T]] =
    DELETE(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "returnOld" -> returnOld.toString,
        "silent" -> silent.toString
      ),
      Map(
        "If-Match" -> ifMatch,
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).execute

  def update[T, P](
    patch: P,
    keepNull: Boolean = false,
    mergeObjects: Boolean = true,
    waitForSync: Boolean = false,
    ignoreRevs: Boolean = true,
    returnOld: Boolean = false,
    returnNew: Boolean = false,
    silent: Boolean = false,
    ifMatch: Option[String] = None,
    transaction: Option[TransactionId] = None
  )(using
    Encoder[P],
    Decoder[Document[T]]
  ): AIO[Document[T]] =
    PATCH(
      database,
      path,
      Map(
        "keepNull" -> keepNull.toString,
        "mergeObjects" -> mergeObjects.toString,
        "waitForSync" -> waitForSync.toString,
        "ignoreRevs" -> ignoreRevs.toString,
        "returnOld" -> returnOld.toString,
        "returnNew" -> returnNew.toString,
        "silent" -> silent.toString
      ),
      Map(
        "If-Match" -> ifMatch,
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(patch).execute

  def replace[T](
    document: T,
    waitForSync: Boolean = false,
    ignoreRevs: Boolean = true,
    returnOld: Boolean = false,
    returnNew: Boolean = false,
    silent: Boolean = false,
    ifMatch: Option[String] = None,
    transaction: Option[TransactionId] = None
  )(using
    Encoder[T],
    Decoder[Document[T]]
  ): AIO[Document[T]] =
    PUT(
      database,
      path,
      Map(
        "waitForSync" -> waitForSync.toString,
        "ignoreRevs" -> ignoreRevs.toString,
        "returnOld" -> returnOld.toString,
        "returnNew" -> returnNew.toString,
        "silent" -> silent.toString
      ),
      Map(
        "If-Match" -> ifMatch,
        Transaction.Key -> transaction.map(_.unwrap)
      ).collectDefined
    ).withBody(document).execute

  /*
  import VPack.*
  import VObject.updated
  import VPackEncoder.given

  def upsert(obj: VObject)(using
    Encoder[Query],
    Decoder[Cursor[VObject]]
  ): AIO[VObject] =
    val kvs = obj.values.keys
      .map { key =>
        key + ":@" + key
      }
      .mkString(",")
    val queryString =
      s"UPSERT {_key:@_key} INSERT {_key:@_key,$kvs} UPDATE {$kvs} IN @@collection RETURN NEW"

    new ArangoQuery.Impl[Encoder, Decoder](
      databaseName,
      Query(
        queryString,
        obj.updated("@collection", handle.collection.unwrap).updated("_key", handle.key.unwrap)
      )
    ).execute[VObject].map(_.result.head)
  */
