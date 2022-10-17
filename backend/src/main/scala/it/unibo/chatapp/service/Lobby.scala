/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp.service

import it.unibo.chatapp.Room
import it.unibo.chatapp.RoomId
import it.unibo.chatapp.UUID
import it.unibo.chatapp.User
import it.unibo.chatapp.UserId
import it.unibo.chatapp.repository.RoomRepository
import it.unibo.chatapp.service
import zio.IO
import zio.Random
import zio.Ref
import zio.ULayer
import zio.URLayer
import zio.ZIO
import zio.ZLayer
import zio.stream.Stream
import zio.stream.SubscriptionRef
import zio.stream.UStream
import zio.stream.ZSink
import zio.stream.ZStream

import java.io.IOException
import java.net.URL

/**
 * Represents the [[Room]] management service, it takes care of available
 * [[Room]]s and connected [[User]]s.
 */
trait Lobby:
  /**
   * Adds new [[Room]] within the service.
   *
   * @param name
   *   the [[Room]] name.
   * @param description
   *   the [[Room]] description.
   * @return
   *   a [[ZIO]] describing the created [[Room]] in case of success, a failure
   *   of type [[IOException]] otherwise.
   */
  def addRoom(name: String, description: String): IO[IOException, Room]

  /**
   * Joins a [[User]] to a specified [[Room]].
   *
   * @param roomId
   *   identifies the [[Rooms]] of interest.
   * @param userId
   *   identifies the [[User]] who wants to join.
   * @return
   *   [[Unit]] in case of success, a failure of type [[NoSuchElementException]]
   *   otherwise.
   */
  def joinRoom(roomId: RoomId, userId: UserId): IO[NoSuchElementException, Unit]

  /**
   * Disconnect a [[User]] from the [[Room]].
   *
   * @param userId
   *   identifies the [[User]] who wants leave.
   * @return
   *   [[Unit]] in case of success, a failure of type [[IOException]] otherwise.
   */
  def leaveRoom(userId: UserId): IO[IOException, Unit]

  /**
   * Given a [[RoomId]], it exposes the stream of connected [[User]]s.
   *
   * @param roomId
   *   identifies the [[Room]] of interest.
   * @return
   *   the stream containing the number of connected [[User]]s.
   */
  def connectedUsers(roomId: RoomId): UStream[Long]

  /**
    * Exposes the stream of available [[Room]]s.
    *
    * @return
    *   the stream fed with [[Room]]s.
    */
  def availableRooms: UStream[Room]

/**
 * A concrete implementation of [[Lobby]] service.
 *
 * @param roomRepository
 *   storage used retrieve and add new [[Room]]s.
 * @param users
 *   structure which keeps in memory all the connected [[User]]s.
 */
case class LobbyLive(
  private val roomRepository: RoomRepository,
  private val users: SubscriptionRef[Map[UserId, RoomId]]
) extends Lobby:

  override def addRoom(
    name: String,
    description: String
  ): IO[IOException, Room] =
    for
      uuid <- Random.nextUUID.map(UUID.fromJavaUUID)
      room <- ZIO.succeed(Room(uuid, name, description))
      _    <- roomRepository.create(room)
    yield room

  override def joinRoom(
    roomId: RoomId,
    userId: UserId
  ): IO[NoSuchElementException, Unit] =
    roomRepository.get(roomId) *> users.update(_ + ((userId, roomId)))

  override def leaveRoom(userId: UserId): IO[IOException, Unit] =
    users.update(_ - userId)

  override def connectedUsers(roomId: RoomId): UStream[Long] =
    users.changes.map(_.values.filter(_ == roomId).size)

  override def availableRooms: UStream[Room] =
    ZStream.fromIterableZIO(roomRepository.all)

object Lobby:

  /**
    * The production version of the [[Lobby]] service.
    */
  val live: URLayer[RoomRepository, Lobby] = ZLayer.scoped {
    for
      repository <- ZIO.service[RoomRepository]
      init       <- SubscriptionRef.make(Map.empty)
    yield LobbyLive(repository, init)
  }

  def addRoom(
    name: String,
    description: String
  ): ZIO[Lobby, IOException, Room] =
    ZIO.environmentWithZIO(_.get.addRoom(name, description))

  def joinRoom(
    roomId: RoomId,
    userId: UserId
  ): ZIO[Lobby, NoSuchElementException, Unit] =
    ZIO.environmentWithZIO(_.get.joinRoom(roomId, userId))

  def leaveRoom(userId: UserId): ZIO[Lobby, IOException, Unit] =
    ZIO.environmentWithZIO(_.get.leaveRoom(userId))

  def connectedUsers(roomId: RoomId): ZStream[Lobby, Nothing, Long] =
    ZStream.environmentWithStream(_.get.connectedUsers(roomId))

  def availableRooms: ZStream[Lobby, Throwable, Room] =
    ZStream.environmentWithStream(_.get.availableRooms)
