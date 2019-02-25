package transaction

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class Transaction[T](val id: String = UUID.randomUUID.toString) {

  val tmp = System.currentTimeMillis()
  var status = Status.PENDING

  def getStatus()(implicit ec: ExecutionContext): Future[Int] = this.synchronized {
    Future.successful(status)
  }

  def commit()(implicit ec: ExecutionContext): Future[Boolean] = this.synchronized {

    if(status != Status.PENDING) return Future.successful(false)

    status = Status.COMMITTED

    Future.successful(true)
  }

  def abort()(implicit ec: ExecutionContext): Future[Boolean] = this.synchronized {
    if(status != Status.PENDING) return Future.successful(false)

    status = Status.ABORTED

    Future.successful(true)
  }

}
