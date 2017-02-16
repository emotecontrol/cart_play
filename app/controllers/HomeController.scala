package controllers

import java.net.{URLDecoder, URLEncoder}
import java.util.UUID
import javax.inject._

import controllers.Forms._
import persistence.{Cart, CartEntry, MyDatabase, Product}
import play.api._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(env: Environment, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Redirect(routes.HomeController.store)
  }

  def init = Action {
    import MyDatabase._
    Await.result(MyDatabase.autocreate.future(), 5.seconds)

    val initProducts = Seq(
      Product("Milk", "A tasty beverage", "milk.jpeg", 4.99),
      Product("Cheese", "Nice when shredded or melted", "cheese.jpeg", 3.49),
      Product("Bread", "A fluffy loaf of gluten", "bread.jpeg", 3.99)
    )

    initProducts.map(products.store)
    Ok("Initialized data")
  }

  def store = Action.async { request =>
    val user = request.cookies.get("cart")
    import MyDatabase._
    user match {
      case Some(cart) if Try(Json.parse(URLDecoder.decode(cart.value, "UTF-8"))).toOption.flatMap(_.asOpt[Cart]).isDefined =>
        val userCart = Json.parse(URLDecoder.decode(cart.value, "UTF-8")).asOpt[Cart].get
          products.getAllProducts().map { prodList =>
            Ok(views.html.store(addToCartForm)(prodList, userCart.cartItems.size)).withCookies(cart)
          }

      case None =>
        products.getAllProducts().map{ prodList =>
          val newUser = UUID.randomUUID()
          val newCart = URLEncoder.encode(Json.toJson(Cart(newUser, Nil)).toString, "UTF-8")
          Ok(views.html.store(addToCartForm)(prodList, 0)).withCookies(Cookie("cart", newCart, Some(86400)))
        }

    }
  }

  def cart = Action.async { request =>
    val user = request.cookies.get("cart")
    import MyDatabase._
    user match {
      case Some(cart) if Try(Json.parse(URLDecoder.decode(cart.value, "UTF-8"))).toOption.flatMap(_.asOpt[Cart]).isDefined =>
        val userCart = Json.parse(URLDecoder.decode(cart.value, "UTF-8")).asOpt[Cart].get

          products.getAllProducts().map { products =>
            Ok(views.html.cart(userCart.cartItems, products)).withCookies(cart)
          }

      case None =>
        val newUser = UUID.randomUUID()
        val newCart = URLEncoder.encode(Json.toJson(Cart(newUser, Nil)).toString, "UTF-8")
        Future.successful(Ok(views.html.cart(Nil, Nil)).withCookies(Cookie("cart", newCart, Some(86400))))
    }
  }

  def addToCart = Action.async { implicit request =>
    addToCartForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest("Invalid form data."))
      },
      userData => {
        def updateCart(cart: Cart, newEntry: CartEntry): Cart = {
          if (cart.cartItems.exists(entry => entry.productName == newEntry.productName)){
            val oldEntry = cart.cartItems.find(entry => entry.productName == newEntry.productName).get
            val updatedEntry = oldEntry.copy(count = newEntry.count + oldEntry.count)
            val updatedCartItems = cart.cartItems.filterNot(entry => oldEntry == entry) :+ updatedEntry
            cart.copy(cartItems = updatedCartItems)
          } else {
            val updatedCartItems = cart.cartItems :+ newEntry
            cart.copy(cartItems = updatedCartItems)
          }
        }

        val user = request.cookies.get("cart")
        import MyDatabase._
        user match {
          case Some(cart) if Try(Json.parse(URLDecoder.decode(cart.value, "UTF-8"))).toOption.flatMap(_.asOpt[Cart]).isDefined =>
            val userCart = Json.parse(URLDecoder.decode(cart.value, "UTF-8")).asOpt[Cart].get
            val updatedCart = updateCart(userCart, CartEntry(userData.product, userData.quantity))
            val newUser = URLEncoder.encode(Json.toJson(updatedCart).toString, "UTF-8")
              products.getAllProducts().map { products =>
                Ok(views.html.cart(updatedCart.cartItems, products)).withCookies(Cookie("cart", newUser, Some(86400)))
              }


          case None =>
            val newUser = UUID.randomUUID()
            val cart = Cart(newUser, Seq(CartEntry(userData.product, userData.quantity)))
            val cartJson = URLEncoder.encode(Json.toJson(cart).toString, "UTF-8")
                products.getAllProducts().map { products =>
                Ok(views.html.cart(cart.cartItems, products)).withCookies(Cookie("cart", cartJson, Some(86400)))
              }
        }
      }
    )
  }

  def update = Action.async { implicit request =>
    import MyDatabase._
    val form = request.body.asFormUrlEncoded
    val formRegex = "quantity-(\\w+)".r
    products.getAllProducts().map { products =>

      val filteredMap = form.getOrElse(Map.empty).filterKeys(key => key.matches(formRegex.regex))
        .map { case (key, values) => (formRegex.findAllIn(key).matchData.toList.headOption.map(m => m.group(1)).getOrElse(""), values.headOption.getOrElse("")) }.toList
      val user = request.cookies.get("cart")

      user match {
        case Some(cart) if Try(Json.parse(URLDecoder.decode(cart.value, "UTF-8"))).toOption.flatMap(_.asOpt[Cart]).isDefined =>
          val userCart = Json.parse(URLDecoder.decode(cart.value, "UTF-8")).asOpt[Cart].get
          val cartItems = userCart.cartItems
          val updatedCartItems = cartItems.map { item =>
            val matchingValue = filteredMap.find { case (key, _) => key == item.productName }
            if (matchingValue.isDefined) {
              val newCount = Try(matchingValue.get._2.toInt).getOrElse(0)
              CartEntry(item.productName, newCount)
            } else item
          }.filterNot(cartItem => cartItem.count < 1)
          val updatedCart = userCart.copy(cartItems = updatedCartItems)
          val cartJson = URLEncoder.encode(Json.toJson(updatedCart).toString, "UTF-8")
          Redirect(routes.HomeController.cart).withCookies(Cookie("cart", cartJson, Some(86400)))

        case None =>
          val newUser = UUID.randomUUID()
          val newCart = URLEncoder.encode(Json.toJson(Cart(newUser, Nil)).toString, "UTF-8")
          Redirect(routes.HomeController.store).withCookies(Cookie("cart", newCart, Some(86400)))
      }


    }
  }

  def checkout = Action { request =>
    val user = request.cookies.get("cart")
    user match {
      case Some(cart) =>
        Redirect(routes.HomeController.confirm).withCookies(cart, Cookie("checkout", "true", Some(86400)))

      case None =>
        val newUser = UUID.randomUUID()
        val newCart = URLEncoder.encode(Json.toJson(Cart(newUser, Nil)).toString, "UTF-8")
        Redirect(routes.HomeController.store).withCookies(Cookie("cart", newCart, Some(86400)))
    }
  }

  def confirm = Action.async { request =>
    import MyDatabase._

    products.getAllProducts().map { products =>
      val user = request.cookies.get("cart")
      val checkout = request.cookies.get("checkout").map(_.value).getOrElse("false")

      user match {
        case Some(user) if checkout == "true" =>
          val userCart = Try(Json.parse(URLDecoder.decode(user.value, "UTF-8"))).toOption.flatMap(_.asOpt[Cart])
          userCart match {
            case Some(cart) if cart.cartItems.nonEmpty =>
              Ok(views.html.confirm(cart.cartItems, products)).discardingCookies(DiscardingCookie("checkout"), DiscardingCookie("cart"))
            case _ => Redirect(routes.HomeController.store).withCookies(user).discardingCookies(DiscardingCookie("checkout"))
          }

        case Some(user) =>
          Redirect(routes.HomeController.store).withCookies(user)
        case None =>
          val newUser = UUID.randomUUID()
          val newCart = URLEncoder.encode(Json.toJson(Cart(newUser, Nil)).toString, "UTF-8")
          Redirect(routes.HomeController.store).withCookies(Cookie("cart", newCart, Some(86400)))
      }
    }
  }
}


