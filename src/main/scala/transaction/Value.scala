package transaction

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class Value[T](var value: T) {

  val id = UUID.randomUUID.toString
  var lastTx: Option[String] = None
  var tx: Option[String] = None
  var tmp = 0L
  var tmpValue = value

  def repair()(implicit ec: ExecutionContext): Future[Boolean] = this.synchronized {
    val t = Transactions.data(tx.get)

    t.getStatus().map {
      _ match {
        case Status.PENDING => false
        case Status.COMMITTED =>

          lastTx = tx
          tx = None
          tmp = 0L
          value = tmpValue

          true

        case Status.ABORTED =>

          tx = None
          tmp = 0L

          true
      }
    }
  }

  def read(t: String)(implicit ec: ExecutionContext): Future[Option[T]] = this.synchronized {

    if(tx.isEmpty){
      tx = Some(t)
      tmp = System.currentTimeMillis()
      return Future.successful(Some(value))
    }

    if(tx.get.equals(t)) return Future.successful(Some(value))

    /*val now = System.currentTimeMillis()
    val elapsed = now - tmp

    if(elapsed < TIMEOUT) return Future.successful(None)*/

    /*repair().map {
      _ match {
        case true =>

          tx = Some(t)
          tmp = System.currentTimeMillis()

          Some(value)

        case false => None
      }
    }*/

    Future.successful(None)
  }

  def write(t: String, v: T)(implicit ec: ExecutionContext): Future[Boolean] = this.synchronized {
    if(tx.isEmpty || !tx.get.equals(t)) return Future.successful(false)

    /*val now = System.currentTimeMillis()
    val elapsed = now - tmp

    if(elapsed < TIMEOUT) {
      tmpValue = v
      return Future.successful(true)
    }

    repair().map {
      _ match {
        case true => false
        case false =>
          tmpValue = v
          true
      }
    }*/

    tmpValue = v

    return Future.successful(true)
  }

  def commit(t: String)(implicit ec: ExecutionContext): Future[Boolean] = this.synchronized {
    if(tx.isEmpty || !tx.get.equals(t)) return Future.successful(false)

    lastTx = tx
    value = tmpValue
    tx = None
    tmp = 0L

    Future.successful(true)
  }

  def abort(t: String)(implicit ec: ExecutionContext): Future[Boolean] = this.synchronized {
    if(tx.isEmpty || !tx.get.equals(t)) return Future.successful(false)

    tx = None
    tmp = 0L

    Future.successful(true)
  }
}
