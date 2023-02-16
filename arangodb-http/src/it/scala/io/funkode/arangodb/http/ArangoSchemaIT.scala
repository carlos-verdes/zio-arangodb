/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import io.funkode.velocypack.{VPack, VPackEncoder}
import zio.*
import zio.http.Client
import zio.schema.*
import zio.stream.*
import zio.test.*

import model.*
import protocol.*
import ArangoMessage.*

trait ArangoExamplesSchemas extends ArangoExamples:

  import SchemaCodecs.given

  given Schema[Country] = DeriveSchema.gen[Country]
  given Schema[Pet] = DeriveSchema.gen[Pet]
  given Schema[PatchAge] = DeriveSchema.gen[PatchAge]
  given Schema[PetWithKey] = DeriveSchema.gen[PetWithKey]
  given Schema[Rel] = DeriveSchema.gen[Rel]

object ArangoSchemaIT extends ZIOSpecDefault with ArangoExamplesSchemas:

  import SchemaCodecs.given
  import VPack.*
  import docker.ArangoContainer

  override def spec: Spec[TestEnvironment, Any] =
    suite("ArangoDB client should")(
      test("Get server info") {
        for
          serverVersion <- ArangoClientSchema.serverInfo().version()
          // serverVersionFull <- ArangoClientSchema.serverInfo().version(true)
          databases <- ArangoClientSchema.serverInfo().databases
        // TODO review the Map("" -> "") after zio-schema fix issue with default Maps
        yield assertTrue(serverVersion == ServerVersion("arango", "community", "3.10.1", Map("" -> ""))) &&
          /*
          assertTrue(serverVersionFull.version == "3.7.15") &&
          assertTrue(serverVersionFull.details.get("architecture") == Some("64bit")) &&
          assertTrue(serverVersionFull.details.get("mode") == Some("server")) &&
           */
          assertTrue(Set(DatabaseName.system, DatabaseName("test")).subsetOf(databases.toSet))
      } /*,
      test("Create and drop a database") {
        for
          databaseApi <- ArangoClientSchema.database(testDatabaseName).create()
          dataInfo <- databaseApi.info
          collections <- databaseApi.collections()
          deleteResult <- databaseApi.drop
        yield assertTrue(dataInfo.name == testDatabase.name) &&
          assertTrue(!dataInfo.isSystem) &&
          assertTrue(collections.forall(_.isSystem)) &&
          assertTrue(deleteResult)
      },
      test("Create and drop a collection (default database)") {
        for
          collection <- ArangoClientSchema.collection(randomCollection).create()
          collectionInfo <- collection.info
          collectionChecksum <- collection.checksum()
          deleteResult <- collection.drop()
        yield assertTrue(collectionInfo.name == randomCollection) &&
          assertTrue(!collectionInfo.isSystem) &&
          assertTrue(collectionChecksum.name == randomCollection) &&
          assertTrue(deleteResult.id == collectionInfo.id)
      },
      test("Save documents in a collection") {
        for
          documents <- ArangoClientSchema.collection(petsCollection).create().documents
          inserted1 <- documents.insert(pet1, true, true)
          inserted2 <- documents.insert(pet2, true, true)
          insertedCount <- documents.count()
          created <- documents.create(morePets, true, true)
          countAfterCreated <- documents.count()
          updatedDocs <- documents
            .update[Pet, PatchAge](List(patchPet(inserted2._key)), waitForSync = true, returnNew = true)
          countAfterUpdate <- documents.count()
          deletedDocs <- documents.remove[Pet, DocumentKey](List(inserted1._key), true)
          countAfterDelete <- documents.count()
          _ <- ArangoClientSchema.collection(petsCollection).drop()
        yield assertTrue(inserted1.`new` == Some(pet1)) &&
          assertTrue(inserted2.`new` == Some(pet2)) &&
          assertTrue(insertedCount == 2L) &&
          assertTrue(created.map(_.`new`) == morePets.map(Some.apply)) &&
          assertTrue(countAfterCreated == 4L) &&
          assertTrue(updatedDocs.length == 1) &&
          assertTrue(updatedDocs.head.`new` == Some(updatedPet2)) &&
          assertTrue(countAfterUpdate == 4L) &&
          assertTrue(deletedDocs.length == 1) &&
          assertTrue(deletedDocs.head._key == inserted1._key) &&
          assertTrue(countAfterDelete == 3L)
      },
      test("Save single document in a collection") {
        for
          createdCollection <- ArangoClientSchema.collection(pets2Collection).create()
          documents = createdCollection.documents
          document = createdCollection.document(petWithKey._key)
          beforeCount <- documents.count()
          created <- documents.insert(petWithKey, true, true)
          insertedCount <- documents.count()
          fetched <- document.read[PetWithKey]()
          head <- document.head()
          updated <- document
            .update[PetWithKey, PatchAge](patchPetWithKey, waitForSync = true, returnNew = true)
          countAfterUpdate <- documents.count()
          replaced <- document
            .replace[PatchAge](patchPetWithKey, waitForSync = true, returnNew = true)
          countAfterReplace <- documents.count()
          upserted <- document.upsert(upsertPet)
          countAfterUpsert <- documents.count()
          deletedDoc <- document.remove[PatchAge](true)
          countAfterDelete <- documents.count()
          _ <- createdCollection.drop()
        yield assertTrue(beforeCount == 0L) &&
          assertTrue(created.`new`.get == petWithKey) &&
          assertTrue(insertedCount == 1L) &&
          assertTrue(fetched == petWithKey) &&
          assertTrue(head match
            case Header.Response(_, _, code, _) => code == 200
            case _                              => false
          ) &&
          // `update` patches original
          assertTrue(updated.`new`.get == newPetWithKey) &&
          assertTrue(countAfterUpdate == 1L) &&
          // `replace` only stores new document
          assertTrue(replaced.`new`.get == patchPetWithKey) &&
          assertTrue(countAfterReplace == 1L) &&
          // `upsert` insert/update
          assertTrue(upserted.pure == upsertedPet) &&
          assertTrue(countAfterUpsert == 1L) &&
          assertTrue(deletedDoc._key == petWithKey._key) &&
          assertTrue(countAfterDelete == 0L)
      },
      test("Query documents with cursor") {
        for
          db <- ArangoClientSchema.database(DatabaseName("test"))
          queryCountries =
            db
              .query(
                Query("FOR c IN @@col SORT c RETURN c")
                  .bindVar("@col", VString("countries"))
              )
              .count(true)
              .batchSize(2)
          cursor <- queryCountries.cursor[Country]
          firstResults = cursor.body
          more <- cursor.next
          secondResults = more.body
          firstStreamResults <- queryCountries.stream[Country].run(ZSink.take(4))
          streamResultsCount <- queryCountries.stream[Country].run(ZSink.count)
        yield assertTrue(firstResults.count.get > 2L) &&
          assertTrue(firstResults.result == firstCountries) &&
          assertTrue(firstResults.hasMore) &&
          assertTrue(secondResults.count.get > 2L) &&
          assertTrue(secondResults.result == secondCountries) &&
          assertTrue(secondResults.hasMore) &&
          assertTrue(firstStreamResults.toList == (firstCountries ++ secondCountries)) &&
          assertTrue(streamResultsCount == 250L)
      },
      test("Create a graph and query") {
        for
          graph <- ArangoClientSchema.graph(politics)
          graphCreated <- graph.create(graphEdgeDefinitions)
          alliesCol = ArangoClientSchema.db.collection(allies)
          _ <- alliesCol.documents.create(alliesOfEs)
          queryAlliesOfSpain =
            ArangoClientSchema.db
              .query(
                Query("FOR c IN OUTBOUND @startVertex @@edge RETURN c")
                  .bindVar("startVertex", VString(es.unwrap))
                  .bindVar("@edge", VString(allies.unwrap))
              )
          resultQuery <- queryAlliesOfSpain.execute[Country].map(_.result)
        yield assertTrue(graphCreated.name == politics) &&
          assertTrue(graphCreated.edgeDefinitions == graphEdgeDefinitions) &&
          assertTrue(resultQuery == expectedAllies)
      }*/
    ).provideShared(
      Scope.default,
      ArangoConfiguration.default,
      Client.default,
      ArangoClientSchema.testContainers
    ) // @@ TestAspect.sequential
