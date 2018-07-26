package com.vibes.ethereum.helpers

import java.util.UUID
trait GuidExtension {
  /**
    * Creating a random UUID (Universally unique identifier).
    */
  def generateGUID(): String = {
    UUID.randomUUID().toString.toUpperCase
  }
}
