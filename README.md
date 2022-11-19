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

## Scripts on this repository

Start ArangoDB with docker:
```sh
make startDockerArango
```

Run local test versus running ArangoDB instance (default port 8529):
```shell
make run
```
