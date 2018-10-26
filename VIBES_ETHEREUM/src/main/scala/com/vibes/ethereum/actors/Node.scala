package com.vibes.ethereum.actors

import akka.actor.{Actor, ActorPath, ActorRef, Props}
import akka.event.Logging
import com.vibes.ethereum.actors.ethnode.{EvmPrimary, TxPoolerActor}
import com.vibes.ethereum.models.{Account, Block, Client, Transaction}
import com.vibes.ethereum.service.RedisManager
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.AccountingActor._

import scala.collection.mutable
import scala.collection.mutable.{HashMap, ListBuffer}
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Node {
  case class NewTx(tx: Transaction)
  case class NewBlock(block: Block)

  case class InitializeFullNode(bootNode: ActorRef)
  case class InitializeBootNode(accList: ListBuffer[Account])
  case class BlockchainCopyRequest()
  case class BlockchainCopyResponse(accountList: ListBuffer[Account], blocks: ListBuffer[Option[Block]])
  case class CreateAccount(account: Account)

  case class PropogateToNeighbours(block: Block)

  case class AddNeighbours(neighbours:HashMap[String, ActorRef])
  case class NeighbourCopyRequest(clientId: String)
  case class NeighbourCopyResponse(neighbourMap: mutable.HashMap[String, ActorRef])
  case class NodeDiscoveryPing(clientId: String)
  case class NodeDiscoveryPong(clientId: String)
}

class Node(client: Client, reducer: ActorRef, accountingActor: ActorRef, bootNode: Option[ActorRef], setting: Setting.type ) extends Actor {
  accountingActor ! NodeStart
  val log = Logging(context.system, this)
  log.debug("Starting Node")
  //println("Starting Node : " + client.id)
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
          if (neighbourStatus.get(key) == false) {
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
    case CreateAccount(account: Account) => createAccount(account)
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
    case BlockchainCopyRequest() => copyRequest()
    case BlockchainCopyResponse(accountList: ListBuffer[Account], blocks: ListBuffer[Option[Block]]) => blockchainResponse(accountList, blocks)
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
    initiateBlockchainCopy(bNode)
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
      else {//println("Transaction dropped. TTL expired")
         }
    }
  }

  def initializeBootNode(accList: ListBuffer[Account]) = {
    for (acc <- accList) {
      createAccount(acc)
    }
    evmPrimaryActor ! InitializeParentBlock(setting.GenesisBlock)
  }


  def initiateBlockchainCopy(bootNode: ActorRef) = {
        bootNode ! BlockchainCopyRequest
  }

  def copyRequest() = {
    var accountDetailsList = new ListBuffer[Account]()
    for (accId <- accountList) {
      accountDetailsList += redis.getAccount(accId).get
    }
    val blocks = redis.getAllBlocks()
    sender ! BlockchainCopyResponse(accountDetailsList, blocks)
  }

  def blockchainResponse(accList: ListBuffer[Account], blocks: ListBuffer[Option[Block]]) = {
    //Update the node state to : Copying the blockchain
    var latestBlock = new Block(_transactionList = new ListBuffer[Transaction])
    latestBlock.timestamp_=(0L)

    accList.foreach(acc => {redis.putAccount(acc); accountList += acc.address})
    blocks.foreach(opBlock => opBlock.foreach(block => {redis.putBlock(block);
      if (block.timestamp > latestBlock.timestamp) {latestBlock = block}}))
    evmPrimaryActor ! InitializeParentBlock(latestBlock)
  }

  def createAccount(account: Account) = {
    val key = redis.putAccount(account)
    accountList += key.get
    //println("Account Created Successfully in Node")
  }

  override def unhandled(message: Any): Unit = {
    // This message type is not handled by the TxPoolerActor
    // Write the msg details in the log
    //println( message.toString() + "Message type not handled in Node Actor")
  }
}
