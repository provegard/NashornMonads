package com.programmaticallyspeaking.nashornmonads.util

/**
 * Try-with-resources in Scala. From http://illegalexception.schlichtherle.de/2012/07/19/try-with-resources-for-scala/
 * but renamed so that its usage is `using (...) in (...)`.
 * @param resource the resource to use
 * @tparam A the type of the resource
 */
class Using[A <: AutoCloseable](resource: A) {
  def in[B](block: A => B) = {
    var t: Throwable = null
    try {
      block(resource)
    } catch {
      case x: Throwable =>
        t = x
        throw x
    } finally {
      if (resource != null) {
        if (t != null) {
          try {
            resource.close()
          } catch {
            case y: Throwable =>
              t.addSuppressed(y)
          }
        } else {
          resource.close()
        }
      }
    }
  }
}

object Using {
  def using[A <: AutoCloseable](resource: A) = new Using(resource)
}