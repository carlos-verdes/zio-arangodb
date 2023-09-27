/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb
package http

import java.nio.charset.Charset

import zio.*
import zio.http.Client
import zio.json.*
import zio.stream.*
import zio.test.*
import zio.test.Assertion.*
import io.funkode.arangodb.http.Main.Rel
import model.*
import IndexGeoFields.*
import protocol.*
import ArangoMessage.*
import io.funkode.velocypack.{VPack, VPackEncoder}
import zio.json.ast.Json

trait ArangoExamples:

  import JsonCodecs.given
  import VPack.*
  import VPackEncoder.given

  case class Country(flag: String, name: String)
  case class Pet(name: String, age: Int)
  case class PatchAge(_key: DocumentKey, age: Int)
  case class PetWithKey(_key: DocumentKey, name: String, age: Int)
  case class CreatedPet(`new`: PetWithKey)
  case class Rel(_rel: String, _from: DocumentHandle, _to: DocumentHandle)
  case class Edge(_key: DocumentKey, _rel: String, _from: DocumentHandle, _to: DocumentHandle)
  case class Car(_key: String, make: String, model: String)
  case class Customer(_key: String, name: String)

  given JsonCodec[Country] = DeriveJsonCodec.gen[Country]
  given JsonCodec[Pet] = DeriveJsonCodec.gen[Pet]
  given JsonCodec[PatchAge] = DeriveJsonCodec.gen[PatchAge]
  given JsonCodec[PetWithKey] = DeriveJsonCodec.gen[PetWithKey]
  given JsonCodec[CreatedPet] = DeriveJsonCodec.gen[CreatedPet]
  given JsonCodec[Rel] = DeriveJsonCodec.gen[Rel]
  given JsonCodec[Edge] = DeriveJsonCodec.gen[Edge]
  given JsonCodec[Car] = DeriveJsonCodec.gen[Car]
  given JsonCodec[Customer] = DeriveJsonCodec.gen[Customer]

  val testDatabaseName = DatabaseName("ittestdb")
  val testDatabase = DatabaseInfo(testDatabaseName.unwrap, testDatabaseName.unwrap, "", false)

  val randomCollection = CollectionName("someRandomCol")
  val petsCollection = CollectionName("pets")
  val pets2Collection = CollectionName("pets2")
  val pets3Collection = CollectionName("pets3")
  val streamCollection = CollectionName("stream-test")

  val someIndexName = IndexName("someIndex")

  val pet1 = Pet("dog", 2)
  val pet2 = Pet("cat", 3)
  val pet3 = Pet("hamster", 4)
  val pet4 = Pet("fish", 5)
  val pet5 = Pet("turtle", 6)
  val pet5WithKey = PetWithKey(DocumentKey("turtle:6"), "turtle", 6)
  val pet6 = Pet("rabbit", 7)
  val pet6WithKey = PetWithKey(DocumentKey("rabbit:7"), "rabbit", 7)

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
  val geography = GraphName("geography")
  val allies = CollectionName("allies")
  val geographyCollection = CollectionName("geography")
  val geographyCollection2 = CollectionName("geography2")
  val countries = CollectionName("countries")
  val continents = CollectionName("continents")
  val continents2 = CollectionName("continents2")
  val oceans = CollectionName("oceans")
  val oceans2 = CollectionName("oceans2")
  val graphEdgeDefinitions = List(GraphEdgeDefinition(allies, List(countries), List(countries)))
  val graphEdgeDefinitionsGeography = List(
    GraphEdgeDefinition(geographyCollection, List(continents), List(continents))
  )
  val graphEdgeDefinitionsGeography2 = List(
    GraphEdgeDefinition(geographyCollection2, List(continents2), List(continents2))
  )
  val es = DocumentHandle(countries, DocumentKey("ES"))
  val fr = DocumentHandle(countries, DocumentKey("FR"))
  val us = DocumentHandle(countries, DocumentKey("US"))
  def alliesOfEs =
    List(
      Edge(DocumentKey("es-ally-fr"), "ally", es, fr),
      Edge(DocumentKey("es-ally-us"), "ally", es, us),
      Edge(DocumentKey("us-ally-fr"), "ally", us, fr)
    )
  val expectedAllies =
    List(Country("\uD83C\uDDEB\uD83C\uDDF7", "France"), Country("\uD83C\uDDFA\uD83C\uDDF8", "United States"))

  val dealershipGraphName = GraphName("dealership")
  val customersCollection = CollectionName("clients")
  val carsCollection = CollectionName("cars")
  val customerRelsEdgeCollection = CollectionName("cars-rels")

  extension (car: Car) def docHandle: DocumentHandle = DocumentHandle(carsCollection, DocumentKey(car._key))

  extension (customer: Customer)
    def docHandle: DocumentHandle = DocumentHandle(customersCollection, DocumentKey(customer._key))

  val carHonda = Car("123", "Honda", "Civic")
  val carMercedes = Car("1", "Mercedes", "GLE")
  val carFord = Car("2", "Ford", "Ecosport")

  val customerRoger = Customer("abc", "Roger")

