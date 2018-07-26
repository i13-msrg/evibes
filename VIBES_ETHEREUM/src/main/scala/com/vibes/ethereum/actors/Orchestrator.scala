package com.vibes.ethereum.actors

import akka.event.Logging
import akka.actor.{Actor}

object Orchestrator {
  case class StartSimulation(data:String)
}


// TODO : How to stop the orchestrator. When is the simulation complete

// TODO: HTTP API access for Orchestrator
class Orchestrator extends Actor {
  val log = Logging(context.system, this)
  var internalState = Vector[String]()

  import Orchestrator._
  override def preStart(): Unit = {
    log.debug("Starting Orchestrator")
    // initializeSimulation()
    // Schedule createTransactions()
  }

  override def receive: Receive = {
    case StartSimulation(data) => { internalState = internalState:+ data }
  }


  def state = internalState


  def initializeSimulation() = {
    // Start Network Manager
    // Start Storage Manager
    // Start Nodes based on the Input. Create Lat and Lon for all the clients (should be on land)

    // Create Accounts
  }

  def createTransactions() ={
    // Create transactions... in a regular time interval. Schedule this
  }
}
