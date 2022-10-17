/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp

import org.scalajs.dom

object Util:

  def prompt(message: String, default: String): String =
    val res = dom.window.prompt(message, default)
    if res == null then default else res

extension [L <: Throwable, R](either: Either[L, R])
  /* unsafe, throws L */
  def getOrThrow(): R = either.fold(throw _, identity)
