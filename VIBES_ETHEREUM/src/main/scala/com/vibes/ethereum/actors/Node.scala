package com.vibes.ethereum.actors

import akka.actor.{Actor, ActorPath, ActorRef, Props}
import akka.event.Logging
import com.vibes.ethereum.actors.ethnode.{EvmPrimary, TxPoolerActor}
import com.vibes.ethereum.models._
import com.vibes.ethereum.service.RedisManager
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.AccountingActor._

import scala.collection.mutable
import scala.collection.mutable.{HashMap, ListBuffer}
import scala.concurrent.Await
import scala.util.Random

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._


object Node {
  case class NewTx(tx: Transaction)
  case class NewBlock(block: Block)

  case class InitializeFullNode(bootNode: ActorRef)
  case class InitializeBootNode(accList: ListBuffer[Account])
  case class BlockchainCopyRequest()
  case class CreateAccount(account: Account)
  case class ReturnGhostDepth(depth: GHOST_DepthSet)
  case class PropogateToNeighbours(block: Block)

  case class AddNeighbours(neighbours:HashMap[String, ActorRef])
  case class NeighbourCopyRequest(clientId: String)
  case class NeighbourCopyResponse(neighbourMap: mutable.HashMap[String, ActorRef])
  case class NodeDiscoveryPing(clientId: String)
  case class NodeDiscoveryPong(clientId: String)
}

class Node(client: Client, reducer: ActorRef, accountingActor: ActorRef, bootNode: Option[ActorRef], setting: Setting.type ) extends Actor with akka.actor.ActorLogging  {
  accountingActor ! NodeStart
  log.info("Starting Node")
  //log.info("Starting Node : " + client.id)
  accountingActor ! NodeStarted
  var btNode = bootNode
  val redis: RedisManager = new RedisManager(client.id)
  val evmPrimaryActor: ActorRef = context.actorOf(Props(new EvmPrimary(client, redis, accountingActor, context.self)))
  val txPoolerActor: ActorRef = context.actorOf(Props(new TxPoolerActor(evmPrimaryActor, accountingActor, setting, client.id)))
  var accountList = new ListBuffer[String]
  var neighbourMap = new HashMap[String, ActorRef]
  var neighbourStatus = new HashMap[String, Boolean]

  accountingActor ! NodeInitialized
  import Node._


  override def postStop(): Unit = {
    accountingActor ! NodeStop
    super.postStop()
  }

  def addNeighbours(neighbours: HashMap[String, ActorRef]): Unit = {
    val nodeList = neighbours.keys.toList
    for(key <- nodeList) {
      if(neighbourMap.contains(key) == false) {
        neighbourMap.put(key, neighbours.get(key).get)
        neighbourStatus.put(key, true)
        accountingActor ! NeighbourUpdate(nodeList.length)
        accountingActor ! NodeType
      }
    }
  }

  def scheduleNodeDiscovery() = {
    context.system.scheduler.schedule(60 second, 120 second, runnable = new Runnable {
      override def run(): Unit = {
        accountingActor ! Discover
        val statusKeys = neighbourStatus.keys.toList
        for (key <- statusKeys) {
          if (neighbourStatus.get(key).get == false) {
            neighbourMap.remove(key)
            neighbourStatus.remove(key)
          }
        }
        accountingActor ! NeighbourUpdate(neighbourMap.keys.toList.length)
        for (key <- neighbourMap.keysIterator) {
          neighbourMap.get(key).get ! NodeDiscoveryPing(client.id)
          neighbourStatus.put(key, false)
        }
        if (client.clientType == "FULLNODE") {
          btNode.get ! NeighbourCopyRequest(client.id)
        }
        accountingActor ! NodeInitialized

      }
    })
  }


  import ethnode.EvmPrimary._
  import ethnode.TxPoolerActor._
  import ethnode.AccountingActor._

