package io.funkode.arangodb.protocol

// only needed for VelocyStream
enum RequestType(value: Int):
  case DELETE extends RequestType(0)
  case GET extends RequestType(1)
  case POST extends RequestType(2)
  case PUT extends RequestType(3)
  case HEAD extends RequestType(4)
  case PATCH extends RequestType(5)
  case OPTIONS extends RequestType(6)
