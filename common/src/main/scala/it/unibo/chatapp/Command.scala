package it.unibo.chatapp.http

import it.unibo.chatapp.Domain.Message
import it.unibo.chatapp.Domain.Room
import it.unibo.chatapp.Domain.RoomId
import it.unibo.chatapp.Domain.User
import it.unibo.chatapp.Domain.UserId
import zio.json._

object Command: 
  enum ServerCommand:
    case SendAvailableRoms(rooms: Set[Room]) extends ServerCommand

  object ServerCommand:
    implicit val decoder: JsonDecoder[ServerCommand] =
      DeriveJsonDecoder.gen[ServerCommand]
    
    implicit val encoder: JsonEncoder[ServerCommand] =
      DeriveJsonEncoder.gen[ServerCommand]

  enum ServerRoomCommand:
    case SendConnectedUsers(connectedUsers: Long) extends ServerRoomCommand
    case SendRoomMessage(message: Message) extends ServerRoomCommand

  object ServerRoomCommand:
    implicit val decoder: JsonDecoder[ServerRoomCommand] =
      DeriveJsonDecoder.gen[ServerRoomCommand]
    
    implicit val encoder: JsonEncoder[ServerRoomCommand] =
      DeriveJsonEncoder.gen[ServerRoomCommand]

  sealed trait ClientCommand
  case object Subscribe extends ClientCommand
  case class SubscribeRoom(roomId: RoomId) extends ClientCommand
  sealed trait ClientActionCommand extends ClientCommand
  case class JoinRoom(roomId: RoomId, userId: UserId) extends ClientActionCommand
  case class LeaveRoom(userId: UserId) extends ClientActionCommand
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
