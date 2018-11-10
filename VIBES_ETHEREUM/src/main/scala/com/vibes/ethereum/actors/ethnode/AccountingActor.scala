package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.SourceQueueWithComplete
import com.vibes.ethereum.actors.Reducer._
import com.vibes.ethereum.helpers.{EvmState, NodeState, TxPoolState}
import com.vibes.ethereum.models.{Block, Client, Stats}
import scala.math.{abs, max}


case class EventJson(id:String, attr: String, value:String)

object AccountingActor {
  //EVM related events
  case class EvmStart()
  case class EvmStarted()
  case class EvmInitialized()
  case class StartMining()
  case class EvmIdle()
  case class BlockGenerated(startTimestamp: Long, block: Block)
  case class BlockReceived(propTime: Long)
  case class BlockPropogated()
  case class BlockVerificationStarted()
  case class BlockVerified(startTimestamp: Long, block: Block, PropogationTime: Long)
  case class EvmStopped()

  //TxPooler related events
  case class PoolerStart()
  case class PoolerStarted()
  case class PoolerInitialized()
  case class PoolerHeartbeat(txCount: Int, GasAcc: Float)
  case class TxPoolFull()
  case class PoolerStopped()

  //Node related events
  case class NodeType()
  case class NodeStart()
  case class NodeStarted()
  case class NodeInitialized()
  case class NeighbourUpdate(neighbourCount: Int)
  case class Discover()
  case class NodeStop()
}



class AccountingActor(client: Client, evmQueue: SourceQueueWithComplete[EventJson], reducer: ActorRef) extends Actor with akka.actor.ActorLogging {
  log.info("Accounting actor started for " + client.id)
  var evmState  = EvmState.STOPPED
  var nodeState = NodeState.STOPPED
  var txPoolState = TxPoolState.STOPPED
  var stats = new Stats()


  def updateStats(stats: Stats, startTimestamp: Long, block: Block): Stats = {
    stats.timestamp_= (System.currentTimeMillis() / 1000)
    stats.difficulty_= (block.difficulty)
    stats.nodeType_= (client.clientType)
    stats.avgDifficulty_= (stats.calcAvg(stats.blockNum, stats.avgDifficulty, stats.difficulty))
    stats.blockTime_= (block.timestamp - startTimestamp)
    stats.avgBlockTime_= (stats.calcAvg(stats.blockNum, stats.avgBlockTime, stats.blockTime))
    stats.avgBlockTime_= (abs(stats.avgBlockTime))
      /*
      stats.txCost =
      stats.avgTxCost =
      stats.uncleCount =
      stats.avgUncleCount =
      */
    stats.gasSpending_= (block.gasUsed)
    stats.avgGasSpending_= (stats.calcAvg(stats.blockNum, stats.avgGasSpending, stats.gasSpending))
    stats.gasLimit_= (block.gasLimit)
    stats.avgGasLimit_= (stats.calcAvg(stats.blockNum, stats.avgGasLimit, stats.gasLimit))

    return stats
  }

  import AccountingActor._
  override def receive: Receive = {
    case BlockGenerated(startTimestamp, block) => {
      stats = updateStats(stats, startTimestamp, block)
      stats.blockNum_= (stats.blockNum + 1)
      self ! EvmIdle
      reducer ! StatsUpdate(client.id, stats)
    }
    case BlockVerified(startTimestamp: Long, block: Block, propogationTime: Long) => {
      stats = updateStats(stats, startTimestamp, block)
      stats.propTime_=(propogationTime)
      stats.avgPropTime_= (stats.calcAvg(stats.blockNum, stats.avgPropTime, stats.propTime).toLong)
      self ! EvmIdle
      reducer ! StatsUpdate(client.id, stats)
    }

    case BlockReceived(propTime: Long) => {
      stats.propTime_=(propTime)
      stats.avgPropTime_=(stats.calcAvg(stats.blockNum, stats.avgPropTime, stats.propTime).toLong)
      reducer ! StatsUpdate(client.id, stats)
    }

    case PoolerHeartbeat(txCount, gasAcc) => {
      stats.pendingTx_= (txCount)
      stats.avgPendingTx_= (stats.calcAvg(stats.blockNum, stats.avgPendingTx, stats.pendingTx))
      stats.poolGasAcc_= (gasAcc)
      txPoolState = TxPoolState.ACCEPTING_TX
      reducer ! StatsUpdate(client.id, stats)
    }


    case NeighbourUpdate(neighbourCount: Int) => {
      stats.peers = neighbourCount
      stats.avgPeers = stats.calcAvg(stats.blockNum, stats.avgPeers, stats.peers)
    }


    case EvmStart => {evmState = EvmState.STARTING; evmQueue offer EventJson(client.id, "evmState", "Starting")}
    case EvmStarted => {evmState = EvmState.INITIALIZING; evmQueue offer EventJson(client.id, "evmState", "Initializing")}
    case EvmInitialized => {evmState = EvmState.IDLE; evmQueue offer EventJson(client.id, "evmState", "Idle")}
    case EvmIdle => {evmState = EvmState.IDLE; evmQueue offer EventJson(client.id, "evmState", "Idle")}
    case StartMining => {evmState = EvmState.MINING; evmQueue offer EventJson(client.id, "evmState", "Started Mining")}
    case BlockPropogated => {evmState = EvmState.IDLE; evmQueue offer EventJson(client.id, "evmState", "Block Propogated")}
    case BlockVerificationStarted => {evmState = EvmState.VERFYING; evmQueue offer EventJson(client.id, "evmState", "Block Verification Started")}
    case EvmStopped => {evmState = EvmState.STOPPED; evmQueue offer EventJson(client.id, "evmState", "Stopped")}

    case PoolerStart => {txPoolState = TxPoolState.STARTING; evmQueue offer EventJson(client.id, "poolState", "Starting")}
    case PoolerStarted => {txPoolState = TxPoolState.INITIALIZING; evmQueue offer EventJson(client.id, "poolState", "Initializing")}
    case PoolerInitialized => {txPoolState = TxPoolState.IDLE; evmQueue offer EventJson(client.id, "poolState", "Accepting Transactions")}
    case PoolerStopped => {txPoolState = TxPoolState.STOPPED; evmQueue offer EventJson(client.id, "poolState", "Stopped")}
    case TxPoolFull => {txPoolState = TxPoolState.REJECTING_TX; evmQueue offer EventJson(client.id, "poolState", "Tx Pool Full")}

    case NodeType => {evmQueue offer EventJson(client.id, "nodeType", client.clientType)}
    case NodeStart => {nodeState = NodeState.STARTING; evmQueue offer EventJson(client.id, "nodeState", "Starting")}
    case NodeStarted => {nodeState = NodeState.INITIALIZING; evmQueue offer EventJson(client.id, "nodeState", "Initializing")}
    case NodeInitialized => {nodeState = NodeState.ACCEPTING_CONN; evmQueue offer EventJson(client.id, "nodeState", "Accepting Connections")}
    case Discover => {nodeState = NodeState.DISCOVER; evmQueue offer EventJson(client.id, "nodeState", "Neighbour Discovery")}
    case NodeStop => {nodeState = NodeState.STOPPED; evmQueue offer EventJson(client.id, "nodeState", "Stopped")}
  }
}
