package io.funkode.arangodb.protocol

opaque type ArangoVersion = Int

object ArangoVersion:

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val Current: ArangoVersion = 1

  def apply(version: Int): ArangoVersion = 1
  extension (version: ArangoVersion) def unwrap: Int = version
