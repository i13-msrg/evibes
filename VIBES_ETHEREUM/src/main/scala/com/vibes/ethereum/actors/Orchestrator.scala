package com.vibes.ethereum.actors

import akka.event.Logging
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.stream.scaladsl.SourceQueueWithComplete
import com.vibes.ethereum.models.{Account, Client, Transaction}
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.{AccountingActor, EventJson}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Orchestrator {
  case class StartSimulation(settings: Setting.type )
}

// TODO : How to stop the orchestrator. When is the simulation complete

// TODO: HTTP API access for Orchestrator
class Orchestrator(eventQueue: SourceQueueWithComplete[EventJson], localStatsQueue: SourceQueueWithComplete[LocalStatsJson], globalStatsQueue: SourceQueueWithComplete[StatsJson]) extends Actor {
  val log = Logging(context.system, this)

  import Orchestrator._

  override def preStart(): Unit = {
    log.debug("Starting Orchestrator")
    println("Starting Orchestrator")
  }

  import Node._

  override def receive: Receive = {
    case StartSimulation(settings) => startSimulation(settings)
  }


  def startSimulation(settings: Setting.type): Cancellable = {
    val reducer = context.system.actorOf(Props(new Reducer(globalStatsQueue, localStatsQueue)), "reducer")
    val bootNodes = initializeSimilator(settings, reducer)
    val fullNodes = startNodes(settings.nodesNum, new mutable.HashMap[String, ActorRef], reducer, eventQueue, settings, bootNodes)

    // Create initial number of accounts
    val accounts = createAccountsInNode(settings.accountsNum, fullNodes)
    scheduleTxCreation(settings, accounts, fullNodes)
  }


  def initializeSimilator(settings: Setting.type, reducer: ActorRef): mutable.HashMap[String, ActorRef] = {
    println("Boot node initialization started")
    val bootNodes = createBootNodes(settings, reducer, eventQueue)
    Thread.sleep(10000)
    addBootNodeNeighbours(bootNodes, settings.minConn, settings.maxConn)
    bootNodes
  }


  def startNodes(noOfNodes: Int, nodesMap: mutable.HashMap[String, ActorRef], reducer: ActorRef,
                 eventQueue: SourceQueueWithComplete[EventJson], setting: Setting.type,
                 bootNodeMap: mutable.HashMap[String, ActorRef]): mutable.HashMap[String, ActorRef] = {

    for (i <- 0 until noOfNodes) {
      val nodetp = createNode("FULLNODE", reducer, eventQueue, setting)
      nodesMap.put(nodetp._1, nodetp._2)
      val bootNodeKeys = bootNodeMap.keys.toList
      val indx = Random.nextInt(bootNodeKeys.length)
      nodetp._2 ! InitializeFullNode(bootNodeMap.get(bootNodeKeys(indx)).get)
    }
    nodesMap
  }


  def scheduleTxCreation(settings: Setting.type, accounts: ListBuffer[Account], nodeMap: mutable.HashMap[String, ActorRef]): Cancellable = {
    var nodeKeys = nodeMap.keys.toList
    context.system.scheduler.schedule(30 second, 10 second, new Runnable {
      override def run(): Unit = {
        println("################CYCLE START##############")
        val txList = createTransactions(settings.txBatch, accounts)
        for (tx <- txList) {
          val node = nodeMap.get(nodeKeys(Random.nextInt(nodeKeys.length))).get
          node ! NewTx(tx)
        }
      }
    })
  }


  def createNode(nodeType: String, reducer: ActorRef, eventQueue: SourceQueueWithComplete[EventJson], setting: Setting.type): Tuple2[String, ActorRef] = {
    val client = new Client(nodeType, _lat = "10E20W", _lon = "20N5S")
    val accountingActor: ActorRef = context.actorOf(Props(new AccountingActor(client.id, eventQueue, reducer)))
    Thread.sleep(5000)
    var nodeActor = context.system.actorOf(Props(new Node(client, reducer, accountingActor, None, setting)), nodeType + "_" + client.id)
    new Tuple2[String, ActorRef](client.id, nodeActor)
  }


  def createBootNodes(setting: Setting.type, reducer: ActorRef, eventQueue: SourceQueueWithComplete[EventJson]): mutable.HashMap[String, ActorRef] = {
    var nodeMap = new mutable.HashMap[String, ActorRef]
    var genesisAccList = getGenesisAccountsList(setting.bootAccNum)
    for (i <- 1 to setting.bootNodes) {
      val nodetp = createNode("BOOTNODE", reducer, eventQueue, setting)
      nodeMap.put(nodetp._1, nodetp._2)
      nodetp._2 ! InitializeBootNode(genesisAccList)
    }
    nodeMap
  }


  def addBootNodeNeighbours(nodeMap: mutable.HashMap[String, ActorRef], minNeighbours: Int, maxNeighbours: Int) = {
    val nodeList: List[String] = nodeMap.keys.toList
    val neighbours = new mutable.HashMap[String, ActorRef]
    var i: Int = 0
    for (node <- 0 until nodeList.length) {
      val nbrCount = Random.nextInt(maxNeighbours - minNeighbours) + minNeighbours
      while (i < nbrCount) {
        val nbr = Random.nextInt(nodeList.length)
        if (nbr != node) {
          neighbours.put(nodeList(node), nodeMap.get(nodeList(node)).get)
          i += 1
        }
      }
      nodeMap.get(nodeList(node)).get ! AddNeighbours(neighbours)
      i = 0
    }
  }


  // Create Accounts
  def createAccountsInNode(count: Int, nodeMap: mutable.HashMap[String, ActorRef]): ListBuffer[Account] = {
    var accounts = new ListBuffer[Account]()
    var nodeKeys = nodeMap.keys.toList
    for (i <- 0 to count) {
      var key = nodeKeys(Random.nextInt(nodeKeys.length))
      val acc = new Account(_creatorId = key)
      accounts += acc
      log.debug("Account Created : " + acc.address)
      val node = nodeMap.get(key).get
      node ! CreateAccount(acc)
    }
    accounts
  }


  def getGenesisAccountsList(noOfAcc: Int): ListBuffer[Account] = {
    var accList: ListBuffer[Account] = new ListBuffer[Account]()
    var i = 0
    for (i <- 1 to noOfAcc) {
      var acc = new Account(_creatorId = "GENESIS_ACCOUNT")
      accList += acc
    }
    return accList
  }


  //Creates a list of transactions. Can be done periodically
  def createTransactions(count: Int, accList: ListBuffer[Account]): ListBuffer[Transaction] = {
    var txList = new ListBuffer[Transaction]()
    var i = 0
    for (i <- 1 to count) {
      val accounts = Random.shuffle(accList).take(2)
      txList += new Transaction(_sender = accounts(0).address, _receiver = accounts(1).address)
    }
    return txList
  }
}