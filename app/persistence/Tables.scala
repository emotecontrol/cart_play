package persistence

import com.websudos.phantom.dsl._

import scala.concurrent.Future

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
      .value(_.imageResource, product.imageResource)
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

