/*
 * TODO: License goes here!
 */
package io.funkode.arangodb.model

import zio.*

case class ArangoError(code: Long, error: Boolean, errorMessage: String, errorNum: Long) extends Throwable:
  override def getMessage: _root_.java.lang.String =
    s"ArangoError(code: $code, num: $errorNum, message: $errorMessage)"

object ArangoError:

  val NotFound = 400
  val Conflict = 409

  extension [R, O](arangoService: ZIO[R, ArangoError, O])
    def ifNotFound(f: => ZIO[R, ArangoError, O]) =
      arangoService.catchSome { case ArangoError(NotFound, true, _, _) => f }

    def ifConflict(f: => ZIO[R, ArangoError, O]) =
      arangoService.catchSome { case ArangoError(Conflict, true, _, _) => f }
