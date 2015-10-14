package com.programmaticallyspeaking.nashornmonads.nashorn

import java.io._
import java.nio.file.{FileSystems, StandardCopyOption, Files, Path}
import java.util.{UUID, Scanner}
import javax.script.{ScriptContext, ScriptEngine, Bindings}

import com.factor10.programmaticallyspeaking.nashornmonads.util.{IO, Using}
import jdk.nashorn.api.scripting.NashornScriptEngineFactory

object Bridge {
  def apply(): Bridge = apply(classOf[Bridge])

  def apply(resourceReferenceClass: Class[_]): Bridge = {
    val engine = new NashornScriptEngineFactory().getScriptEngine
    new Bridge(engine, engine.getBindings(ScriptContext.ENGINE_SCOPE), resourceReferenceClass)
  }
}

class Bridge(engine: ScriptEngine, bindings: Bindings, resourceReferenceClass: Class[_]) {
  // Patch the global load function so that it supports resource loading
  patchLoad()

  // Install Q for promise support
  installPromiseSupport()

  private def getResourceAsStream(resourcePath: String): Option[InputStream] =
    Option(resourceReferenceClass.getResourceAsStream(resourcePath))

  private def writeResourceToTempFile(is: InputStream, resourcePath: String): File = {
    val tempFile = File.createTempFile("bridge", resourcePath.replace("/", "_"))
    Files.copy(is, tempFile.toPath, StandardCopyOption.REPLACE_EXISTING)
    tempFile.deleteOnExit() // the file gets deleted when the JVM exits
    tempFile
  }

  private def getResourceAsTempFile(resourcePath: String): Option[File] =
    getResourceAsStream(resourcePath).map(stream => writeResourceToTempFile(stream, resourcePath))

  private def patchLoad(): Unit = {
    val loader = new Loader
    bindings.put("__loader", loader)
    eval(
      """
        |var noResourceFoundEx = Java.type('com.programmaticallyspeaking.nashornmonads.nashorn.NoResourceFoundException');
        |var global = this;
        |var originalLoad = global.load;
        |this.load = function () {
        |  var args = Array.prototype.slice.call(arguments);
        |  try {
        |    originalLoad.apply(this, args);
        |  } catch (e) {
        |    try {
        |      global.__loader.load(e, args[0]);
        |    } catch (fromLoader) {
        |      if (fromLoader instanceof noResourceFoundEx)
        |        throw e; // the loader couldn't load the resource, re-throw the original error
        |      throw fromLoader;
        |    }
        |  }
        |};
      """.stripMargin)
  }

  private def installPromiseSupport(): Unit = {
    eval(
      """
        |load('/lib/generated/jvm-npm.js');
        |this.Q = require('Q');
      """.stripMargin)
  }

  def eval(script: String): AnyRef = engine.eval(script, bindings)

  class Loader {
    def load(originalException: AnyRef, src: AnyRef): AnyRef = {
      // Maybe the source refers to a resource. Try to obtain the resource as a temp file.
      getResourceAsTempFile(src.toString) match {
        case Some(tempFile) =>
          val filePath = tempFile.getAbsolutePath.replace("\\", "\\\\")
          eval(s"""load('$filePath');""")
        case None =>
          // No resource found, throw the original exception so that the ultimate caller of `load` gets a proper
          // JavaScript exception.
          throw new NoResourceFoundException() // caught by the load wrapper above
      }
    }
  }
}


/**
 * A marker exception thrown by the resource loader inside [[Bridge]], so that the load wrapper can determine why
 * resource loading failed. This is a top-level class so that it has an "easy" class name.
 */
class NoResourceFoundException extends RuntimeException