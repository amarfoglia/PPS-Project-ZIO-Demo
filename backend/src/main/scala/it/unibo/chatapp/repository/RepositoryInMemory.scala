/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp.repository

import it.unibo.chatapp.Entity
import it.unibo.chatapp.UUID
import zio.Console
import zio.IO
import zio.Random
import zio.Ref
import zio.UIO
import zio.ZIO

import java.io.IOException

class RepositoryInMemory[Id <: UUID, A <: Entity[Id]](ref: Ref[Map[Id, A]])
  extends Repository[Id, A]:

  override def create(a: A): UIO[A] =
    ref.update(_ + (a.id -> a)) *> ZIO.succeed(a)

  override def all: UIO[List[A]] = ref.get.map(_.values.toList)

  override def get(id: Id): IO[NoSuchElementException, A] =
    ref.get.map(_.get(id)).someOrFail(new NoSuchElementException)

object RepositoryInMemory:
  case class Config(path: String)
