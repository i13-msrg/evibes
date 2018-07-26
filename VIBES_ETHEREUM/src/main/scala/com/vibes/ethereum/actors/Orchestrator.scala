package com.vibes.ethereum.actors

import akka.event.Logging
import akka.actor.{Actor, ActorRef, Props}
import com.vibes.ethereum.models.{Account, Client, Transaction}
import com.vibes.ethereum.Setting

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Orchestrator {
  case class StartSimulation(settings: Setting.type )
}


// TODO : How to stop the orchestrator. When is the simulation complete

// TODO: HTTP API access for Orchestrator
class Orchestrator extends Actor {
  val log = Logging(context.system, this)
  var internalState = Vector[String]()
  var nodeRef = new ListBuffer[ActorRef]()


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

  def initializeSimilator(settings: Setting.type ) = {
    val nodes =  createNodes(settings.nodesNum)
    val accounts = createAccounts(settings.accountsNum, nodes)

    //TODO: Terminate once the expected number of transactions are generated
    context.system.scheduler.schedule(5 second, 10 second, new Runnable {
      override def run(): Unit = {
        println("################CYCLE START##############")
        val txList = createTransactions(settings.txBatch, accounts)
        val i =0
        for (i<- 0 to txList.length-1) {
          val selActor = Random.shuffle(nodeRef).take(1)(0)
          selActor ! NewTx(txList(i))
        }}
      })
  }

  // Create Nodes
  def createNodes(count: Int) : ListBuffer[String] = {
    var i =0
    var clientList = new ListBuffer[String]()
    for(i <- 1 to count) {
      val client = new Client("FULL_NODE", _lat = "10E20W", _lon = "20N5S")
      val nodeActor = context.system.actorOf(Props(new Node(client)), "node_" + i.toString)
      clientList += client.id
      nodeRef += nodeActor
      nodeActor ! StartNode
    }
    return clientList
  }

  // Create Accounts
  def createAccounts(count: Int, clientList: ListBuffer[String]): ListBuffer[Account] ={
    var i =0
    var accList = new ListBuffer[Account]()
    for(i <- 0 to count) {
      val acc = new Account(_creatorId = Random.shuffle(clientList).take(1)(0))
      accList += acc
      println("Account Created : " + acc.address)
    }
    return accList
  }

  //Creates a list of transactions. Can be done periodically
  def createTransactions(count: Int, accList: ListBuffer[Account]): ListBuffer[Transaction] = {
    var txList = new ListBuffer[Transaction]()
    var i =0
    for (i<- 1 to count) {
      val accounts = Random.shuffle(accList).take(2)
      txList += new Transaction(_sender=accounts(0).address, _receiver=accounts(1).address)
    }
    return txList
  }
  //Set a schedule to generate Transactions for sending to Nodes
  //def tx = new Transaction(_nonce= , _gasPrice=, _receiver=, _value=, _sender=)
}
