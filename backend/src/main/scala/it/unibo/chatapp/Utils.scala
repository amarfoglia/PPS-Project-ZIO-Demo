/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp

import zio.IO
import zio.Task
import zio.UIO
import zio.URIO
import zio.ZIO

import java.io.File
import java.io.IOException
import scala.io.BufferedSource

object Utils:

  def openFile(path: String): IO[IOException, BufferedSource] =
    ZIO.attempt(scala.io.Source.fromResource(path)).refineToOrDie[IOException]

  def closeFile(source: BufferedSource): UIO[Unit] = ZIO.succeed(source.close())

  /**
   * Resourceful method to access a local file.
   *
   * @param path
   *   to local file.
   * @param use
   *   represents the consumer of the loaded resource.
   * @return
   *   a [[ZIO]] witch includes the acquisition and release of the resource.
   */
  def withFile[A](path: String)(
    use: BufferedSource => UIO[A]
  ): IO[IOException, A] = ZIO.acquireReleaseWith(openFile(path))(closeFile)(use)
