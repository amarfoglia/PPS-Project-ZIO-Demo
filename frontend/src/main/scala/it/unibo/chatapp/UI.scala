/*
 * Copyright (c) 2022 amarfoglia.
 *
 * This file is part of PPS-Project-ZIO-Demo, and is distributed under the terms
 * of the MIT License as described in the file LICENSE.
 */

package it.unibo.chatapp

import scalatags.JsDom.TypedTag
import org.scalajs.dom.Element
import scalatags.JsDom.all._

/**
 * Represents a DOM element created programmatically.
 */
trait Component:
  def tag: TypedTag[Element]

case class RoomComponent(
  room: Room,
  onSelectedRoom: Room => Unit
) extends Component:

  override val tag: TypedTag[Element] =
    val roomTag = room.name.split("\\s+").reduce(_.head + "" + _.head)
    div(
      `class` := "row sideBar-body",
      onclick := { () => onSelectedRoom(room) }
    )(
      div(`class` := "col-sm-3 col-xs-3 sideBar-room")(
        div(`class` := "room-icon")(
          p(roomTag)
        )
      ),
      div(`class` := "col-sm-9 col-xs-9 sideBar-main")(
        div(`class` := "row")(
          div(`class` := "col-sm-8 col-xs-8 sideBar-name")(
            span(`class` := "name-meta")(room.name)
          )
        )
      )
    )

enum OwnerType(val name: String):
  case Sender   extends OwnerType("sender")
  case Receiver extends OwnerType("receiver")

case class TextMessageComponent(message: Message.Text, ownerType: OwnerType)
  extends Component:

  override val tag: TypedTag[Element] =
    div(`class` := "row message-body")(
      div(`class` := s"col-sm-12 message-main-${ownerType.name}")(
        div(`class` := ownerType.name)(
          div(`class` := "message-owner pull-right")(message.owner.name),
          div(`class` := "message-text")(message.body)
        )
      )
    )

case class ButtonComponent(text: String, onClick: () => Unit) extends Component:

  override def tag: TypedTag[Element] =
    button(
      `class` := "btn btn-default",
      onclick := { onClick },
      `type`  := "button"
    )(text)
