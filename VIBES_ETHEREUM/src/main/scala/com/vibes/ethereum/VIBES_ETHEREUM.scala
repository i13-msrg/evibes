package com.vibes.ethereum

import akka.actor.{ActorSystem, Props}
import com.vibes.ethereum.actors.Orchestrator
import com.vibes.ethereum.actors.Orchestrator._

object Setting {
  val nodesNum: Int = 100
  val txNum: Int = 100
  val accountsNum: Int = 10
  val txBatch: Int = 10
}

object VIBES_ETHEREUM {
  def main(args: Array[String]) {
    println("Welcome to Ethereum simulation")
    val system = ActorSystem("Ethereum")

    val orchestrator = system.actorOf(Props[Orchestrator], "orchestrator")
    orchestrator ! StartSimulation(Setting)
  }
}
