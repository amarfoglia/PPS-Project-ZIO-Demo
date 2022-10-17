/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp.http

import it.unibo.chatapp.ClientActionCommand
import it.unibo.chatapp.ClientCommand
import it.unibo.chatapp.Subscribe
import it.unibo.chatapp.SubscribeRoom
import it.unibo.chatapp.service.Auth
import it.unibo.chatapp.service.Chat
import it.unibo.chatapp.service.Lobby
import zhttp.http.!!
import zhttp.http./
import zhttp.http.Http
import zhttp.http.Method
import zhttp.http.Middleware
import zhttp.http.Request
import zhttp.http.Response
import zhttp.http._
import zhttp.service.Channel
import zhttp.service.ChannelEvent
import zhttp.service.ChannelEvent.ChannelRead
import zhttp.service.ChannelEvent.ChannelUnregistered
import zhttp.service.Server
import zhttp.socket.SocketApp
import zhttp.socket.SocketApp.apply
import zhttp.socket.WebSocketChannelEvent
import zhttp.socket.WebSocketFrame
import zio.Scope
import zio.ZIO
import zio.json.DecoderOps
import zio.json.EncoderOps
import zio.json.JsonDecoder
import zio.stream.ZStream

import java.io.IOException
import java.nio.channels.ClosedChannelException

object JsonDecode:
  def unapply[A: JsonDecoder](obj: String): Option[A] = obj.fromJson[A].toOption

val commandFilter: Http[
  Any,
  Nothing,
  WebSocketChannelEvent,
  (Channel[WebSocketFrame], ClientCommand)
] =
  Http.collect[WebSocketChannelEvent] {
    case ChannelEvent(
          channel,
          ChannelRead(WebSocketFrame.Text(JsonDecode[ClientCommand](command)))
        ) =>
      (channel, command)
  }

extension [R, E, A, B](http: Http[R, E, A, B])

  def debug(fb: (B) => ZIO[Any, Nothing, Any]): Http[R, E, A, B] =
    http >>> Http.collectZIO(ZIO.succeed(_).tap(fb))

def userCommandSocket =
  commandFilter >>>
    Http.collectZIO[(Channel[WebSocketFrame], ClientCommand)] {
      case (ch, Subscribe) =>
        Controller.subscribe
          .map(_.toJson)
          .map(WebSocketFrame.Text(_))
          .runForeach(ch.writeAndFlush(_))
      case (ch, command: ClientActionCommand) =>
        Controller.handleClientCommand(command).runDrain
    }

def dataStreamSocket =
  commandFilter >>>
    Http.collectZIO[(Channel[WebSocketFrame], ClientCommand)] {
      case (ch, SubscribeRoom(roomId)) =>
        Controller
          .subscribeRoom(roomId)
          .map(_.toJson)
          .map(WebSocketFrame.Text(_))
          .runForeach(ch.writeAndFlush(_))
    }

def commandSocket: SocketApp[Controller] = userCommandSocket.toSocketApp

def dataSocket: SocketApp[Controller & Scope] = dataStreamSocket.toSocketApp

import zhttp.http.middleware.Cors.{CorsConfig}

val config: CorsConfig = CorsConfig(
  allowedOrigins = _ == "localhost",
  allowedMethods = Some(Set(Method.POST))
)

val app: Http[Controller, Throwable, Request, Response] =
  Http.collectZIO[Request] {
    case req @ (Method.POST -> !! / "signup") =>
      for
        body <- req.body.asString
        user <- Controller.signup(body)
      yield Response.json(user.toJson)

    case Method.GET -> !! / "subscribe" =>
      Response.fromSocketApp(commandSocket)

    case Method.GET -> !! / "subscribeRoom" =>
      Response.fromSocketApp(dataSocket).provideSome[Controller](Scope.default)
  } @@ Middleware.cors(config)
