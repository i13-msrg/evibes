package com.vibes.ethereum.actors

import akka.event.Logging
import akka.actor.{Actor, Props}
import com.vibes.ethereum.models.{Account, Client, Transaction}
import com.vibes.ethereum.Setting
object Orchestrator {
  case class StartSimulation(settings: Setting.type )
}


// TODO : How to stop the orchestrator. When is the simulation complete

// TODO: HTTP API access for Orchestrator
class Orchestrator extends Actor {
  val log = Logging(context.system, this)
  var internalState = Vector[String]()
  val accountsList = List[Account]()


  import Orchestrator._
  override def preStart(): Unit = {
    log.debug("Starting Orchestrator")
    println("Starting Orchestrator")
  }
  import Node._
  override def receive: Receive = {
    case StartSimulation(settings) => initializeSimilator(settings)
  }


  def state = internalState
/*
  def createAccounts(accNum: Int) : List[Account] ={
    var i = 0
    for (i <- 0 to accNum) {
      new Account(_creatorId = "ThiSDUMMP")
    }
  }
*/

  def initializeSimilator(settings: Setting.type ) = {
    var i =0
    for(i <- 1 to 10) {
      var client = new Client("FULL_NODE", _lat = "10E20W", _lon = "20N5S")
      val nodeActor = context.system.actorOf(Props[Node], "node_" + i.toString)
      nodeActor ! StartNode(client)
    }
    //context.system.actorSelection("/user/*") ! StartNode(client)
  }

  // Create Accounts
  //Set a schedule to generate Transactions for sending to Nodes
  //def tx = new Transaction(_nonce= , _gasPrice=, _receiver=, _value=, _sender=)
}
