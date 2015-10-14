package test.com.programmaticallyspeaking.nashornmonads.nashorn

import java.util.concurrent.{TimeoutException, TimeUnit, Executors}

import com.programmaticallyspeaking.nashornmonads.nashorn.WindowTimers
import jdk.nashorn.api.scripting.{AbstractJSObject, JSObject}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._

class WindowTimersTest extends FunSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  def functionObject(f: (Seq[AnyRef]) => Unit): JSObject = new AbstractJSObject {
    override def isFunction: Boolean = true

    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
      f(args.toSeq)
      null
    }
  }

  val service = Executors.newSingleThreadScheduledExecutor()
  val underTest = new WindowTimers(service)

  def collectingFunction(promise: Promise[String]): (Seq[AnyRef]) => Unit = args =>
    promise.success("called with args: " + args.mkString(", "))

  describe("setTimeout") {
    it("should schedule execution of a function") {
      val promise = Promise[String]()
      val fun = functionObject(collectingFunction(promise))
      underTest.setTimeout(fun, 10)

      whenReady(promise.future) { result =>
        result should be ("called with args: ")
      }
    }

    it("should return a numerical ID") {
      // Spec: https://html.spec.whatwg.org/multipage/webappapis.html#timer-initialisation-steps - step 2
      val promise = Promise[String]()
      val fun = functionObject(collectingFunction(promise))
      val retVal = underTest.setTimeout(fun, 10)

      retVal shouldBe a[java.lang.Integer]
    }

    it("should NOT return zero as the first numerical ID") {
      val promise = Promise[String]()
      val fun = functionObject(collectingFunction(promise))
      val timers = new WindowTimers(service) // cannot reuse underTest, since it may have created a timer already
      val retVal = timers.setTimeout(fun, 10)

      retVal shouldNot be (0)
    }

    it("should support arguments") {
      val promise = Promise[String]()
      val fun = functionObject(collectingFunction(promise))
      underTest.setTimeout(fun, 10, "a", "b")

      whenReady(promise.future) { result =>
        result should be ("called with args: a, b")
      }
    }
  }

  describe("clearTimeout") {
    it("should cancel scheduled execution of a function") {
      val promise = Promise[String]()
      val fun = functionObject(collectingFunction(promise))
      val token = underTest.setTimeout(fun, 100)
      underTest.clearTimeout(token)

      intercept[TimeoutException] {
        Await.result(promise.future, 200.milliseconds)
      }
    }

    it("should ignore undefined/null argument") {
      underTest.clearTimeout(null)
      1 should be (1) // dummy assert
    }
  }

  describe("setInterval") {
    it("should schedule recurring execution of a function") {
      val promise = Promise[String]()
      var invocation = 0
      val fun = functionObject(_ => {
        invocation += 1
        if (invocation > 1) promise.success("test")
      })
      underTest.setInterval(fun, 10, 50)

      whenReady(promise.future) { result =>
        result should be ("test")
      }
    }

    it("should support arguments") {
      val promise = Promise[String]()
      var invocation = 0
      val fun = functionObject(args => {
        invocation += 1
        if (invocation > 1) promise.success("test " + args.mkString(", "))
      })
      underTest.setInterval(fun, 10, 50, "a", "b")

      whenReady(promise.future) { result =>
        result should be ("test a, b")
      }
    }

    it("should return a numerical ID") {
      // Spec: https://html.spec.whatwg.org/multipage/webappapis.html#timer-initialisation-steps - step 2
      val promise = Promise[String]()
      val fun = functionObject(collectingFunction(promise))
      val retVal = underTest.setInterval(fun, 10, 10)

      retVal shouldBe a[java.lang.Integer]
    }

    it("should NOT return zero as the first numerical ID") {
      val promise = Promise[String]()
      val fun = functionObject(collectingFunction(promise))
      val timers = new WindowTimers(service) // cannot reuse underTest, since it may have created a timer already
      val retVal = underTest.setInterval(fun, 10, 10)

      retVal shouldNot be (0)
    }
  }

  describe("clearInterval") {
    it("should cancel scheduled execution of a function") {
      val promise = Promise[String]()
      val fun = functionObject(collectingFunction(promise))
      val token = underTest.setInterval(fun, 100, 50)
      underTest.clearInterval(token)

      intercept[TimeoutException] {
        Await.result(promise.future, 200.milliseconds)
      }
    }

    it("should ignore undefined/null argument") {
      underTest.clearInterval(null)
      1 should be (1) // dummy assert
    }
  }


  override protected def afterAll(): Unit = {
    service.shutdownNow()
    service.awaitTermination(1, TimeUnit.SECONDS)
  }
}