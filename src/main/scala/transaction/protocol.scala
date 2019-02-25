package transaction

package object protocol {

  trait Command

  case class Read(tx: String, keys: Seq[String]) extends Command

}
