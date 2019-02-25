package object transaction {

  val TIMEOUT = 30L

  object Status {
    val ABORTED = 0
    val COMMITTED = 1
    val PENDING = 2
  }

}
