package test.com.programmaticallyspeaking.nashornmonads.nashorn

import com.programmaticallyspeaking.nashornmonads.nashorn.Bridge
import org.scalatest.{FlatSpec, Matchers, FunSpec}

class BridgeTest extends FlatSpec with Matchers {

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

  "The global" should "expose Q" in {
    val bridge = Bridge()
    val q = bridge.eval("this.Q;")
    q shouldNot be (null)
  }
//
//  "The jvm-npm module" should "add require support" in {
//    val bridge = Bridge()
//
//    bridge.eval("load('/lib/generated/jvm-npm.js');")
//    val q = bridge.eval("require('Q');")
//    q shouldNot be (null)
//  }
}