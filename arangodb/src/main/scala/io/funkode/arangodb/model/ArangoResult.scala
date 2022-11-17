package io.funkode.arangodb.model

case class ArangoResult[T](error: Boolean, code: Int, result: T)
