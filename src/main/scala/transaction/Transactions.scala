package transaction

import scala.collection.concurrent.TrieMap

object Transactions {

  val data = TrieMap[String, Transaction[Int]]()

}
