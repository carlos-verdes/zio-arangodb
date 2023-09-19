/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb

import io.funkode.velocypack.*
import model.*
import protocol.*
import ArangoMessage.*
import VPack.*

class ArangoGraph[Encoder[_], Decoder[_]](database: DatabaseName, graphName: GraphName)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  def name: GraphName = graphName

  private val path = ApiGharialPath.addPart(graphName.unwrap)
  private val vertexPath = path.addPart("vertex")
  private val edgePath = path.addPart("edge")

  def create(
      edgeDefinitions: List[GraphEdgeDefinition] = List.empty,
      orphanCollections: List[String] = List.empty,
      waitForSync: Boolean = false
  )(using Encoder[GraphCreate], Decoder[GraphInfo.Response]): AIO[GraphInfo] =
    POST(
      database,
      ApiGharialPath,
      Map(
        "waitForSync" -> waitForSync.toString
      )
    ).withBody(
      GraphCreate(name, edgeDefinitions, orphanCollections)
    ).execute[GraphInfo.Response, Encoder, Decoder]
      .map(_.graph)

  def info(using Decoder[ArangoResult[GraphInfo.Response]]): AIO[GraphInfo] =
    GET(database, path)
      .executeIgnoreResult[GraphInfo.Response, Encoder, Decoder]
      .map(_.graph)

  def drop(dropCollections: Boolean = false)(using
      Decoder[ArangoResult[RemovedResult]]
  ): AIO[Boolean] =
    DELETE(database, path, Map("dropCollections" -> dropCollections.toString))
      .executeIgnoreResult[RemovedResult, Encoder, Decoder]
      .map(_.removed)

  def vertexCollections(using Decoder[ArangoResponse[GraphCollections]]): AIO[List[CollectionName]] =
    GET(database, vertexPath)
      .execute[ArangoResponse[GraphCollections], Encoder, Decoder]
      .map(_.result.collections)

  def addVertexCollection(
      collection: CollectionName
  )(using Encoder[VertexCollectionCreate], Decoder[ArangoResponse[GraphInfo.Response]]): AIO[GraphInfo] =
    POST(database, vertexPath)
      .withBody(VertexCollectionCreate(collection))
      .execute[ArangoResponse[GraphInfo.Response], Encoder, Decoder]
      .map(_.result.graph)

  def removeVertexCollection(
      collection: CollectionName,
      dropCollection: Boolean = false
  )(using Decoder[ArangoResponse[GraphInfo.Response]]): AIO[GraphInfo] =
    DELETE(
      database,
      vertexPath.addPart(collection.unwrap),
      Map(
        "dropCollection" -> dropCollection.toString
      )
    ).execute[ArangoResponse[GraphInfo.Response], Encoder, Decoder].map(_.result.graph)

  def edgeCollections(using Decoder[ArangoResponse[GraphCollections]]): AIO[List[CollectionName]] =
    GET(database, edgePath)
      .execute[ArangoResponse[GraphCollections], Encoder, Decoder]
      .map(_.result.collections)

  def addEdgeCollection(
      collection: CollectionName,
      from: List[CollectionName],
      to: List[CollectionName]
  )(using Encoder[GraphEdgeDefinition], Decoder[ArangoResponse[GraphInfo.Response]]): AIO[GraphInfo] =
    POST(database, edgePath)
      .withBody(GraphEdgeDefinition(collection, from, to))
      .execute[ArangoResponse[GraphInfo.Response], Encoder, Decoder]
      .map(_.result.graph)

  def replaceEdgeCollection(
      collection: CollectionName,
      from: List[CollectionName],
      to: List[CollectionName]
  )(using Encoder[GraphEdgeDefinition], Decoder[ArangoResponse[GraphInfo.Response]]): AIO[GraphInfo] =
    PUT(database, edgePath.addPart(collection.unwrap))
      .withBody(GraphEdgeDefinition(collection, from, to))
      .execute[ArangoResponse[GraphInfo.Response], Encoder, Decoder]
      .map(_.result.graph)

  def removeEdgeCollection(
      collection: CollectionName,
      dropCollections: Boolean = false
  )(using Decoder[ArangoResponse[GraphInfo.Response]]): AIO[GraphInfo] =
    DELETE(
      database,
      edgePath.addPart(collection.unwrap),
      Map(
        "dropCollections" -> dropCollections.toString
      )
    ).execute[ArangoResponse[GraphInfo.Response], Encoder, Decoder].map(_.result.graph)

  def vertex(collectionName: CollectionName): ArangoVertexCollection[Encoder, Decoder] =
    new ArangoVertexCollection[Encoder, Decoder](database, name, collectionName)

  def vertexDocument(handle: DocumentHandle): ArangoVertexDocument[Encoder, Decoder] =
    new ArangoVertexDocument[Encoder, Decoder](database, name, handle)

  def edge(collectionName: CollectionName): ArangoEdgeCollection[Encoder, Decoder] =
    new ArangoEdgeCollection[Encoder, Decoder](database, name, collectionName)

  def edgeInstance(handle: DocumentHandle): ArangoEdgeDocument[Encoder, Decoder] =
    new ArangoEdgeDocument[Encoder, Decoder](database, name, handle)
