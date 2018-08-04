package com.vibes.ethereum.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import com.vibes.ethereum.actors.ethnode.{EvmPrimary, TxPoolerActor}
import com.vibes.ethereum.models.{Account, Block, Client, Transaction}
import com.vibes.ethereum.service.RedisManager
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.TxPoolerActor.AddTxToPool

import scala.collection.mutable.ListBuffer

object Node {
  case class StartNode()
  case class NewTx(tx: Transaction)
  case class NewBlock(block: Block)
  case class CreateAccount(account: Account)
}

class Node(client: Client, neighbourName:ListBuffer[String], setting: Setting.type ) extends Actor{
  val log = Logging(context.system, this)
  val evmPrimaryActor: ActorRef = context.actorOf(Props(new EvmPrimary(client.id, context.self)))
  val txPoolerActor: ActorRef = context.actorOf(Props(new TxPoolerActor(evmPrimaryActor, setting, client.id)))
  val redis = new RedisManager(client.id)


  override def preStart(): Unit = {
    log.debug("Starting Node")
    println("Starting Node : " + client.id)
  }

  import Node._
  override def receive: Receive = {
    case NewTx(tx) => {txPoolerActor ! AddTxToPool(tx)}
    case NewBlock(block: Block) => newBlock(block)
    case CreateAccount(account: Account) => createAccount(account)
    case _ => unhandled(message = AnyRef)
  }


  def createAccount(account: Account) = {
    redis.putAccount(account)
    println("Account Created Successfully in Node")
  }

  def newBlock(block : Block) = {
    // Check if the block for these transactions already created.
    // TODO: Verify the working from the reference
  }

  override def unhandled(message: Any): Unit = {
    // This message type is not handled by the TxPoolerActor
    // Write the msg details in the log
    println("Message type not handled in Node Actor")
  }
}
