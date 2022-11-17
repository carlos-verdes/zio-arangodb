/*
 * TODO: License goes here!
 */
package io.funkode.arangodb.model

final case class Cursor[T](
    cached: Boolean,
    count: Option[Long] = None,
    extra: Option[Cursor.Extra] = None,
    hasMore: Boolean,
    id: Option[String] = None,
    result: List[T]
)

object Cursor:

  final case class ExtraStats(
      writesExecuted: Option[Long] = None,
      writesIgnored: Option[Long] = None,
      scannedFull: Option[Long] = None,
      scannedIndex: Option[Long] = None,
      filtered: Option[Long] = None,
      httpRequests: Option[Long] = None,
      fullCount: Option[Long] = None,
      executionTime: Option[Double] = None,
      peakMemoryUsage: Option[Long] = None
  )

  final case class Extra(
      stats: ExtraStats
  )
