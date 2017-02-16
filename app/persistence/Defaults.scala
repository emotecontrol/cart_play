package persistence

import com.websudos.phantom.connectors.ContactPoints
import com.websudos.phantom.dsl._

import scala.concurrent.Await
//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Defaults {

  val hosts = Seq("127.0.0.1")

  val connector = ContactPoints(hosts).keySpace("challenge")
}


class MyDatabase(val keyspace: KeySpaceDef) extends Database(keyspace) {
  object products extends ConcreteProducts with keyspace.Connector

}

object MyDatabase extends MyDatabase(Defaults.connector)

object DBInit {
  implicit val session = MyDatabase.session
  implicit val space = MyDatabase.space
  Await.result(MyDatabase.autocreate.future(), 5.seconds)
}