package com.vibes.ethereum.actors
import akka.actor.Actor
import com.vibes.ethereum.helpers.NodeState.NodeState
import com.vibes.ethereum.helpers.TxPoolState.TxPoolState
import com.vibes.ethereum.helpers.EvmState.EvmState
import com.vibes.ethereum.models.Stats



object Reducer {
  case class statsUpdate(clientId: String, stats: Stats)
  case class nodeUpdate(clientId: String, stats: Stats, nodeAddr: String, peerRemoved: Boolean)
  case class nodeStateUpdate(clientId: String, state: NodeState)
  case class poolStateUpdate(clientId: String, state: TxPoolState)
  case class evmStateUpdate(clientId: String, state: EvmState)
}

class Reducer extends Actor{

  import Reducer._
  override def receive: Receive = {
    case statsUpdate(clientId, stats) => {}
    case nodeUpdate(clientId, stats, nodeAddr, peerRemoved) => {}
    case nodeStateUpdate(clientId, state) => {}
    case poolStateUpdate(clientId, state) => {}
    case evmStateUpdate(clientId, state) => {}
  }
}
