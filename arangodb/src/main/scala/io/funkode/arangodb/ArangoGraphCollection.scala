/*
 * TODO: License goes here!
 */
package io.funkode.arangodb

import model.*
import protocol.*
import ArangoMessage.*

class ArangoGraphCollection[Encoder[_], Decoder[_]](
    databaseName: DatabaseName,
    graphName: GraphName,
    collectionName: CollectionName
)(using
    arangoClient: ArangoClient[Encoder, Decoder]
):

  def name: CollectionName = collectionName

  def vertex(key: DocumentKey): ArangoGraphVertex[Encoder, Decoder] =
    new ArangoGraphVertex[Encoder, Decoder](databaseName, graphName, DocumentHandle(name, key))

  def edge(key: DocumentKey): ArangoGraphEdge[Encoder, Decoder] =
    new ArangoGraphEdge[Encoder, Decoder](databaseName, graphName, DocumentHandle(name, key))
