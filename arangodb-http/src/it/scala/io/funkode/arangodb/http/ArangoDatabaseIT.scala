/*
 * TODO: License goes here!
 */
package io.funkode.arangodb
package http

import io.funkode.velocypack.{VPack, VPackEncoder}
import zio.*
import zio.http.Client
import zio.json.*
import zio.test.*
import zio.stream.*
import model.*
import protocol.*
import ArangoMessage.*

trait ArangoExamples:

  import JsonCodecs.given
  import VPack.*
  import VPackEncoder.given

  case class Country(flag: String, name: String) derives JsonCodec

  case class Pet(name: String, age: Int) derives JsonCodec

  case class PatchAge(_key: DocumentKey, age: Int) derives JsonCodec

  case class PetWithKey(_key: DocumentKey, name: String, age: Int) derives JsonCodec

  case class Rel(_rel: String, _from: DocumentHandle, _to: DocumentHandle) derives JsonCodec

  val testDatabaseName = DatabaseName("ittestdb")
  val testDatabase = DatabaseInfo(testDatabaseName.unwrap, testDatabaseName.unwrap, "", false)

  val randomCollection = CollectionName("someRandomCol")
  val petsCollection = CollectionName("pets")
  val pets2Collection = CollectionName("pets2")

  val pet1 = Pet("dog", 2)
  val pet2 = Pet("cat", 3)
  val pet3 = Pet("hamster", 4)
  val pet4 = Pet("fish", 5)

  val petWithKey = PetWithKey(DocumentKey("123"), "turtle", 23)
  val patchPetWithKey = PatchAge(DocumentKey("123"), 24)
  // TODO review why UPSERT doesn't behave like web interface, we should ommit name attribute
  val upsertPet = VObject("name" -> "turtle", "age" -> 30)
  val upsertedPet = VObject("name" -> "turtle", "age" -> 30)
  val newPetWithKey = PetWithKey(DocumentKey("123"), "turtle", 24)

  def patchPet(_key: DocumentKey) = PatchAge(_key, 5)

  val updatedPet2 = pet2.copy(age = 5)
  val morePets = List(pet3, pet4)

  val firstCountries = List(Country("🇦🇩", "Andorra"), Country("🇦🇪", "United Arab Emirates"))
  val secondCountries = List(Country("🇦🇫", "Afghanistan"), Country("🇦🇬", "Antigua and Barbuda"))

  val politics = GraphName("politics")
  val allies = CollectionName("allies")
  val countries = CollectionName("countries")
  val graphEdgeDefinitions = List(GraphEdgeDefinition(allies, List(countries), List(countries)))
  val es = DocumentHandle(countries, DocumentKey("ES"))
  val fr = DocumentHandle(countries, DocumentKey("FR"))
  val us = DocumentHandle(countries, DocumentKey("US"))
  val alliesOfEs = List(Rel("ally", es, fr), Rel("ally", es, us), Rel("ally", us, fr))
  val expectedAllies =
    List(Country("\uD83C\uDDEB\uD83C\uDDF7", "France"), Country("\uD83C\uDDFA\uD83C\uDDF8", "United States"))

object ArangoDatabaseIT extends ZIOSpecDefault with ArangoExamples:

  import JsonCodecs.given
  import VPack.*
  import docker.ArangoContainer

  override def spec: Spec[TestEnvironment, Any] =
    suite("ArangoDB client should")(
      test("Get server info") {
        for
          serverVersion <- ArangoClientJson.serverInfo().version()
          databases <- ArangoClientJson.serverInfo().databases
        yield assertTrue(serverVersion == ServerVersion("arango", "community", "3.7.15")) &&
          assertTrue(Set(DatabaseName.system, DatabaseName("test")).subsetOf(databases.toSet))
      },
      test("Create and drop a database") {
        for
          databaseApi <- ArangoClientJson.database(testDatabaseName).create()
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
          collection <- ArangoClientJson.collection(randomCollection).create()
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
          documents <- ArangoClientJson.collection(petsCollection).create().documents
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
          _ <- ArangoClientJson.collection(petsCollection).drop()
        yield assertTrue(inserted1.`new`.get == pet1) &&
          assertTrue(inserted2.`new`.get == pet2) &&
          assertTrue(insertedCount == 2L) &&
          assertTrue(created.map(_.`new`.get) == morePets) &&
          assertTrue(countAfterCreated == 4L) &&
          assertTrue(updatedDocs.length == 1) &&
          assertTrue(updatedDocs.head.`new`.get == updatedPet2) &&
          assertTrue(countAfterUpdate == 4L) &&
          assertTrue(deletedDocs.length == 1) &&
          assertTrue(deletedDocs.head._key == inserted1._key) &&
          assertTrue(countAfterDelete == 3L)
      },
      test("Save single document in a collection") {
        for
          createdCollection <- ArangoClientJson.collection(pets2Collection).create()
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
          db <- ArangoClientJson.database(DatabaseName("test"))
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
          graph <- ArangoClientJson.graph(politics)
          graphCreated <- graph.create(graphEdgeDefinitions)
          alliesCol = ArangoClientJson.db.collection(allies)
          _ <- alliesCol.documents.create(alliesOfEs)
          queryAlliesOfSpain =
            ArangoClientJson.db
              .query(
                Query("FOR c IN OUTBOUND @startVertex @@edge RETURN c")
                  .bindVar("startVertex", VString(es.unwrap))
                  .bindVar("@edge", VString(allies.unwrap))
              )
          resultQuery <- queryAlliesOfSpain.execute[Country].map(_.result)
        yield assertTrue(graphCreated.name == politics) &&
          assertTrue(graphCreated.edgeDefinitions == graphEdgeDefinitions) &&
          assertTrue(resultQuery == expectedAllies)
      }
    ).provideShared(
      Scope.default,
      ArangoConfiguration.default,
      Client.default,
      ArangoClientJson.testContainers
    ) // @@ TestAspect.sequential
