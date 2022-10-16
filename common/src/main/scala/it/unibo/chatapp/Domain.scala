package it.unibo.chatapp

import zio.Random
import zio.UIO
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

object Domain:
  // Originally a opaque type.
  // see: github.com/zio/zio-json/issues/444
  type UUID = String

  object UUID:
    def unsafe(s: String): UUID = s

    def fromJavaUUID(uuid: java.util.UUID): UUID =
      unsafe(uuid.toString)

  type UserId = UUID
  type RoomId = UUID
  type MessageId = UUID

  trait Entity[Id <: UUID]:
    def id: Id

  case class User(override val id: UserId, name: String) extends Entity[UserId]

  object User:
    implicit val decoder: JsonDecoder[User] =
      DeriveJsonDecoder.gen[User]

    implicit val encoder: JsonEncoder[User] =
      DeriveJsonEncoder.gen[User]

  case class Room(
      override val id: RoomId,
      name: String,
      description: String
  ) extends Entity[RoomId]

  object Room:
    implicit val decoder: JsonDecoder[Room] =
      DeriveJsonDecoder.gen[Room]

    implicit val encoder: JsonEncoder[Room] =
      DeriveJsonEncoder.gen[Room]

  enum Message:
    case Announce(override val id: MessageId, body: String)
        extends Message
        with Entity[MessageId]
    case Text(override val id: MessageId, owner: User, body: String)
        extends Message
        with Entity[MessageId]

  object Message:
    implicit val decoder: JsonDecoder[Message] =
      DeriveJsonDecoder.gen[Message]

    implicit val encoder: JsonEncoder[Message] =
      DeriveJsonEncoder.gen[Message]
