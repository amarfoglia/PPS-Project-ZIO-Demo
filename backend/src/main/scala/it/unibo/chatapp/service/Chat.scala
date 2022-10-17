/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp.service

import it.unibo.chatapp.Message
import it.unibo.chatapp.RoomId
import it.unibo.chatapp.UUID
import it.unibo.chatapp.User
import it.unibo.chatapp.repository.RoomRepository
import zio.Console
import zio.Hub
import zio.IO
import zio.Queue
import zio.Random
import zio.Ref
import zio.Scope
import zio.Task
import zio.UIO
import zio.ULayer
import zio.URLayer
import zio.ZIO
import zio.ZLayer
import zio.stm.STM
import zio.stm.TMap
import zio.stream.Stream
import zio.stream.SubscriptionRef
import zio.stream.UStream
import zio.stream.ZStream

import java.{util => ju}
import scala.collection.mutable.Map as MutableMap
import scala.util.chaining._

/**
 * Represents the message management service, it takes care of publishing and
 * sharing of messages to all interested parties.
 */
trait Chat:

  /**
   * It allows a [[User]] to publish a [[Message]] within a specified [[Room]]
   * by [[RoomId]].
   *
   * @param roomId
   *   identifies the [[Room]] of interest.
   * @param owner
   *   the [[User]] who create the [[Message]].
   * @param message
   *   the [[Message]] to be published.
   * @return
   *   a [[Stream]] which certifies the correct publication with a [[Unit]]
   *   element.
   */
  def publishMessage(
    roomId: RoomId,
    owner: User,
    message: String
  ): Stream[NoSuchElementException, Unit]

  /**
   * It exposes a stream continuously fed with new [[Message]]s posted within a
   * certain [[Room]].
   * @param roomId
   *   identifies the [[Room]] of interest.
   * @return
   *   the [[ZStream]] that emits new messages.
   */
  def roomMessages(
    roomId: RoomId
  ): ZStream[Scope, NoSuchElementException, Message]

/**
 * A concrete implementation of the [[Chat]] service.
 *
 * @param roomRepository
 *   storage used to retrieve the [[Room]]s.
 * @param messageHubs
 *   structure which keeps in memory all the [[Message]]s published in
 *   the relative [[Room]]s.
 */
case class ChatLive(
  private val roomRepository: RoomRepository,
  private val messageHubs: Ref.Synchronized[Map[RoomId, Hub[Message]]]
) extends Chat:
  val defaultWindowSize = 256

  private def createHub(roomId: RoomId): UIO[Hub[Message]] =
    for
      hub <- Hub.sliding[Message](defaultWindowSize)
      _   <- ZIO.attempt { messageHubs.update(_.updated(roomId, hub)) }.orDie
    yield hub

  private def getOrCreateHub(
    roomId: RoomId
  ): IO[NoSuchElementException, Hub[Message]] =
    messageHubs.modifyZIO { s =>
      for
        hub <- ZIO
          .fromOption(s.get(roomId))
          .mapError(_ => new NoSuchElementException)
          .catchAll { _ =>
            roomRepository.get(roomId) *> createHub(roomId)
          }
        updated <- ZIO.succeed(s.updated(roomId, hub))
      yield (hub, updated)
    }

  override def publishMessage(
    roomId: RoomId,
    owner: User,
    message: String
  ): Stream[NoSuchElementException, Unit] =
    ZStream.fromZIO(
      for
        uuid <- Random.nextUUID.map(UUID.fromJavaUUID)
        msg  <- ZIO.succeed(Message.Text(uuid, owner, message))
        hub  <- getOrCreateHub(roomId)
        _    <- hub.offer(msg)
      yield ZIO.unit
    )

  import zio.durationInt

  override def roomMessages(
    roomId: RoomId
  ): ZStream[Scope, NoSuchElementException, Message] =
    for
      hub   <- ZStream.fromZIO(getOrCreateHub(roomId))
      queue <- ZStream.fromZIO(hub.subscribe)
      msg   <- ZStream.fromQueue(queue)
    yield msg

object Chat:

  /**
   * The production version of the [[Chat]] service.
   */
  val live: URLayer[RoomRepository, Chat] = ZLayer.scoped {
    for
      ref  <- Ref.Synchronized.make(Map.empty[RoomId, Hub[Message]])
      repo <- ZIO.service[RoomRepository]
    yield ChatLive(repo, ref)
  }

  def publishMessage(
    roomId: RoomId,
    owner: User,
    messageBody: String
  ): ZStream[Chat, NoSuchElementException, Unit] =
    ZStream.environmentWithStream[Chat](
      _.get.publishMessage(roomId, owner, messageBody)
    )

  def roomMessages(
    roomId: RoomId
  ): ZStream[Chat & Scope, NoSuchElementException, Message] =
    ZStream.environmentWithStream[Chat](_.get.roomMessages(roomId))
