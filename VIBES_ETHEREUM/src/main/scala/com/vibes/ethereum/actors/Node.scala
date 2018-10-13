package com.vibes.ethereum.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.stream.scaladsl.SourceQueueWithComplete
import com.vibes.ethereum.actors.ethnode.{AccountingActor, EvmPrimary, TxPoolerActor}
import com.vibes.ethereum.models.{Account, Block, Client, Transaction}
import com.vibes.ethereum.service.RedisManager
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.AccountingActor.{NodeInitialized, NodeStart, NodeStarted, NodeStop}

import scala.collection.mutable.ListBuffer

object Node {
  case class NewTx(tx: Transaction)
  case class NewBlock(block: Block)
  case class CreateAccount(account: Account)
}

class Node(client: Client, neighbourName:ListBuffer[String], reducer: ActorRef, accountingActor: ActorRef,  setting: Setting.type ) extends Actor{
  accountingActor ! NodeStarted
  val log = Logging(context.system, this)
  val redis: RedisManager = new RedisManager(client.id)
  val evmPrimaryActor: ActorRef = context.actorOf(Props(new EvmPrimary(client, redis, accountingActor, context.self)))
  val txPoolerActor: ActorRef = context.actorOf(Props(new TxPoolerActor(evmPrimaryActor, accountingActor, setting, client.id)))
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

  import Node._
  import ethnode.EvmPrimary._
  import ethnode.TxPoolerActor._
  import ethnode.AccountingActor._

  override def receive: Receive = {
    case NewTx(tx) => {txPoolerActor ! AddTxToPool(tx)}
    case NewBlock(block: Block) => {evmPrimaryActor ! NewExtBlock(block)}
    case CreateAccount(account: Account) => createAccount(account)
    case _ => unhandled(message = AnyRef)
  }


  def createAccount(account: Account) = {
    //redis.putAccount(account)
    println("Account Created Successfully in Node")
  }

  override def unhandled(message: Any): Unit = {
    // This message type is not handled by the TxPoolerActor
    // Write the msg details in the log
    println( message.toString() + "Message type not handled in Node Actor")
  }
}
