package it.unibo.chatapp

import zio.Random
import zio.UIO
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

// Originally a opaque type.
// see: github.com/zio/zio-json/issues/444
type UUID = String

object UUID:
  def unsafe(s: String): UUID = s

  def fromJavaUUID(uuid: java.util.UUID): UUID = unsafe(uuid.toString)

type UserId    = UUID
type RoomId    = UUID
type MessageId = UUID

/**
 * Represents any domain concept that can be uniquely identified by an id.
 */
trait Entity[Id <: UUID]:
  def id: Id

/**
 * Represents a chat user.
 *
 * @param id
 *   unique user identifier.
 * @param name
 *   a human-readable name.
 */
case class User(override val id: UserId, name: String) extends Entity[UserId]

object User:

  implicit val decoder: JsonDecoder[User] =
    DeriveJsonDecoder.gen[User]

  implicit val encoder: JsonEncoder[User] =
    DeriveJsonEncoder.gen[User]

/**
 * Represents a chat room which allows connected users to communicate with each
 * other.
 *
 * @param id
 *   unique room identifier.
 * @param name
 *   name assigned to the room.
 * @param description
 *   description associated with the room.
 */
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

/**
 * Represents a message from a user exchanged within a chat
 */
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
