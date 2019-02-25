package transaction

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.Actor

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import protocol._
import akka.pattern._

import scala.collection.concurrent.TrieMap

class Transactor(val id: String) extends Actor {

  implicit val ec = context.system.dispatcher

  var executing = new AtomicBoolean()
  var transactions = Seq[(String, (Long, Seq[String], Promise[Option[Map[String, Int]]]))]()

  override def preStart(): Unit = {
    context.system.scheduler.schedule(0 seconds, 10 milliseconds) {
      executing.set(true)

      val txs = transactions.sortBy(_._1)

      executing.set(false)
    }
  }

  def read(cmd: Read): Future[Option[Map[String, Int]]] = {
    val p = Promise[Option[Map[String, Int]]]
    transactions = transactions :+ cmd.tx -> (System.currentTimeMillis(), cmd.keys, p)
    p.future
  }

  override def receive: Receive = {
    case cmd: Read => read(cmd).pipeTo(sender)
    case _ =>
  }
}
