package persistence

import java.util.UUID

import com.websudos.phantom.dsl._

import scala.concurrent.Future

case class Product(name: String, description: String, imageResource: String, price: Double)

case class CartEntry(user: UUID, productName: String, count: Int)

class Products extends CassandraTable[ConcreteProducts, Product] {
  object name extends StringColumn(this) with PartitionKey[String]
  object description extends StringColumn(this)
  object imageResource extends StringColumn(this)
  object price extends DoubleColumn(this)

  def fromRow(row: Row): Product = {
    Product(
      name(row),
      description(row),
      imageResource(row),
      price(row)
    )
  }
}

abstract class ConcreteProducts extends Products with RootConnector {
  def store(product: Product): Future[ResultSet] = {
    insert
      .value(_.name, product.name)
      .value(_.description, product.description)
      .value(_.price, product.price)
      .future()
  }

  def getByName(name: String): Future[Option[Product]] = {
    select
      .where(_.name eqs name)
      .one()
  }

  def getAllProducts(): Future[Seq[Product]] = {
    select.fetch()
  }
}

class CartEntries extends CassandraTable[ConcreteCartEntries, CartEntry] {
  object user extends UUIDColumn(this) with PartitionKey[UUID]
  object productName extends StringColumn(this) with PrimaryKey[String]
  object count extends IntColumn(this)

  def fromRow(row: Row): CartEntry = {
    CartEntry(
      user(row),
      productName(row),
      count(row)
    )
  }
}

abstract class ConcreteCartEntries extends CartEntries with RootConnector {
  def store(cartEntry: CartEntry): Future[ResultSet] = {
    insert
      .value(_.user, cartEntry.user)
      .value(_.productName, cartEntry.productName)
      .value(_.count, cartEntry.count)
      .future()
  }

  def getCart(uuid: UUID): Future[List[CartEntry]] = {
    select
      .where(_.user eqs uuid)
      .fetch()
  }

  def removeCartEntry(uuid: UUID, productName: String): Future[ResultSet] = {
    delete()
      .where(_.user eqs uuid)
      .and(_.productName eqs productName)
      .future()
  }

  def updateCartEntry(uuid: UUID, productName: String, count: Int): Future[ResultSet] = {
    update()
      .where(_.user eqs uuid)
      .and(_.productName eqs productName)
      .modify(_.count setTo count)
      .future()
  }
}