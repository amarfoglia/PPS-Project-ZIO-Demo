/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp

import org.scalajs.dom.WebSocket
import zio.json.JsonEncoder
import zio.json.EncoderOps
import zio.json.DecoderOps
import zio.json.JsonDecoder

extension (ws: WebSocket)
  def sendJson[A: JsonEncoder](a: A): Unit = ws.send(a.toJson)

  def on[A: JsonDecoder](pf: PartialFunction[A, Unit]): Unit =
    def handleParseError(message: String) = println(message)

    ws.onmessage = _.data.toString
      .fromJson[A]
      .fold(
        handleParseError,
        { x =>
          if pf isDefinedAt x then pf(x) else ()
        }
      )
