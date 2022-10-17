package it.unibo.chatapp

import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

/**
 * Represents a group of commands send from the server and containing bootstrap
 * information.
 */
enum ServerCommand:
  case SendAvailableRoms(rooms: Set[Room]) extends ServerCommand

object ServerCommand:

  implicit val decoder: JsonDecoder[ServerCommand] =
    DeriveJsonDecoder.gen[ServerCommand]

  implicit val encoder: JsonEncoder[ServerCommand] =
    DeriveJsonEncoder.gen[ServerCommand]

/**
 * Represents a group of commands send from the server and containing information
 * about a specific [[Room]].
 */
enum ServerRoomCommand:
  case SendConnectedUsers(connectedUsers: Long) extends ServerRoomCommand
  case SendRoomMessage(message: Message)        extends ServerRoomCommand

object ServerRoomCommand:

  implicit val decoder: JsonDecoder[ServerRoomCommand] =
    DeriveJsonDecoder.gen[ServerRoomCommand]

  implicit val encoder: JsonEncoder[ServerRoomCommand] =
    DeriveJsonEncoder.gen[ServerRoomCommand]

/**
 * Represents a group of commands send from the client to the server.
 */
sealed trait ClientCommand

/**
 * Represents a client subscription required to receive bootstrap information.
 */
case object Subscribe extends ClientCommand

/**
 * Represents a client subscription required to receive information about a
 * specific [[Room]]
 *
 * @param roomId
 *   identifies the [[Room]] to subscribe to.
 */
case class SubscribeRoom(roomId: RoomId) extends ClientCommand

/**
 * Specialization of [[ClientCommand]] which has an active role within the
 * application logic.
 */
sealed trait ClientActionCommand                    extends ClientCommand
case class JoinRoom(roomId: RoomId, userId: UserId) extends ClientActionCommand
case class LeaveRoom(userId: UserId)                extends ClientActionCommand

case class SendTextMessage(
  roomId: RoomId,
  user: User,
  messageBody: String
) extends ClientActionCommand

object ClientCommand:

  implicit val decoder: JsonDecoder[ClientCommand] =
    DeriveJsonDecoder.gen[ClientCommand]

  implicit val encoder: JsonEncoder[ClientCommand] =
    DeriveJsonEncoder.gen[ClientCommand]
