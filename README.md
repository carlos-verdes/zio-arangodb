# zio-arangodb
Main repo for zio-arangodb

## Usage

ArangoDB supports 3 type of API's:
- Velocypack over Velocystream (sockets)
- Velocypack over HTTP
- JSON over HTTP

This library is built on top of ZIO and is designed in a way it can (potentially) implement all protocols.
Current version supports JSON over HTTP only using zio-http as underlying client.

Example of usage:
```scala

import io.funkode.arangodb.*
import io.funkode.arangodb.http.*
import zio.*
import zio.Console.*
import zio.http.Client
import zio.http.Middleware.*
import zio.json.*

object Main extends ZIOAppDefault:

  import model.* // arango API types
  import JsonCodecs.given // json codecs for arango API types

  val testDb = DatabaseName("test")

  def app =
    for
      db <- ArangoClientJson.database(testDb).createIfNotExist()
      dbInfo <- db.info
      _ <- printLine(s"""Database info: $dbInfo""")
      collections <- db.collections(true)
      _ <- printLine(s"""Database collections: ${collections.map(_.name).mkString(", ")}""")
      _ <- printLine(s"""Press any key to exit""")
      _ <- readLine
    yield ()

  def run = app.provide(
    ArangoConfiguration.default, // check reference.conf (HOCON)
    ArangoClientJson.live, // JSON over HTTP
    Client.default, // zio-http client 
    Scope.default
  )
```

## Testing (testcontainers / Docker)

To do IT testing you can use `ArangoClientJson.testcontainers` instead of `live`.
It will add an `ArangoContainer` layer running on a random port and a client already configured against the instance

Example from it tests (check code for more examples):
```scala

import io.funkode.arangodb.*
import io.funkode.arangodb.http.*
import io.funkode.arangodb.model.*
import io.funkode.velocypack.VPack
import zio.*
import zio.http.Client
import zio.json.*
import zio.test.*


object ArangoDatabaseIT extends ZIOSpecDefault:

import JsonCodecs.given
import VPack.*
import docker.ArangoContainer

override def spec: Spec[TestEnvironment, Any] =
  suite("ArangoDB client should")(
    test("Create and drop a database") {
      for
        container <- ZIO.service[ArangoContainer]
        _ <- Console.printLine(s"ArangoDB container running on port ${container.container.getFirstMappedPort.nn}")
        databaseApi <- ArangoClientJson.database("pets").create()
        dataInfo <- databaseApi.info
        deleteResult <- databaseApi.drop
      yield assertTrue(dataInfo.name == testDatabase.name) &&
        assertTrue(!dataInfo.isSystem) &&
        assertTrue(deleteResult)
    }).provideShared(
    Scope.default,
    ArangoConfiguration.default,
    Client.default,
    ArangoClientJson.testContainers
  )
```
## ServerInfo API [link](./arangodb/src/main/scala/io/funkode/arangodb/ArangoServer.scala)
High level methods:
- `def databases: List[DatabaseName]` - list of server databases
- `def version(details: Boolean = false): ServerVersion` - server version, license type, etc

Example of usage:
```scala
for
  databases <- ArangoClientJson.serverInfo().databases
yield databases
```
## Database API [link](./arangodb/src/main/scala/io/funkode/arangodb/ArangoDatabase.scala)

High level methods:
- `def name: DatabaseName` - name of current database
- `def info: DatabaseInfo` - get database info
- `def create(users: List[DatabaseCreate.User]): ArangoDatabase` - create this database, will fail if already exist
- `def createIfNotExist(users: List[DatabaseCreate.User]): ArangoDatabase`
- `def drop: Boolean` - drop current database
- `def collections: List[CollectionInfo]` - list of database collections
- `def graphs: List[GraphInfo]` - list of database graphs
- `def collection(name: CollectionName): ArangoCollection` - access to collection API

Example of usage:
```scala
val testDb = DatabaseName("test")

for
  dbApi <- ArangoClientJson.database(testDb).createIfNotExist()
  databaseInfo <- dbApi.info
  collections <- dbApi.collections()
yield (databaseInfo, collections)
```

## Collection API [link](./arangodb/src/main/scala/io/funkode/arangodb/ArangoCollection.scala)

High level methods:
- `def database: DatabaseName` - current database name
- `def name: CollectionName` - current collection name
- `def info: CollectionInfo` - collection info
- `def create(setup: CollectionCreate => CollectionCreate)` - create collection
- `def createIfNotExist(setup: CollectionCreate => CollectionCreate)` - create collection if not exist
- `def drop(isSystem: Boolean = false): DeleteResult` - drop collection

Example of usage:
```scala
for
  collection <- ArangoClientJson.collection("pets").createIfNotExist
  collectionInfo <- collection.info
yield collectionInfo
```

## Documents API [link](./arangodb/src/main/scala/io/funkode/arangodb/ArangoDocuments.scala)

High level methods:
- `def database: DatabaseName` - current database name
- `def collection: CollectionName` - current collection name
- `def count: Long` - number of documents for this collection
- `def insert[T](document: T, ...): Document[T]` - insert new document, retrieves `Document[T]` which has internal id, key and revision
- `def create[T](documents: List[T], ...): List[Document[T]]` - insert list of documents
- `def replace[T](documents: List[T], ...): List[Document[T]]` - replace list of documents
- `def update[T, P](patch: List[P], ...): List[Document[T]]` - patch list of documents
- `def remove[T, K](keys: List[K], ...): List[Document[T]]` - remove list of documents by key

Example of usage:
```scala
for
  documents <- ArangoClientJson.collection(petsCollection).createIfNotExist().documents
  document <- documents.insert(pet1, true, true)
yield
  document

```
  `
## Scripts on this repository

Start ArangoDB with docker:
```sh
make startDockerArango
```

Run local test versus running ArangoDB instance (default port 8529):
```shell
make run
```
