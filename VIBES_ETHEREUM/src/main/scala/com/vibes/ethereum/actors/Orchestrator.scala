package com.vibes.ethereum.actors

import akka.event.Logging
import akka.actor.{Actor, ActorRef, Props}
import akka.stream.scaladsl.SourceQueueWithComplete
import com.vibes.ethereum.models.{Account, Client, Transaction}
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.{AccountingActor, EventJson}

import scala.collection.AbstractSeq
import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap


object Orchestrator {
  case class StartSimulation(settings: Setting.type )
}


// TODO : How to stop the orchestrator. When is the simulation complete

// TODO: HTTP API access for Orchestrator
class Orchestrator(evmQueue: SourceQueueWithComplete[EventJson], localStatsQueue: SourceQueueWithComplete[LocalStatsJson], globalEventsQueue: SourceQueueWithComplete[StatsJson]) extends Actor {
  val log = Logging(context.system, this)
  var internalState = Vector[String]()
  var nodeRef = new HashMap[String, ActorRef]
  var bootNodes = new ListBuffer[String]
  var genesisAccList = new ListBuffer[Account]

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

  def createGenesisAccounts(noOfAcc : Int): ListBuffer[Account] = {
    var accList: ListBuffer[Account] = new ListBuffer[Account]()
    var i = 0
    for(i <- 1 to noOfAcc) {
      var acc = new Account(_creatorId = "GENESIS_ACCOUNT")
      accList += acc
    }
    return accList
  }


  def initializeSimilator(settings: Setting.type ) = {
    val reducer = context.system.actorOf(Props(new Reducer(globalEventsQueue, localStatsQueue)), "reducer")
    var adjmat = generateNeighbours(settings.bootNodes, settings.minConn, settings.maxConn)
    bootNodes = createNodes(settings, adjmat, reducer, bootNodes, "BOOT_NODES")
    genesisAccList = createGenesisAccounts(settings.bootAccNum)

    val bootAccounts = createAccounts(settings.bootAccNum, bootNodes)


    // Generate Secondary nodes
    adjmat = generateNeighbours(settings.nodesNum, settings.minConn, settings.maxConn)
    val nodes =  createNodes(settings, adjmat, reducer, bootNodes, "SECONDARY_FULL_NODES")
    val accounts = createAccounts(settings.accountsNum, nodes)

    //TODO: Terminate once the expected number of transactions are generated
    context.system.scheduler.schedule(5 second, 10 second, new Runnable {
      override def run(): Unit = {
        println("################CYCLE START##############")
        val txList = createTransactions(settings.txBatch, accounts)
        val i =0
        for (tx <- txList) {
          val selActor = nodeRef(Random.shuffle(nodeRef.keySet).take(1).toList(0))
          selActor ! NewTx(tx)
        }
      }
    })
  }



  // Create Nodes
  /*
  * 1. Create primary nodes
  * 2. Add genesis block to all the primary nodes
  * 3. When a secondary node is created, it selects one primary node at random and copies all the blockchain from the primary node
  *
  * */
  def createNodes(setting: Setting.type, adjMatrix: Array[Array[Int]], reducer:ActorRef, bootNodes: ListBuffer[String], nodeType: String ) : ListBuffer[String] = {
    var clientList = new ListBuffer[String]()

    for(i <- 0 to setting.nodesNum-1) {
      val client = new Client(nodeType, _lat = "10E20W", _lon = "20N5S")
      val neighbourName = compileNeighbourList(i, adjMatrix(i))
      val accountingActor: ActorRef = context.actorOf(Props(new AccountingActor(client.id, evmQueue, reducer)))

      if (nodeType != "BOOT_NODES") {
        var bootNode = nodeRef.get(Random.shuffle(bootNodes).take(1)(0)).get
        var nodeActor = context.system.actorOf(Props(new Node(client, neighbourName, reducer, accountingActor, Option(bootNode), setting)), "node_" + i.toString)
        nodeRef.put(client.id, nodeActor)
        nodeActor ! InitiateBlockchainCopy
      }
      else {
        var nodeActor = context.system.actorOf(Props(new Node(client, neighbourName, reducer, accountingActor, None, setting)), "node_" + i.toString)
        nodeRef.put(client.id, nodeActor)
        nodeActor ! InitializeBootNode(genesisAccList)
      }
      clientList += client.id
    }
    return clientList
  }


  // Create Accounts
  def createAccounts(count: Int, clientList: ListBuffer[String]): ListBuffer[Account] = {
    var i =0
    var accList = new ListBuffer[Account]()
    for(i <- 0 to count) {
      val acc = new Account(_creatorId = Random.shuffle(clientList).take(1)(0))
      accList += acc
      log.debug("Account Created : " + acc.address)
      val node = nodeRef.get(acc.creatorId)
      node.get ! CreateAccount(acc)
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


  def compileNeighbourList(nodeIndex: Int, adjRow: Array[Int]): ListBuffer[ActorRef] = {
    var nodeList = new ListBuffer[ActorRef]
    for (i <- 0 to adjRow.length-1 if adjRow(i) == 1) {
      var node = nodeRef.get("node_" + i)
      node match{
        case Some(node) => {nodeList += node}
      }
    }
    nodeList
  }
}
