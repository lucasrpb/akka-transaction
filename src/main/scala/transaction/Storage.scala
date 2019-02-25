package transaction

import scala.collection.concurrent.TrieMap

object Storage {

  val data = TrieMap[String, Value[Int]]()

}
