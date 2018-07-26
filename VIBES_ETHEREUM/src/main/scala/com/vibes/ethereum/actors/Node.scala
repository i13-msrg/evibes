package com.vibes.ethereum.actors

import akka.actor.Actor
import akka.event.Logging
import com.vibes.ethereum.models.Client
import com.vibes.ethereum.service.RedisManager

object Node {
  case class StartNode(client: Client)
}

class Node extends Actor{
  val log = Logging(context.system, this)

  override def preStart(): Unit = {
    log.debug("Starting Node")
    println("Starting Node")
  }

  import Node._
  override def receive: Receive = {
    case StartNode(client: Client) => nodeStart(client)
  }

  def nodeStart(client: Client) = {
    println("Client with id:" + client.id + " creation msg recvd successfully")
    val redis = new RedisManager(client.id)
    val key = redis.putClient(client)
    println("Client saved as : " + key)
    val clnt = redis.getClient(client.id)
    println("Client retrieved as : " + clnt.id)
  }
}
