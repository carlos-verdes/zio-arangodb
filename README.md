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

## Scripts on this repository

Start ArangoDB with docker:
```sh
make startDockerArango
```

Run local test versus running ArangoDB instance (default port 8529):
```shell
make run
```
