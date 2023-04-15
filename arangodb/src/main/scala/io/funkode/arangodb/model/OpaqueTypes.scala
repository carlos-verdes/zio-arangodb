/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

import io.lemonlabs.uri.UrlPath
import zio.config.ConfigDescriptor.string
import zio.config.magnolia.Descriptor

opaque type CollectionName = String
opaque type IndexName = String
opaque type IndexId = String
opaque type IndexHandle = (CollectionName, IndexId)
opaque type DocumentKey = String
opaque type DocumentHandle = (CollectionName, DocumentKey)
opaque type DocumentRevision = String
opaque type DatabaseName = String
opaque type GraphName = String
opaque type TransactionId = String

object IndexName:

  def apply(name: String): IndexName = name
  extension (name: IndexName) def unwrap: String = name

  val primary: IndexName = IndexName("primary")
  val edge: IndexName = IndexName("edge")

object IndexHandle:

  def apply(col: CollectionName, id: IndexId): IndexHandle = (col, id)

  def parse(path: String): Option[IndexHandle] =
    path.split('/') match
      case Array(collection, id) => Some(apply(CollectionName(collection), id))
      case _                     => None

  extension (handle: IndexHandle)
    def collection: CollectionName = handle._1
    def id: IndexId = handle._2
    def isEmpty: Boolean = handle._1.isEmpty && handle._2.isEmpty
    def path: UrlPath =
      val col: CollectionName = CollectionName(handle._1).unwrap
      val id: IndexId = handle._2
      UrlPath.empty.addPart(col).addPart(id)
    def unwrap: String =
      val col: CollectionName = CollectionName(handle._1).unwrap
      val id: IndexId = handle._2
      s"${col}/${id}"

object IndexId:

  def apply(id: String): IndexId = id

  extension (id: IndexId) def unwrap: String = id

object CollectionName:

  def apply(name: String): CollectionName = name
  extension (name: CollectionName) def unwrap: String = name

  import zio.config.ConfigDescriptor.string
  import zio.config.magnolia.Descriptor

  given Descriptor[CollectionName] = Descriptor.from(string)

object DocumentHandle:

  val Key = "_key"

  def apply(col: CollectionName, key: DocumentKey): DocumentHandle = (col, key)

  def parse(path: String): Option[DocumentHandle] =
    path.split('/') match
      case Array(collection, key) => Some(apply(CollectionName(collection), DocumentKey(key)))
      case _                      => None

  extension (handle: DocumentHandle)
    def collection: CollectionName = handle._1
    def key: DocumentKey = handle._2
    def isEmpty: Boolean = handle._1.isEmpty && handle._2.isEmpty
    def path: UrlPath =
      val col: CollectionName = CollectionName(handle._1).unwrap
      val key: DocumentKey = DocumentKey(handle._2).unwrap
      UrlPath.empty.addPart(col).addPart(key)
    def unwrap: String =
      val col: CollectionName = CollectionName(handle._1).unwrap
      val key: DocumentKey = DocumentKey(handle._2).unwrap
      s"${col}/${key}"

object DocumentKey:

  val Key = "_key"

  def apply(key: String): DocumentKey = key

  extension (key: DocumentKey) def unwrap: String = key

  val empty = apply("")
  extension (key: DocumentKey) def isEmpty: Boolean = key.isEmpty

object DatabaseName:

  def apply(name: String): DatabaseName = name
  extension (name: DatabaseName) def unwrap: String = name

  given Descriptor[DatabaseName] = Descriptor.from(string)

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val system = DatabaseName("_system")

object DocumentRevision:

  val Key: String = "_rev"

  def apply(value: String): DocumentRevision = value
  extension (value: DocumentRevision) def unwrap: String = value

  val empty = apply("")

object GraphName:

  def apply(value: String): GraphName = value
  extension (value: GraphName) def unwrap: String = value

object TransactionId:

  def apply(value: String): TransactionId = value
  extension (value: TransactionId) def unwrap: String = value
