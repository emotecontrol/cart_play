package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

case class AddToCart(product: String, quantity: Int)

object Forms {
  val addToCartForm = Form(
    mapping(
      "product" -> nonEmptyText,
      "quantity" -> number
    )(AddToCart.apply)(AddToCart.unapply)
  )
}
