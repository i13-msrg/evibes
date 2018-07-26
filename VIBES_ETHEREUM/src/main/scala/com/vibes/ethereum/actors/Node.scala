package com.vibes.ethereum.actors

import akka.actor.Actor
import akka.event.Logging
import com.vibes.ethereum.models.{Client, Transaction}
import com.vibes.ethereum.service.RedisManager

object Node {
  case class StartNode()
  case class NewTx(tx: Transaction)
}

class Node(client: Client) extends Actor{
  val log = Logging(context.system, this)

  override def preStart(): Unit = {
    log.debug("Starting Node")
    println("Starting Node : " + client.id)
  }

  import Node._
  override def receive: Receive = {
    case StartNode() => nodeStart()
    case NewTx(tx) => {println("New Tx recvd:" + client.id + "TX:" + tx.id)}
  }

  def nodeStart() = {
    println("Client with id:" + client.id + " creation msg recvd successfully")
    val redis = new RedisManager(client.id)
    val key = redis.putClient(client)
    println("Client saved as : " + key)
    val clnt = redis.getClient(client.id)
    println("Client retrieved as : " + clnt.id)
  }
}
