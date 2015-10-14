package com.factor10.programmaticallyspeaking.nashornmonads.util

object IO {
  def doIgnoringExceptions(block: => Unit): Unit = {
    try {
      block
    } catch {
      case ex: Exception =>
      // ignore
    }
  }
}