/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp

import it.unibo.chatapp.ServerRoomCommand.SendConnectedUsers
import it.unibo.chatapp.ServerRoomCommand.SendRoomMessage
import org.scalajs.dom
import org.scalajs.dom.HTMLTextAreaElement
import org.scalajs.dom.HttpMethod
import org.scalajs.dom.RequestInit
import org.scalajs.dom.document
import zio.json.DecoderOps

import util.chaining.scalaUtilChainingOps
import scala.scalajs.js.Promise

val baseUrl       = "localhost:8091"
val cmdSocketUrl  = s"ws://$baseUrl/subscribe"
val dataSocketUrl = s"ws://$baseUrl/subscribeRoom"
val registerUrl   = s"http://$baseUrl/signup"

object Main:

  case class AppState(
    user: User,
    roomData: Option[RoomData],
    cmdSocket: dom.WebSocket
  )

  case class RoomData(
    room: Room,
    socket: dom.WebSocket
  )

  var state: AppState = null

  def main(args: Array[String]): Unit = {
    document.addEventListener(
      "DOMContentLoaded",
      { (e: dom.Event) =>
        val roomList       = document.getElementById("room-list")
        val connectedUsers = document.getElementById("connected-users")
        val chatBox        = document.getElementById("chat-box")
        val selectedRoom   = document.getElementById("selected-room")
        val commentBar     = document.getElementById("comment-bar")
        val startMessage   = document.getElementById("start-message")
        val messageInput = document
          .getElementById("comment")
          .asInstanceOf[HTMLTextAreaElement]
        document
          .getElementById("send-container")
          .append(ButtonComponent("Send", sendMessage).tag.render)

        def sendMessage() =
          state match
            case AppState(user, Some(RoomData(room, _)), cmdSocket) =>
              cmdSocket.sendJson[ClientCommand](
                SendTextMessage(room.id, user, messageInput.value)
              )
              messageInput.value = ""
            case _ => ()

        def createDataSocket(roomId: RoomId): dom.WebSocket =
          new dom.WebSocket(dataSocketUrl)
            .tap(_.on[ServerRoomCommand] {
              case SendConnectedUsers(count) =>
                connectedUsers.innerHTML = s"connected users: $count"
              case SendRoomMessage(msg: Message.Announce) =>
                println(msg)
              case SendRoomMessage(msg: Message.Text) =>
                val ownerType =
                  if state.user == msg.owner
                  then OwnerType.Sender
                  else OwnerType.Receiver
                TextMessageComponent(msg, ownerType).tag.render
                  .tap(chatBox.appendChild(_))
                  .tap(_.scrollIntoView())
            })
            .tap(ws =>
              ws.onopen = _ =>
                ws.sendJson[ClientCommand](SubscribeRoom(roomId))
                state.cmdSocket
                  .sendJson[ClientCommand](JoinRoom(roomId, state.user.id))
            )
            .tap(_.onclose = _ => println("closing data socket..."))

        def createCmdSocket(): Either[Throwable, dom.WebSocket] =
          Right(
            new dom.WebSocket(cmdSocketUrl)
              .tap(_.on[ServerCommand] {
                case ServerCommand.SendAvailableRoms(rooms) =>
                  roomList.innerHTML = ""
                  rooms
                    .map(RoomComponent(_, onSelectRoom))
                    .map(_.tag.render)
                    .foreach(roomList.appendChild)
              })
              .tap(ws => ws.onopen = _ => ws.sendJson[ClientCommand](Subscribe))
              .tap(_.onclose = _ => println("closing command socket"))
          )

        def onSelectRoom(room: Room): Unit =
          state match {
            case AppState(
                  user,
                  Some(RoomData(_, dataSocket)),
                  commandSocket
                ) =>
              commandSocket.sendJson[ClientCommand](LeaveRoom(user.id))
              dataSocket.close()
              chatBox.innerHTML = ""
            case _ =>
              startMessage.remove()
              chatBox.removeAttribute("hidden")
              commentBar.removeAttribute("hidden")
          }
          selectedRoom.innerHTML = room.name
          state = state.copy(
            roomData = Option(RoomData(room, createDataSocket(room.id)))
          )

        val username = Util.prompt("What's your name?", "John Smith")

        import scalajs.js.Thenable.Implicits.thenable2future
        import concurrent.ExecutionContext.Implicits.global

        val requestInit = new RequestInit {
          body = username;
          method = HttpMethod.POST;
        }

        for
          response <- dom.fetch(registerUrl, requestInit)
          text     <- response.text()
          user = text
            .fromJson[User]
            .getOrElse(throw RuntimeException("Can't retrieve user."))
          cmdSocket = createCmdSocket().getOrThrow()
        yield state = AppState(
          roomData = Option.empty,
          user = user,
          cmdSocket = cmdSocket
        )
      }
    )
  }
