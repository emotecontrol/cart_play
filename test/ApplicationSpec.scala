import java.net.URLEncoder
import java.util.UUID

import org.scalatestplus.play._
import persistence.{Cart, CartEntry}
import play.api.libs.json.Json
import play.api.mvc.Cookie
import play.api.test._
import play.api.test.Helpers._


class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }
  }

  "HomeController" should {

    "render the store page" in {
      val home = route(app, FakeRequest(GET, "/store")).get
      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
    }

  }

    "create a cookie for a new user" in {
      val home = route(app, FakeRequest(GET, "/store")).get
      cookies(home).get("cart") mustBe defined
    }

    "redirect to confirm when confirmation request has a cart cookie" in {
      val dummyCart = Cart(UUID.randomUUID(), Seq(CartEntry("Bread",1)))
      val newCart = URLEncoder.encode(Json.toJson(dummyCart).toString, "UTF-8")

      val home = route(app, FakeRequest(GET, "/checkout").withCookies(Cookie("cart", newCart))).get
      redirectLocation(home) mustBe Some("/confirm")
    }

    "redirect to store when confirmation request has no cart cookie" in {
      val home = route(app, FakeRequest(GET, "/checkout")).get
      redirectLocation(home) mustBe Some("/store")
    }

    "add a checkout cookie when confirmation request is passed to confirm" in {
      val dummyCart = Cart(UUID.randomUUID(), Seq(CartEntry("Bread",1)))
      val newCart = URLEncoder.encode(Json.toJson(dummyCart).toString, "UTF-8")

      val home = route(app, FakeRequest(GET, "/checkout").withCookies(Cookie("cart", newCart))).get

      cookies(home).get("checkout").get.value mustBe "true"
    }

    "remove cookies when confirmation request is handled by confirm" in {
      val dummyCart = Cart(UUID.randomUUID(), Seq(CartEntry("Bread",1)))
      val newCart = URLEncoder.encode(Json.toJson(dummyCart).toString, "UTF-8")

      val home = route(app, FakeRequest(GET, "/confirm").withCookies(Cookie("cart", newCart), Cookie("checkout", "true"))).get

      cookies(home).get("checkout").get.value mustBe ""
      cookies(home).get("cart").get.value mustBe ""
    }


}
