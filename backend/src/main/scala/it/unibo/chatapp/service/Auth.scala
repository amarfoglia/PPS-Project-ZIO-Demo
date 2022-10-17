/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp.service

import it.unibo.chatapp.UUID
import it.unibo.chatapp.User
import it.unibo.chatapp.repository.UserRepository
import zio.Random
import zio.Task
import zio.UIO
import zio.URIO
import zio.URLayer
import zio.ZIO
import zio.ZLayer

/**
 * Represents a naive authentication service used to register the [[User]].
 */
trait Auth:
  /**
   * Creates a [[User]] given a username.
   * @param username
   *   name that identifies the [[User]] within the service.
   * @return
   *   a description of the created [[User]]
   */
  def register(username: String): UIO[User]

/**
 * A concrete implementation of the [[Auth]] service.
 *
 * @param repository
 *   the storage used to save the created [[User]].
 */
case class AuthLive(repository: UserRepository) extends Auth:

  override def register(username: String): UIO[User] =
    for
      uuid <- Random.nextUUID.map(UUID.fromJavaUUID)
      user = User(uuid, username)
      _ <- repository.create(user)
    yield user

object Auth:

  /**
   * The production version of the [[Auth]] service.
   */
  val live: URLayer[UserRepository, Auth] =
    ZLayer.fromFunction(AuthLive(_))

  def register(username: String): URIO[Auth, User] =
    ZIO.environmentWithZIO(_.get.register(username))
