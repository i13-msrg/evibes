package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.SourceQueueWithComplete
import com.vibes.ethereum.actors.Reducer._
import com.vibes.ethereum.helpers.{EvmState, NodeState, TxPoolState}
import com.vibes.ethereum.models.{Block, Stats}



case class EventJson(id:String, attr: String, value:String)

object AccountingActor {
  //EVM related events
  case class EvmStart()
  case class EvmStarted()
  case class EvmInitialized()
  case class StartMining()
  case class EvmIdle()
  case class BlockGenerated(startTimestamp: Long, block: Block)
  case class BlockPropogated()
  case class BlockVerificationStarted()
  case class BlockVerified(startTimestamp: Long, block: Block)
  case class EvmStopped()

  //TxPooler related events
  case class PoolerStart()
  case class PoolerStarted()
  case class PoolerInitialized()
  case class PoolerHeartbeat(txCount: Int, GasAcc: Float)
  case class TxPoolFull()
  case class PoolerStopped()

  //Node related events
  case class NodeStart()
  case class NodeStarted()
  case class NodeInitialized()
  case class NeighbourRemoved(neighbourAddr: String)
  case class NeighbourAdded(neighbourAddr: String)
  case class Discover()
  case class NodeStop()
}



class AccountingActor(clientId: String, evmQueue: SourceQueueWithComplete[EventJson], reducer: ActorRef) extends Actor{
  println("Accounting actor started for " + clientId)
  var evmState  = EvmState.STOPPED
  var nodeState = NodeState.STOPPED
  var txPoolState = TxPoolState.STOPPED
  var stats = new Stats()


  def updateStats(stats: Stats, startTimestamp: Long, block: Block): Stats = {
    stats.blockNum += 1
    stats.timestamp = System.currentTimeMillis() / 1000
    stats.difficulty = block.difficulty
    stats.avgDifficulty = stats.calcAvg(stats.blockNum, stats.avgDifficulty, stats.difficulty)
    stats.blockTime = block.timestamp - startTimestamp
    stats.avgBlockTime = stats.calcAvg(stats.blockNum, stats.avgBlockTime, stats.blockTime)
      /*
      stats.txCost =
      stats.avgTxCost =
      stats.uncleCount =
      stats.avgUncleCount =
      */
    stats.gasSpending = block.gasUsed
    stats.avgGasSpending = stats.calcAvg(stats.blockNum, stats.avgGasSpending, stats.gasSpending)
    stats.gasLimit = block.gasLimit
    stats.avgGasLimit = stats.calcAvg(stats.blockNum, stats.avgGasLimit, stats.gasLimit)

    return stats
  }

  import AccountingActor._
  override def receive: Receive = {
    case BlockGenerated(startTimestamp, block) => {
      stats = updateStats(stats, startTimestamp, block)
      self ! EvmIdle
      reducer ! StatsUpdate(clientId, stats)
    }
    case BlockVerified(startTimestamp: Long, block: Block) => {
      stats = updateStats(stats, startTimestamp, block)
      stats.propTime = startTimestamp - block.timestamp
      stats.avgPropTime = stats.calcAvg(stats.blockNum, stats.avgPropTime, stats.propTime).toLong
      self ! EvmIdle
      reducer ! StatsUpdate(clientId, stats)
    }

    case PoolerHeartbeat(txCount, gasAcc) => {
      stats.pendingTx = txCount
      stats.avgPendingTx = stats.calcAvg(stats.blockNum, stats.avgPendingTx, stats.pendingTx)
      stats.poolGasAcc = gasAcc
      txPoolState = TxPoolState.ACCEPTING_TX
      reducer ! StatsUpdate(clientId, stats)
    }


    case NeighbourRemoved(neighbourAddr: String) => {
      stats.peers -= 1
      stats.avgPeers = stats.calcAvg(stats.blockNum, stats.avgPeers, stats.peers)
      reducer ! NodeUpdate(clientId, stats, neighbourAddr, true)
    }

    case NeighbourAdded(neighbourAddr: String) => {
      stats.peers += 1
      stats.avgPeers = stats.calcAvg(stats.blockNum, stats.avgPeers, stats.peers)
      reducer ! NodeUpdate(clientId, stats, neighbourAddr, false)
    }

    case EvmStart => {evmState = EvmState.STARTING; evmQueue offer EventJson(clientId, "evmState", "Starting")}
    case EvmStarted => {evmState = EvmState.INITIALIZING; evmQueue offer EventJson(clientId, "evmState", "Started")}
    case EvmInitialized => {evmState = EvmState.IDLE; evmQueue offer EventJson(clientId, "evmState", "Initialized")}
    case EvmIdle => {evmState = EvmState.IDLE; evmQueue offer EventJson(clientId, "evmState", "Idle")}
    case StartMining => {evmState = EvmState.MINING; evmQueue offer EventJson(clientId, "evmState", "Started Mining")}
    case BlockPropogated => {evmState = EvmState.IDLE; evmQueue offer EventJson(clientId, "evmState", "Block Propogated")}
    case BlockVerificationStarted => {evmState = EvmState.VERFYING; evmQueue offer EventJson(clientId, "evmState", "Block Verification Started")}
    case EvmStopped => {evmState = EvmState.STOPPED; evmQueue offer EventJson(clientId, "evmState", "Stopped")}

    case PoolerStart => {txPoolState = TxPoolState.STARTING; evmQueue offer EventJson(clientId, "poolState", "Starting")}
    case PoolerStarted => {txPoolState = TxPoolState.INITIALIZING; evmQueue offer EventJson(clientId, "poolState", "Started")}
    case PoolerInitialized => {txPoolState = TxPoolState.IDLE; evmQueue offer EventJson(clientId, "poolState", "Initialized")}
    case PoolerStopped => {txPoolState = TxPoolState.STOPPED; evmQueue offer EventJson(clientId, "poolState", "Stopped")}
    case TxPoolFull => {txPoolState = TxPoolState.REJECTING_TX; evmQueue offer EventJson(clientId, "poolState", "Tx Pool Full")}

    case NodeStart() => {nodeState = NodeState.STARTING; evmQueue offer EventJson(clientId, "nodeState", "Starting")}
    case NodeStarted() => {nodeState = NodeState.INITIALIZING; evmQueue offer EventJson(clientId, "nodeState", "Initializing")}
    case NodeInitialized() => {nodeState = NodeState.ACCEPTING_CONN; evmQueue offer EventJson(clientId, "nodeState", "Accepting Connections")}
    case Discover() => {nodeState = NodeState.DISCOVER; evmQueue offer EventJson(clientId, "nodeState", "Discovery")}
    case NodeStop() => {nodeState = NodeState.STOPPED; evmQueue offer EventJson(clientId, "nodeState", "Stopped")}
  }
}
