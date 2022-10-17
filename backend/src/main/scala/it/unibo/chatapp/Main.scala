/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp

import it.unibo.chatapp.http.Controller
import it.unibo.chatapp.http.app
import it.unibo.chatapp.repository.Repository
import it.unibo.chatapp.repository.RepositoryInMemory
import it.unibo.chatapp.repository.RepositoryInMemory.Config
import it.unibo.chatapp.service.Auth
import it.unibo.chatapp.service.Chat
import it.unibo.chatapp.service.Lobby
import zhttp.service.Server
import zio.Console.printLine
import zio.ExitCode
import zio.ZIO
import zio.ZIOAppDefault
import zio.ZLayer

object Main extends ZIOAppDefault:

  val program = for
    _ <- printLine("Starting server")
    _ <- Server.start(8091, app)
  yield ExitCode.success

  val run = program
    .provide(
      Controller.live,
      Auth.live,
      Lobby.live,
      Chat.live,
      Repository.inMemory[RoomId, Room](Config(path = "rooms.json")),
      Repository.inMemory[UserId, User](Config(path = "users.json"))
      // ZLayer.Debug.tree
    )
    .catchAll(printLine(_))
    .fork *> ZIO.never
