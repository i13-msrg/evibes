package com.vibes.ethereum.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import com.vibes.ethereum.actors.ethnode.{EvmPrimary, TxPoolerActor}
import com.vibes.ethereum.models.{Account, Block, Client, Transaction}
import com.vibes.ethereum.service.RedisManager
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.AccountingActor.{NodeInitialized, NodeStart, NodeStarted, NodeStop}

import scala.collection.mutable.ListBuffer

object Node {
  case class NewTx(tx: Transaction)
  case class NewBlock(block: Block)
  case class InitiateBlockchainCopy()
  case class InitializeBootNode(accList: ListBuffer[Account])
  case class BlockchainCopyRequest()
  // Response will return the block id to sopy all the account states in the new block
  case class BlockchainCopyResponse(accountList: ListBuffer[Account])
  case class CreateAccount(account: Account)
  case class PropogateToNeighbours(block: Block)
}

class Node(client: Client, neighbourName:ListBuffer[ActorRef], reducer: ActorRef, accountingActor: ActorRef, bootNode: Option[ActorRef], setting: Setting.type ) extends Actor {
  accountingActor ! NodeStarted
  val log = Logging(context.system, this)
  val redis: RedisManager = new RedisManager(client.id)
  val evmPrimaryActor: ActorRef = context.actorOf(Props(new EvmPrimary(client, redis, accountingActor, context.self)))
  val txPoolerActor: ActorRef = context.actorOf(Props(new TxPoolerActor(evmPrimaryActor, accountingActor, setting, client.id)))
  var accountList = ListBuffer[String]
  import Node._

  accountingActor ! NodeInitialized


  override def preStart(): Unit = {
    log.debug("Starting Node")
    println("Starting Node : " + client.id)
    accountingActor ! NodeStart
  }


  override def postStop(): Unit = {
    accountingActor ! NodeStop
    super.postStop()
  }


  import ethnode.EvmPrimary._
  import ethnode.TxPoolerActor._
  import ethnode.AccountingActor._

  override def receive: Receive = {
    case NewTx(tx) => {txPoolerActor ! AddTxToPool(tx); propogateToNeighboursTx(tx)}
    case NewBlock(block: Block) => {evmPrimaryActor ! NewExtBlock(block)}
    case CreateAccount(account: Account) => createAccount(account)
    case InitializeBootNode(accList: ListBuffer[Account]) => initializeBootNode(accList)
    case InitiateBlockchainCopy => initiateBlockchainCopy()
    case BlockchainCopyRequest => copyRequest()
    case BlockchainCopyResponse(accountList: ListBuffer[Account]) => blockchainResponse(accountList)
    case PropogateToNeighbours(block: Block) => propogateToNeighbours(block)
    case _ => unhandled(message = AnyRef)
  }


  def propogateToNeighbours(block: Block) = {
    /*
    1. Find the neighbour actorref
    2. Send the msg
    * */
    for (neighbour <- neighbourName) {
      neighbour ! NewBlock(block)
    }
  }

  def propogateToNeighboursTx(tx: Transaction) = {
    /*
    1. Find the neighbour actorref
    2. Send the msg
    * */
    for (neighbour <- neighbourName) {
      neighbour ! NewTx(tx)
    }
  }

  def initializeBootNode(accList: ListBuffer[Account]) = {
    for (acc <- accList) {
      createAccount(acc)
    }
    evmPrimaryActor ! InitializeGenesisBlock(setting.GenesisBlock)
  }


  def initiateBlockchainCopy() = {
    bootNode match{
      case Some(bootNode) => {
        bootNode ! BlockchainCopyRequest
      }
      case _ => {println("THis is a Boot Node. Not a secondary node")}
    }
    // Change state to : Initiated blockchain copy
  }

  def copyRequest() = {
    var accountDetailsList = new ListBuffer[Account]()
    for (accId <- accountList) {
      accountDetailsList += redis.getAccount(accId).get
    }
    sender ! BlockchainCopyResponse(accountDetailsList)
  }

  def blockchainResponse(accList: ListBuffer[Account]) = {
    //Update the node state to : Copying the blockchain
    for (acc<- accList) {
      redis.putAccount(acc)
    }
  }

  def createAccount(account: Account) = {
    val key = redis.putAccount(account)
    accountList += key.get
    println("Account Created Successfully in Node")
  }

  override def unhandled(message: Any): Unit = {
    // This message type is not handled by the TxPoolerActor
    // Write the msg details in the log
    println( message.toString() + "Message type not handled in Node Actor")
  }
}
