package io.funkode.arangodb.protocol

// only needed for VelocyStream
enum MessageType(value: Int, show: String):
  case Request extends MessageType(1, MessageTypeConstants.Request)
  case ResponseFinal extends MessageType(2, MessageTypeConstants.ResponseFinal)
  // case ResponseChunk extends MessageType(3, RESPONSE_CHUNK)
  case Authentication extends MessageType(1000, MessageTypeConstants.Authentication)

object MessageTypeConstants:

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val Request = "request"

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val ResponseFinal = "response-final"

  // @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  // val RESPONSE_CHUNK = "response-chunk"

  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
  val Authentication = "authentication"
