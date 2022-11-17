package io.funkode.arangodb.model

case class DatabaseCreate(
    name: DatabaseName,
    users: List[DatabaseCreate.User] = List.empty,
    options: Map[String, String] = Map.empty
)

object DatabaseCreate:

  final case class User(
      username: String,
      passwd: Option[String] = None,
      active: Boolean = true
      //  extra: Option[Any],
  )
