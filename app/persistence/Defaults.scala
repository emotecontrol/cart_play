package persistence

import com.websudos.phantom.connectors.ContactPoints
import com.websudos.phantom.dsl.{Database, KeySpaceDef}

object Defaults {

  val hosts = Seq("127.0.0.1")

  val Connector = ContactPoints(hosts).keySpace("challenge")
}


class MyDatabase(val keyspace: KeySpaceDef) extends Database(keyspace) {
  object products extends ConcreteProducts with keyspace.Connector
  object cartEntries extends ConcreteCartEntries with keyspace.Connector
}

object MyDatabase extends MyDatabase(Defaults.connector)