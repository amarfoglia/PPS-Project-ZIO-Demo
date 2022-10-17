/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp.repository

import it.unibo.chatapp.Entity
import it.unibo.chatapp.Room
import it.unibo.chatapp.RoomId
import it.unibo.chatapp.UUID
import it.unibo.chatapp.User
import it.unibo.chatapp.UserId
import it.unibo.chatapp.repository.RepositoryInMemory.Config
import zio.IO
import zio.Ref
import zio.Tag
import zio.UIO
import zio.ZIO
import zio.ZLayer
import zio.json.JsonDecoder
import zio.json.JsonEncoder

import java.io.IOException

/**
 * Represents an object which participates in the domain but really abstracts
 * away storage and infrastructure details.
 */
trait Repository[Id <: UUID, A <: Entity[Id]]:
  /**
   * Stores an [[Entity]].
   *
   * @param a
   *   the [[Entity]] to be stored.
   * @return
   *   a description of the created [[Entity]].
   */
  def create(a: A): UIO[A]

  /**
   * Retrieves all the stored [[Entity]].
   *
   * @return
   *   a description of the [[Entity]] list.
   */
  def all: UIO[List[A]]

  /**
   * Retrieves an [[Entity]] by id.
   *
   * @param id
   *   identifies the [[Entity]] of interest.
   * @return
   *   a [[ZIO]] containing the selected [[Entity]] in case of success, a
   *   [[NoSuchElementException]] otherwise.
   */
  def get(id: Id): IO[NoSuchElementException, A]

type UserRepository = Repository[UserId, User]
type RoomRepository = Repository[RoomId, Room]

object Repository:

  import it.unibo.chatapp.Utils.withFile
  import zio.json.DecoderOps
  import util.chaining.scalaUtilChainingOps

  def inMemory[Id <: UUID, A <: Entity[Id]](config: Config)(using
    Tag[Id],
    Tag[A],
    JsonEncoder[A],
    JsonDecoder[A]
  ): ZLayer[Any, IOException, Repository[Id, A]] =
    ZLayer.scoped {
      for
        content <- withFile(config.path)(_.getLines.mkString.pipe(ZIO.succeed))
        rooms <- ZIO
          .from(content.fromJson[List[A]])
          .orElseFail(new IOException("Could not parse rooms"))
        init <- ZIO.succeed(rooms.zipWithIndex.map { case (r, _) =>
          (r.id, r)
        }.toMap)
        ref <- Ref.make(init)
      yield RepositoryInMemory(ref)
    }

  def test[Id <: UUID, A <: Entity[Id]](using
    Tag[Id],
    Tag[A]
  ): ZLayer[Any, IOException, Repository[Id, A]] =
    ZLayer.scoped {
      for ref <- Ref.make(Map.empty)
      yield RepositoryInMemory[Id, A](ref)
    }
