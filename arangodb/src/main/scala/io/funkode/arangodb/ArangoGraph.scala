/*
 * TODO: License goes here!
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

  def vertexCollections(using Decoder[ArangoResult[GraphCollections]]): AIO[List[CollectionName]] =
    GET(database, vertexPath)
      .executeIgnoreResult[GraphCollections, Encoder, Decoder]
      .map(_.collections)

  def addVertexCollection(
    collection: CollectionName
  )(using Encoder[VertexCollectionCreate], Decoder[ArangoResult[GraphInfo.Response]]): AIO[GraphInfo] =
    POST(database, vertexPath)
      .withBody(VertexCollectionCreate(collection))
      .executeIgnoreResult[GraphInfo.Response, Encoder, Decoder]
      .map(_.graph)

  def removeVertexCollection(
    collection: CollectionName,
    dropCollection: Boolean = false
  )(using Decoder[ArangoResult[GraphInfo.Response]]): AIO[GraphInfo] =
    DELETE(
      database,
      vertexPath.addPart(collection.unwrap),
      Map(
        "dropCollection" -> dropCollection.toString
      )
    ).executeIgnoreResult[GraphInfo.Response, Encoder, Decoder].map(_.graph)

  def collection(collection: CollectionName): ArangoGraphCollection[Encoder, Decoder] =
    new ArangoGraphCollection[Encoder, Decoder](database, name, collection)

  def vertex(handle: DocumentHandle): ArangoGraphVertex[Encoder, Decoder] =
    new ArangoGraphVertex[Encoder, Decoder](database, name, handle)

  def edge(handle: DocumentHandle): ArangoGraphEdge[Encoder, Decoder] =
    new ArangoGraphEdge[Encoder, Decoder](database, name, handle)