object ArangoJsonIT extends ZIOSpecDefault with ArangoExamples:

  import JsonCodecs.given
  import VPack.*
  import docker.ArangoContainer

  override def spec: Spec[TestEnvironment, Any] =
    suite("ArangoDB client should")(
      test("Get server info") {
        for
          serverVersion <- ArangoClientJson.serverInfo().version()
          serverVersionFull <- ArangoClientJson.serverInfo().version(true)
          databases <- ArangoClientJson.serverInfo().databases
        yield assertTrue(serverVersion == ServerVersion("arango", "community", "3.10.1")) &&
          assertTrue(serverVersionFull.version == "3.10.1") &&
          assertTrue(serverVersionFull.details.get("architecture") == Some("64bit")) &&
          assertTrue(serverVersionFull.details.get("mode") == Some("server")) &&
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
          documents <- ArangoClientJson.collection(petsCollection).createIfNotExist().documents
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
        yield assertTrue(inserted1.`new` == Some(pet1)) &&
          assertTrue(inserted2.`new` == Some(pet2)) &&
          assertTrue(insertedCount == 2L) &&
          assertTrue(created.map(_.`new`) == morePets.map(Some(_))) &&
          assertTrue(countAfterCreated == 4L) &&
          assertTrue(updatedDocs.length == 1) &&
          assertTrue(updatedDocs.head.`new` == Some(updatedPet2)) &&
          assertTrue(countAfterUpdate == 4L) &&
          assertTrue(deletedDocs.length == 1) &&
          assertTrue(deletedDocs.head._key == inserted1._key) &&
          assertTrue(countAfterDelete == 3L)
      },
      test("Create transaction and commit") {
        for
          createdCollection <- ArangoClientJson.collection(pets3Collection).createIfNotExist()
          documents = createdCollection.documents
          transactions <- ArangoClientJson.transactions
          tx <- transactions.begin(
            write = List(pets3Collection),
            read = List(pets3Collection),
            waitForSync = true
          )
          inserted1 <- documents.insert(
            document = pet5WithKey,
            waitForSync = true,
            returnNew = true,
            transaction = Some(tx.id)
          )
          notFound1 <- createdCollection.document(pet5WithKey._key).head().flip
          inserted2 <- documents.insert(
            document = pet6WithKey,
            waitForSync = true,
            returnNew = true,
            transaction = Some(tx.id)
          )
          notFound2 <- createdCollection.document(pet6WithKey._key).head().flip
          _ <- tx.commit
          found1 <- createdCollection.document(pet5WithKey._key).read[Pet]()
          found2 <- createdCollection.document(pet6WithKey._key).read[Pet]()
          _ <- ArangoClientJson.collection(pets3Collection).drop()
        yield assertTrue(inserted1.`new`.contains(pet5WithKey)) &&
          assertTrue(inserted2.`new`.contains(pet6WithKey)) &&
          assertTrue(notFound1 match
            case ArangoError(404, _, _, _) => true
            case _                         => false
          ) &&
          assertTrue(notFound2 match
            case ArangoError(404, _, _, _) => true
            case _                         => false
          ) &&
          assertTrue(found1 == pet5) &&
          assertTrue(found2 == pet6)
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
      test("Manage collection doesn't exist when fetching document") {
        for
          invalidCollection <- ArangoClientJson.collection(CollectionName("collectionDoesntExist"))
          error <- invalidCollection
            .document(DocumentKey("turtle"))
            .read[Pet]()
            .flip
        yield assertTrue(
          error == ArangoError(404L, true, "collection or view not found: collectionDoesntExist", 1203L)
        )
      },
      test("Manage document doesn't exist when fetching document") {
        for
          countriesCollection <- ArangoClientJson.collection(CollectionName("countries"))
          error <- countriesCollection
            .document(DocumentKey("turtle"))
            .read[Pet]()
            .flip
        yield assertTrue(error == ArangoError(404L, true, "document not found", 1202L))
      },
      test("Manage document doesn't exist when fetching document head") {
        for
          countriesCollection <- ArangoClientJson.collection(CollectionName("countries"))
          error <- countriesCollection
            .document(DocumentKey("turtle"))
            .head()
            .flip
        yield assertTrue(error == ArangoError(404L, true, "Incorrect status from server: 404. Body: ", -1))
      },
      test("Manage document doesn't exist when fetching raw documents") {
        for
          countriesCollection <- ArangoClientJson.collection(CollectionName("countries"))
          error <- countriesCollection
            .document(DocumentKey("turtle"))
            .readRaw()
            .via(ZPipeline.utf8Decode)
            .run(ZSink.collectAll)
            .flip
        yield assertTrue(error == ArangoError(404L, true, "document not found", 1202L))
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
          alliesCol = ArangoClientJson.db.collection(allies).createEdgeIfNotExist()
          _ <- alliesCol.documents.create(alliesOfEs)
          queryAlliesOfSpain =
            ArangoClientJson.db
              .query(
                Query("FOR c IN OUTBOUND @startVertex @@edge RETURN c")
                  .bindVar("startVertex", VString(es.unwrap))
                  .bindVar("@edge", VString(allies.unwrap))
              )
          resultQuery <- queryAlliesOfSpain.execute[Country].map(_.result)
          vertexCollections <- graph.vertexCollections
          _ <- graph
            .edgeInstance(DocumentHandle(allies, alliesOfEs.head._key))
            .remove[Country](returnOld = true)
          resultQueryAfterUnlink <- queryAlliesOfSpain.execute[Country].map(_.result)
        yield assertTrue(graphCreated.name == politics) &&
          assertTrue(graphCreated.edgeDefinitions == graphEdgeDefinitions) &&
          assertTrue(resultQuery.sortBy(_.name) == expectedAllies.sortBy(_.name)) &&
          assert(vertexCollections)(hasSameElements(List(countries))) &&
          assertTrue(resultQueryAfterUnlink.sortBy(_.name) == expectedAllies.tail.sortBy(_.name))
      },
      test("Delete edge when vertex document is deleted") {
        for
          graph <- ArangoClientJson.graph(dealershipGraphName)
          _ <- graph.create()
          _ <- graph.addVertexCollection(carsCollection)
          _ <- graph.addVertexCollection(customersCollection)
          _ <- graph.addEdgeCollection(
            customerRelsEdgeCollection,
            List(customersCollection),
            List(carsCollection)
          )
          cars = graph.vertex(carsCollection)
          customers = graph.vertex(customersCollection)
          customerRels = graph.edge(customerRelsEdgeCollection)
          _ <- customers.createVertexDocument(customerRoger)
          _ <- cars.createVertexDocument(carMercedes)
          _ <- cars.createVertexDocument(carFord)
          _ <- customerRels.createEdgeDocument(Rel("owns", customerRoger.docHandle, carMercedes.docHandle))
          _ <- customerRels.createEdgeDocument(Rel("owns", customerRoger.docHandle, carFord.docHandle))
          carsOwnedByRogerQuery =
            ArangoClientJson.db
              .query(
                Query("FOR c IN OUTBOUND @startVertex @@edge RETURN c")
                  .bindVar("startVertex", VString(customerRoger.docHandle.unwrap))
                  .bindVar("@edge", VString(customerRelsEdgeCollection.unwrap))
              )
          ownerWithTwoCars <- carsOwnedByRogerQuery.execute[Car].map(_.result)
          _ <- graph.vertexDocument(carMercedes.docHandle).remove[Car]()
          ownerSoldFirstCar <- carsOwnedByRogerQuery.execute[Car].map(_.result)
        yield assertTrue(ownerWithTwoCars.sortBy(_._key) == List(carMercedes, carFord)) &&
          assertTrue(ownerSoldFirstCar.sortBy(_._key) == List(carFord))
      },
      test("Add vertex collection to the graph") {
        for
          graph <- ArangoClientJson.graph(geography)
          _ <- graph.create(graphEdgeDefinitionsGeography)
          _ <- graph.addVertexCollection(oceans)
          collections <- graph.vertexCollections
        yield assert(collections)(hasSameElements(List(oceans, continents)))
      },
      test("Remove vertex collection") {
        for
          graph <- ArangoClientJson.graph(GraphName("geography2"))
          _ <- graph.create(graphEdgeDefinitionsGeography2)
          _ <- graph.addVertexCollection(oceans2)
          collectionsBefore <- graph.vertexCollections
          _ <- graph.removeVertexCollection(oceans2)
          collectionsAfter <- graph.vertexCollections
        yield assertTrue(collectionsBefore != collectionsAfter) && assertTrue(
          collectionsAfter == List(continents2)
        )
      },
      test("Save and retrieve document from byte array stream") {
        for
          createdCollection <- ArangoClientJson.collection(streamCollection).create()
          documents = createdCollection.documents
          key = DocumentKey("tobby")
          document = createdCollection.document(key)
          beforeCount <- documents.count()
          documentStream = ZStream.fromIterable("""{
              |  "_key": "tobby",
              |  "name": "Petehar",
              |  "age": 12
              |}""".stripMargin.getBytes(Charset.forName("UTF-8")))
          createdStream = documents.insertRaw(documentStream, true, true)
          createdParsed <- JsonDecoder[CreatedPet].decodeJsonStreamInput(createdStream)
          // createdParsed <- JsonDecoder[CreatedPet].decodeJsonStreamInput(created)
          insertedCount <- documents.count()
          fetchedParsed <- JsonDecoder[PetWithKey].decodeJsonStreamInput(document.readRaw())
          // fetchedParsed <- JsonDecoder[PetWithKey].decodeJsonStreamInput(fetched)
          head <- document.head()
          deletedDoc <- document.remove[Pet](true)
          countAfterDelete <- documents.count()
          _ <- createdCollection.drop()
        yield assertTrue(beforeCount == 0L) &&
          assertTrue(createdParsed.`new` == PetWithKey(key, "Petehar", 12)) &&
          assertTrue(insertedCount == 1L) &&
          assertTrue(fetchedParsed == PetWithKey(key, "Petehar", 12)) &&
          assertTrue(head match
            case Header.Response(_, _, code, _) => code == 200
            case _                              => false
          ) &&
          assertTrue(deletedDoc._key == key) &&
          assertTrue(countAfterDelete == 0L)
      },
      test("Create geo index with one field") {
        val collectionName = CollectionName("c1")
        for
          collection <- ArangoClientJson.collection(collectionName).create()
          indexes = collection.indexes
          infoFromCreate <- indexes.create(
            IndexCreate.Geo.location(someIndexName, "loc", Some(true))
          )
          maybeInfoFromIndexes <- indexes.infos.map(_.find(_.name == someIndexName))
          _ <- collection.drop()
        yield
          val expectedInfo = IndexInfo.Geo(
            IndexHandle(collectionName, infoFromCreate.handle.id),
            someIndexName,
            Location("loc"),
            true
          )
          assertTrue(infoFromCreate == expectedInfo) &&
          assertTrue(maybeInfoFromIndexes.contains(expectedInfo))
      },
      test("Create geo index with two fields") {
        val collectionName = CollectionName("c2")
        for
          collection <- ArangoClientJson.collection(collectionName).create()
          indexes = collection.indexes
          infoFromCreate <- indexes.create(
            IndexCreate.Geo.latLong(someIndexName, "lat", "long")
          )
          maybeInfoFromIndexes <- indexes.infos.map(_.find(_.name == someIndexName))
          _ <- collection.drop()
        yield
          val expectedInfo = IndexInfo.Geo(
            IndexHandle(collectionName, infoFromCreate.handle.id),
            someIndexName,
            LatLong("lat", "long"),
            false
          )
          assertTrue(infoFromCreate == expectedInfo) &&
          assertTrue(maybeInfoFromIndexes.contains(expectedInfo))
      },
      test("Get index by name") {
        val collectionName = CollectionName("c3")
        for
          collection <- ArangoClientJson.collection(collectionName).createEdge()
          maybeIndex <- collection.findIndexByName(IndexName.edge)
          index = maybeIndex.get
          info <- index.info
          _ <- collection.drop()
        yield assertTrue(
          info == IndexInfo.Edge(
            index.handle,
            IndexName.edge
          )
        )
      },
      test("Drop index") {
        for
          collection <- ArangoClientJson.collection(CollectionName("c4")).create()
          indexes = collection.indexes
          infoFromCreate <- indexes.create(IndexCreate.Geo.location(someIndexName, "loc"))
          indexesCountBeforeDelete <- indexes.infos.map(_.length)
          foundIndexBeforeDelete <- collection.findIndexByName(someIndexName)
          index = collection.index(infoFromCreate.handle.id)
          handleFromDrop <- index.drop
          indexesCountAfterDelete <- indexes.infos.map(_.length)
          foundIndexAfterDelete <- collection.findIndexByName(someIndexName)
          _ <- collection.drop()
        yield assertTrue(indexesCountBeforeDelete == indexesCountAfterDelete + 1) &&
          assertTrue(handleFromDrop == infoFromCreate.handle) &&
          assertTrue(foundIndexBeforeDelete.isDefined) &&
          assertTrue(foundIndexAfterDelete.isEmpty)
      }
    ).provideShared(
      ArangoConfiguration.default,
      Client.default,
      ArangoClientJson.testContainers
    ) // @@ TestAspect.sequential
