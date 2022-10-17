/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp.http

import it.unibo.chatapp.ClientActionCommand
import it.unibo.chatapp.JoinRoom
import it.unibo.chatapp.LeaveRoom
import it.unibo.chatapp.RoomId
import it.unibo.chatapp.SendTextMessage
import it.unibo.chatapp.ServerCommand
import it.unibo.chatapp.ServerRoomCommand
import it.unibo.chatapp.User
import it.unibo.chatapp.service.Auth
import it.unibo.chatapp.service.Chat
import it.unibo.chatapp.service.Lobby
import zhttp.http.Response
import zhttp.http.Response.apply
import zhttp.service.Client
import zio.Scope
import zio.UIO
import zio.URLayer
import zio.ZIO
import zio.ZLayer
import zio.stream.Stream
import zio.stream.UStream
import zio.stream.ZSink
import zio.stream.ZStream

import java.io.IOException

/**
 * Represents the entry point of the application functionalities. It implements
 * virtual communication channels by exploiting the abstraction offered by
 * [[zio.ZStream]].
 */
trait Controller:

  /**
   * Subscribes an observer in order to share bootstrap information.
   *
   * @return
   *   a stream of [[ServerCommand]] which wraps data such as the [[Room]] list.
   */
  def subscribe: UStream[ServerCommand]

  /**
   * Subscribes an observer to a specific [[Room]] by id.
   *
   * @param roomId
   *   identifies the [[Room]] to subscribe to.
   * @return
   *   a stream of [[ServerRoomCommand]] which wraps information about the
   *   [[Room]].
   */
  def subscribeRoom(
    roomId: RoomId
  ): ZStream[Scope, NoSuchElementException, ServerRoomCommand]

  /**
   * It handles the client actions interpreting the received command.
   *
   * @param command
   *   a specific [[ClientActionCommand]] that must be handled.
   * @return
   *   a stream of [[Unit]] in case of success, a controlled failure otherwise.
   */
  def handleClientCommand(
    command: ClientActionCommand
  ): Stream[NoSuchElementException | IOException, Unit]

  /**
   * Registers the client by username.
   *
   * @param username
   *   name that identifies the [[User]] within the service.
   * @return
   *   a description of the created [[User]].
   */
  def signup(username: String): UIO[User]

/**
 * A Concrete implementation of [[Controller]] service.
 *
 * @param auth
 *   the [[Auth]] service.
 * @param chat
 *   the [[Chat]] service.
 * @param lobby
 *   the [[Lobby]] service.
 */
class ControllerLive(val auth: Auth, val chat: Chat, val lobby: Lobby)
  extends Controller:

  override def subscribe: UStream[ServerCommand] =
    lobby.availableRooms
      .aggregateAsync(ZSink.collectAllToSet)
      .map(ServerCommand.SendAvailableRoms.apply)

  override def subscribeRoom(
    roomId: RoomId
  ): ZStream[Scope, NoSuchElementException, ServerRoomCommand] =
    ZStream.mergeAllUnbounded()(
      lobby
        .connectedUsers(roomId)
        .map(ServerRoomCommand.SendConnectedUsers.apply),
      chat.roomMessages(roomId).map(ServerRoomCommand.SendRoomMessage.apply)
    )

  override def handleClientCommand(
    command: ClientActionCommand
  ): Stream[NoSuchElementException | IOException, Unit] =
    command match
      case JoinRoom(roomId, userId) =>
        ZStream.fromZIO(lobby.joinRoom(roomId, userId))

      case LeaveRoom(userId) =>
        ZStream.fromZIO(lobby.leaveRoom(userId))

      case SendTextMessage(roomId, user, message) =>
        chat.publishMessage(roomId, user, message)

  override def signup(userName: String) = auth.register(userName)

object Controller:

  /**
   * The production version of the [[Controller]] service.
   */
  val live: URLayer[Auth & Chat & Lobby, Controller] = ZLayer.scoped {
    for
      auth <- ZIO.service[Auth]
      chat <- ZIO.service[Chat]
      room <- ZIO.service[Lobby]
    yield ControllerLive(auth, chat, room)
  }

  def subscribe: ZStream[Controller, Nothing, ServerCommand] =
    ZStream.environmentWithStream(_.get.subscribe)

  def subscribeRoom(
    roomId: RoomId
  ): ZStream[Controller & Scope, NoSuchElementException, ServerRoomCommand] =
    ZStream.environmentWithStream[Controller](_.get.subscribeRoom(roomId))

  def handleClientCommand(
    command: ClientActionCommand
  ): ZStream[Controller, NoSuchElementException | IOException, Unit] =
    ZStream.environmentWithStream(_.get.handleClientCommand(command))

  def signup(
    userName: String
  ): ZIO[Controller, Nothing, User] =
    ZIO.environmentWithZIO(_.get.signup(userName))