  override def receive: Receive = {
    case NewTx(tx) => {txPoolerActor ! AddTxToPool(tx); propogateToNeighboursTx(tx)}
    case NewBlock(block: Block) => {
      //compute proptime
      var startTime = System.currentTimeMillis() / 1000
      var propTime = (startTime - block.timestamp)
      evmPrimaryActor ! NewExtBlock(block)
      accountingActor ! BlockReceived(propTime)
    }
    case CreateAccount(account: Account) => {evmPrimaryActor ! CreateAccountEVM(account)}
    case PropogateToNeighbours(block: Block) => propogateToNeighbours(block)

    case NeighbourCopyRequest(clientId: String) => {
                                                      neighbourMap.put(clientId, sender)
                                                      neighbourStatus.put(clientId, true)
                                                      sender ! NeighbourCopyResponse(neighbourMap)
                                                    }
    case NeighbourCopyResponse(neighbourMap: mutable.HashMap[String, ActorRef]) => addNeighbours(neighbourMap)
    case AddNeighbours(neighbours: HashMap[String, ActorRef]) => {addNeighbours(neighbours)}
    case NodeDiscoveryPing(clientId: String) => handlePing(clientId)
    case NodeDiscoveryPong(clientId: String) => handlePong(clientId)

    case InitializeBootNode(accList: ListBuffer[Account]) => initializeBootNode(accList)
    case InitializeFullNode(bootNode: ActorRef) => initializeFullNode(bootNode)
    case BlockchainCopyRequest() => {copyRequest()}
    case ReturnGhostDepth(depth: GHOST_DepthSet) => {evmPrimaryActor ! UpdateGhostDepth(depth)}
    case _ => unhandled(message = AnyRef)
  }


  def handlePing(senderId : String) = {
    //Random probability
    var seed = Random.nextInt(10)
    if(seed%2 == 0) {
      sender ! NodeDiscoveryPong(client.id)
      neighbourMap.put(senderId, sender)
      neighbourStatus.put(senderId, true)
    }
  }

  def handlePong(senderId : String) = {
    neighbourStatus.put(senderId, true)
    neighbourMap.put(senderId, sender)
    accountingActor ! NeighbourUpdate(neighbourMap.keys.toList.length)
  }

  def initializeFullNode(bNode: ActorRef) = {
    log.info("#######################################INITIALIZE FULL NODE MSG RECEIVED")
    implicit val timeout = Timeout(50 seconds)
    val future = bNode ? BlockchainCopyRequest()
    val tuple = Await.result(future, timeout.duration).asInstanceOf[Tuple3[String, ListBuffer[Block],GHOST_DepthSet]]
    blockchainResponse(tuple._1,tuple._2,tuple._3)
    bNode ! NeighbourCopyRequest(client.id)
    btNode = Option(bNode)
  }


  def propogateToNeighbours(block: Block) = {
    /*
    1. Find the neighbour actorref
    2. Send the msg
    * */
    for (neighbour <- neighbourMap.valuesIterator) {
      neighbour ! NewBlock(block)
    }
  }


  def propogateToNeighboursTx(tx: Transaction) = {
    /*
    1. Find the neighbour actorref
    2. Send the msg
    * */
    for (neighbour <- neighbourMap.valuesIterator) {
      tx.ttl -= 1
      if (tx.ttl > 0) {neighbour ! NewTx(tx)}
      else {//log.info("Transaction dropped. TTL expired")
         }
    }
  }

  def initializeBootNode(accList: ListBuffer[Account]) = {
    evmPrimaryActor ! InitializeBlockchain(setting.GenesisBlock, accList)
  }


  def copyRequest() = {
    log.info("###############BLOCKCHAIN COPY REQ: RECEIVED")
    implicit val timeout = Timeout(50 seconds)
    val blocks = redis.getAllBlocks()
    val future = evmPrimaryActor ? GetGhostDepth()
    val depth = Await.result(future, timeout.duration).asInstanceOf[GHOST_DepthSet]
    val tuple = new Tuple3[String, ListBuffer[Block],GHOST_DepthSet](client.id, blocks, depth)
    sender ! tuple
  }

  def blockchainResponse(clientId:String, blocks: ListBuffer[Block], depth: GHOST_DepthSet) = {
    log.info("Blockchain copy respnse ......")
    //Update the node state to : Copying the blockchain
    blocks.foreach(block => {
      redis.putBlock(block);
      //Deviation from the regular message sending. Fetching values directly from REDIS.
      val stateMap = redis.getWorldState(block.id, clientId)
      val txState = redis.getTxState(block.id,clientId)
      redis.putTxState(block.id, txState)
      for (acc <- stateMap.valuesIterator) {redis.putAccount(acc, block.id)}
      evmPrimaryActor ! UpdateGhostDepth(depth)
    })
    log.info("$$$$$$$$$$$$$$$$BLOCKCHAIN COPY RESPONSE: RECEIVED")
  }

  override def unhandled(message: Any): Unit = {
    // This message type is not handled by the TxPoolerActor
    // Write the msg details in the log
    //log.info( message.toString() + "Message type not handled in Node Actor")
  }
}
