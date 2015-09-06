package com.github.vonnagy.service.container.http

import java.util.UUID

import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{Formats, CustomSerializer}
import org.json4s.ext.JodaTimeSerializers
import spray.http._
import spray.httpx.Json4sJacksonSupport
import spray.httpx.marshalling.{BasicMarshallers, Marshaller, MetaMarshallers}

trait DefaultMarshallers extends MetaMarshallers with BasicMarshallers {

  def json4sSupport(implicit fmt: Formats): Json4sJacksonSupport = new Json4sJacksonSupport {
    implicit def json4sJacksonFormats = fmt
  }

  // The implicit formats used for serialization. This can be overridden
  implicit def jsonFormats = org.json4s.DefaultFormats ++ JodaTimeSerializers.all ++ List(UUIDSerializer)

  def jsonUnmarshaller[T: Manifest] = json4sSupport.json4sUnmarshaller[T]

  def jsonMarshaller[T <: AnyRef] = json4sSupport.json4sMarshaller

  def plainMarshaller[T <: Any] =
    Marshaller.delegate[T, String](ContentTypes.`text/plain`)(_.toString)

  case object UUIDSerializer extends CustomSerializer[UUID](format => ( {
    case JString(u) => UUID.fromString(u)
    case JNull => null
    case u => UUID.fromString(u.toString)
  }, {
    case u: UUID => JString(u.toString)
  }
      ))
}
