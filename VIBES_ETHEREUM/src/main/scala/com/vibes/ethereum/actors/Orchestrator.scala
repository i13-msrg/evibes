package com.vibes.ethereum.actors

import akka.event.Logging
import akka.actor.{Actor, ActorRef, Props}
import com.vibes.ethereum.models.{Account, Client, Transaction}
import com.vibes.ethereum.Setting

import scala.collection.AbstractSeq
import scala.collection.mutable.ListBuffer
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
    var adjmat = generateNeighbours(settings.nodesNum, settings.minConn, settings.maxConn)
    val nodes =  createNodes(settings, adjmat)
    val accounts = createAccounts(settings.accountsNum, nodes)

    //TODO: Terminate once the expected number of transactions are generated
    context.system.scheduler.schedule(5 second, 10 second, new Runnable {
      override def run(): Unit = {
        println("################CYCLE START##############")
        val txList = createTransactions(settings.txBatch, accounts)
        val i =0
        for (tx <- txList) {
          val selActor = Random.shuffle(nodeRef).take(1)(0)
          selActor ! NewTx(tx)
        }
      }
    })
  }

  // Create Nodes
  def createNodes(setting: Setting.type, adjMatrix: Array[Array[Int]] ) : ListBuffer[String] = {
    var clientList = new ListBuffer[String]()
    for(i <- 0 to setting.nodesNum-1) {
      val client = new Client("FULL_NODE", _lat = "10E20W", _lon = "20N5S")
      val neighbourName = compileNeighbourList(i, adjMatrix(i))
      val nodeActor = context.system.actorOf(Props(new Node(client, neighbourName, setting)), "node_" + i.toString)
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

  //Generate Neighbours for nodes.
  //TODO: Replace this method by a decentralized method similar to Node Discovery Protocol taking the network behaviour into account

  def generateNeighbours(numNodes: Int, minConn: Int, maxConn: Int) = {
    val adjMatrix = Array.ofDim[Int](numNodes, numNodes)
    var nodeCount: Int = 0
    var nodeList = (0 to numNodes-1).toList
    for (i <- 0 to numNodes-1) {
      var rangeList = (minConn to maxConn).toList
      nodeCount = Random.shuffle(rangeList).take(1)(0)
      for (n <- Random.shuffle(nodeList).take(nodeCount)) {
        adjMatrix(i)(n) = 1
        adjMatrix(n)(i) = 1
      }
    }
    adjMatrix
  }

  def compileNeighbourList(nodeIndex: Int, adjRow: Array[Int]) = {
    var nodeList = new ListBuffer[String]
    for (i <- 0 to adjRow.length-1 if adjRow(i) == 1) {
      nodeList += "node_" + i
    }
    nodeList
  }
}
