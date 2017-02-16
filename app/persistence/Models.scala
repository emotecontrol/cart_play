package persistence

import java.util.UUID

import play.api.data.validation.ValidationError
import play.api.libs.json._

import scala.util.Try

case class CartEntry(productName: String, count: Int)
case class Cart(user: UUID, cartItems: Seq[CartEntry])
case class Product(name: String, description: String, imageResource: String, price: Double)

object CartEntry {
  implicit val format = Json.format[CartEntry]
}

object Cart {
  def parseUUID(s: String): Option[java.util.UUID] = Try(java.util.UUID.fromString(s)).toOption

  implicit val uuidFormat = new Format[UUID] {
    def reads(json: JsValue): JsResult[UUID] = json match {
      case JsString(s) =>
        parseUUID(s).map(JsSuccess(_)).getOrElse(JsError(Seq(JsPath() -> Seq(ValidationError("Expected UUID string")))))
      case _ =>
        JsError(Seq(JsPath() -> Seq(ValidationError("Expected UUID string"))))
    }
    def writes(uuid: java.util.UUID): JsValue = JsString(uuid.toString)
  }

  implicit val format = Json.format[Cart]
}



