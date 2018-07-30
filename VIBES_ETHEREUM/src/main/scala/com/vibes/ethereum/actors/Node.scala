package com.vibes.ethereum.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import com.vibes.ethereum.actors.ethnode.TxPoolerActor
import com.vibes.ethereum.models.{Block, Client, Transaction}
import com.vibes.ethereum.service.RedisManager
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.TxPoolerActor.AddTxToPool

object Node {
  case class StartNode()
  case class NewTx(tx: Transaction)
  case class NewBlock(block: Block)
}

class Node(client: Client, setting: Setting.type ) extends Actor{
  val log = Logging(context.system, this)
  //val evmPrimaryActor: ActorRef = context.actorOf(Props[EvmPrimary])
  val txPoolerActor: ActorRef = context.actorOf(Props(new TxPoolerActor(setting, client.id)))
  val redis = new RedisManager(client.id)


  override def preStart(): Unit = {
    log.debug("Starting Node")
    println("Starting Node : " + client.id)
  }

  import Node._
  override def receive: Receive = {
    case StartNode() => nodeStart()
    case NewTx(tx) => {txPoolerActor ! AddTxToPool(tx)}
    case NewBlock(block: Block) => newBlock(block)
  }

  def nodeStart() = {
    println("Client with id:" + client.id + " creation msg recvd successfully")
    val redis = new RedisManager(client.id)
    val key = redis.putClient(client)
    println("Client saved as : " + key)
    val clnt = redis.getClient(client.id)
    println("Client retrieved as : " + clnt.id)
  }

  def newBlock(block : Block) = {
    // Check if the block for these transactions already created.
    // TODO: Verify the working from the reference
  }
}
