package test.com.programmaticallyspeaking.nashornmonads.nashorn

import com.programmaticallyspeaking.nashornmonads.nashorn.Bridge
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers, FunSpec}

class BridgeTest extends FlatSpec with Matchers with TableDrivenPropertyChecks  {

  "The bridge" should "patch the global load function so that it can load resources" in {
    val bridge = Bridge(classOf[BridgeTest])

    bridge.eval("load('/patch_load/some.js');")
    val result = bridge.eval("this.evidence();")

    result.toString should be ("I'm alive")
  }

  "The patched load function used by the bridge" should "deal with a missing resource" in {
    val bridge = Bridge(classOf[BridgeTest])

    val ex = intercept[Exception] {
      bridge.eval("load('/patch_load/notFound.js');")
    }

    ex.getMessage should include ("Cannot load script")
  }

  val globalExamples =
    Table(
      "global",
      "Q",
      "setTimeout",
      "setInterval",
      "clearTimeout",
      "clearInterval"
    )

  "The global" should "expose the appropriate objects/functions" in {
    forAll(globalExamples) { name =>
      val bridge = Bridge()
      val ref = bridge.eval(s"this['$name'];")

      assert(ref ne null, s"-- could not find global function/object $name")
    }
  }
}