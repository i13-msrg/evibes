package com.vibes.ethereum

import akka.actor.{ActorSystem, Props}
import com.vibes.ethereum.actors.Orchestrator


object VIBES_ETHEREUM {
  def main(args: Array[String]) {
    println("Welcome to Ethereum simulation")
    val system = ActorSystem("Ethereum")
    val orchestrator = system.actorOf(Props[Orchestrator], "orchestrator")
    //orchestrator ! StartSimulation
  }
}
