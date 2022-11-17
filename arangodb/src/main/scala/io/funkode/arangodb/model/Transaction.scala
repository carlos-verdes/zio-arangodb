/*
 * TODO: License goes here!
 */
package io.funkode.arangodb.model

case class Transaction(id: TransactionId, status: String)

object Transaction:
  val Key: String = "x-arango-trx-id"
