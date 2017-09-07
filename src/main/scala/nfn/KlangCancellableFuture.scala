// Source: https://gist.github.com/Tolsi/69b01acbe7cd79fc93cf

package nfn

import java.util.concurrent.{CompletableFuture, Executors}
import java.util.function.Supplier

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

trait CancellableFuture[T] extends Future[T] {
  def future(): Future[T]

  def cancel(): Unit

  def isCancelled: Boolean
}

// original from https://gist.github.com/viktorklang/5409467
object KlangCancellableFuture {
  def apply[T](work: => T)(implicit executor: ExecutionContext): KlangCancellableFuture[T] = {
    new KlangCancellableFuture(work)
  }
}

class KlangCancellableFuture[T](work: => T)(implicit executor: ExecutionContext) extends CancellableFuture[T] {
  private val p = Promise[T]()
  private val lock = new Object
  private var currentThread: Thread = null
  @volatile
  private var cancelled: Boolean = false

  override val future = p.future

  run()

  private def run(): Unit = {
    p tryCompleteWith Future {
      throwCancellationExceptionIfCancelled {
        val thread = Thread.currentThread
        lock.synchronized {
          updateCurrentThread(thread)
        }
        try {
          throwCancellationExceptionIfCancelled(work)
        } finally {
          lock.synchronized {
            updateCurrentThread(null)
          } ne thread
          //Deal with interrupted flag of this thread in desired
        }
      }
    }
  }

  private def throwCancellationExceptionIfCancelled(body: => T) = {
    if (cancelled) throw new CancellationException
    body
  }

  private def updateCurrentThread(newThread: Thread): Thread = {
    val old = currentThread
    currentThread = newThread
    old
  }

  override def cancel(): Unit = {
    lock.synchronized {
      Option(updateCurrentThread(null)).foreach(_.interrupt())
      cancelled |= p.tryFailure(new CancellationException)
    }
  }

  override def isCancelled: Boolean = future.value match {
    case _@Some(Failure(t:CancellationException)) => true
    case _ => false
  }

  override def onComplete[U](f: (Try[T]) => U)(implicit executor: ExecutionContext): Unit = future.onComplete(f)

  override def isCompleted: Boolean = future.isCompleted

  override def value: Option[Try[T]] = future.value

  @throws[Exception](classOf[Exception])
  override def result(atMost: Duration)(implicit permit: CanAwait): T = future.result(atMost)

  @throws[InterruptedException](classOf[InterruptedException])
  @throws[TimeoutException](classOf[TimeoutException])
  override def ready(atMost: Duration)(implicit permit: CanAwait): KlangCancellableFuture.this.type = {
    this
//    KlangCancellableFuture[T](Await.result(future, atMost))
  }
}

object KlangCancellableFutureTestApp extends App {
  def blockCall(name: String, sec: Int) = {
    println(s"$name: start")
    try {
      for {i <- 1 to sec} {
        Thread.sleep(1000)
        println(s"$name: i'm alive $i")
      }
      println(s"$name finish")
    } catch {
      case _: InterruptedException =>
        println(s"$name stop")
    }
  }

  val pool = Executors.newFixedThreadPool(1)
  implicit val ctx = ExecutionContext.fromExecutor(pool)

  val cancellableFutures = for {i <- 1 to 200} yield {
    KlangCancellableFuture(blockCall(s"task-$i", 5))
  }

  Thread.sleep(3000)

  for {
    (f, i) <- cancellableFutures.zipWithIndex
    if i < 100
  } f.cancel()

  Thread.sleep(6000)

  cancellableFutures.foreach(_.cancel())

  println(s"${cancellableFutures.count(_.isCancelled)} tasks was cancelled")

  pool.shutdown()
}

object KlangCancellableFutureTestApp2 extends App {
  val pool = Executors.newFixedThreadPool(1)
  implicit val ctx = ExecutionContext.fromExecutor(pool)

  private val cancellable = KlangCancellableFuture {
    try {
      for (i <- 1 to 100) {
        for (a <- 1 to 100000) {
          println(s"i: $i, a: $a")
        }
        Thread.sleep(1)
      }
    } catch {
      case e: Exception => println(s"Catch exception $e")
    }
  }

  cancellable.cancel()
  pool.shutdown()
}

object JavaCompletableFutureTestApp extends App {
  private val cf = CompletableFuture.supplyAsync(new Supplier[String]() {
    override def get() = {
      try {
        for (i <- 1 to 30) {
          for (a <- 1 to 100000) {
            println(s"i: $i, a: $a")
          }
          //        Thread.sleep(1)
        }
      } catch {
        case e: Exception => println(s"Catch exception $e")
      }
      "test"
    }
  })
  println("START")
  Thread.sleep(1000)
  try {
    cf.cancel(false)
  } catch {
    case e: CancellationException => println(s"Catch ex: $e")
  }
  cf.join()
}


object JavaFutureTestApp extends App {
  private val executor = Executors.newFixedThreadPool(1)
  private val future = executor.submit(new Runnable() {
    override def run(): Unit = {
      for (i <- 1 to 20) {
        if (!Thread.currentThread().isInterrupted()) {
          for (a <- 1 to 100000) {
            println(s"x i: $i, a: $a")
          }
        }
      }
    }
  })
  println("START")
  Thread.sleep(1000)
  future.cancel(true)
  println("DONE")
  executor.shutdown()
}