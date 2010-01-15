import java.util.concurrent.{TimeUnit, CountDownLatch}
import jetlang.example.{PooledFiber, ThreadedFiber, JetlangActor}
import org.scalatest.FunSuite

case class A(t: String)
case class B(t: String)
case class C(t: String)

class NestedActor(count: Int) extends JetlangActor with ThreadedFiber {
  val latch = new CountDownLatch(count)

  def act() = {
    case A(n) => {
      receive {
        case B(n) => {
          receive {
            case C(n) => {
              latch.countDown
            }
          }
        }
      }
    }
  }
}

class Reply extends JetlangActor with ThreadedFiber {
  def act() = {
    case B(n) => sender ! C("ReplyTo")
  }
}

class Starter(replyActor: Reply) extends JetlangActor with ThreadedFiber {
  val latch = new CountDownLatch(1)

  def act() = {
    case A(n) => replyActor ! B("Reply")
    case C(n) => latch.countDown
  }
}

class NestedReactTest extends FunSuite {
  test("should handleNestedReacts") {
    val actor = new NestedActor(1)
    actor.start
    actor ! A("A1")
    actor ! B("B1")
    actor ! C("C1")

    assert(actor.latch.await(10, TimeUnit.SECONDS))
    actor.exit
  }

  test("should handleNestedReactsWithOutOfOrderMessages") {
    val actor = new NestedActor(2)
    actor.start
    actor ! A("A1")
    actor ! A("A2")
    actor ! B("B1")
    actor ! B("B2")
    actor ! C("C1")
    actor ! C("C2")

    assert(actor.latch.await(10, TimeUnit.SECONDS))
    actor.exit
  }

  test("should reply to sender") {
    val reply = new Reply()
    val starter = new Starter(reply)
    reply.start
    starter.start
    starter ! A("A")
    assert(starter.latch.await(10, TimeUnit.SECONDS))
    starter.exit
    reply.exit
  }
}