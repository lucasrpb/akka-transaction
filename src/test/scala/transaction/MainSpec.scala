package transaction

import java.util.UUID
import java.util.concurrent.{Executors, ThreadLocalRandom}
import java.util.concurrent.atomic.AtomicReference

import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class MainSpec extends FlatSpec {

  val MAX_VALUE = 100
  val rand = ThreadLocalRandom.current()

  def test(): Unit = {

    val n = 10

    var accounts = Seq.empty[Value[Int]]
    var before = Seq.empty[(String, Int)]

    for(i<-0 until n){
      val balance = rand.nextInt(0, MAX_VALUE)
      val account = new Value[Int](balance)

      accounts = accounts :+ account
      before = before :+ (account.id -> balance)

      Storage.data += account.id -> account
    }

    val size = accounts.length

    def transfer(): Future[Boolean] = {

      val p0 = rand.nextInt(0, size)
      val p1 = rand.nextInt(0, size)

      if(p0 == p1) return Future.successful(true)

      val from = accounts(p0)
      val to = accounts(p1)

      val values = Seq(from, to)

      val t = new Transaction[Int]()

      Transactions.data += t.id -> t

      def abort(): Future[Boolean] = {
        t.abort().flatMap { _ =>
          Future.sequence(Seq(from.abort(t.id), to.abort(t.id))).map(_ => false)
        }
      }

      def commit(): Future[Boolean] = {
        t.commit().flatMap { ok =>
          if(ok){
            Future.sequence(Seq(from.commit(t.id), to.commit(t.id))).map(_ => true)
          } else {

            println(s":(")

            abort()
          }
        }
      }

      def read(reads: Seq[Int]): Future[Boolean] = {
        val b1 = reads(0)
        val b2 = reads(1)

        val amount = if(b1 == 0) 0 else rand.nextInt(0, b1)

        println(s"transfering ${amount} from ${from.id} with $b1 to ${to.id} with ${b2}")

        Future.sequence(Seq(from.write(t.id, b1 - amount), to.write(t.id, b2 + amount))).flatMap { writes =>
          if(writes.exists(_ == false)){

            //println(s"write failure ${writes}\n")

            abort()
          } else {
            commit()
          }
        }
      }

      Future.sequence(Seq(from.read(t.id), to.read(t.id))).flatMap { reads =>
        if(reads.exists(_.isEmpty)){
          //println(s"lock failed ${reads}")
          abort()
        } else {
          read(reads.map(_.get))
        }
      }
    }

    val m = 100
    var tasks = Seq.empty[Future[Boolean]]

    for(i<-0 until m){
      tasks = tasks :+ transfer()
    }

    val results = Await.result(Future.sequence(tasks), 10 seconds)
    var moneyBefore = 0
    var moneyAfter = 0

    println()

    before.foreach { case (id, bb) =>

      val a = Storage.data(id)
      val ba = a.value

      moneyBefore += bb
      moneyAfter += ba

      println(s"${id} => before: $bb after: $ba\n")
    }

    val successes = results.count(_== true)
    val failures = results.length - successes

    println(s"results: ${successes} failure: ${failures}\n")
    println(s"before: ${moneyBefore} after: ${moneyAfter}\n")

    assert(moneyBefore == moneyAfter)
  }

  "index data " should "be equal to test data" in {

    val n = 1000

    for(i<-0 until n){
      test()
    }

  }

}
