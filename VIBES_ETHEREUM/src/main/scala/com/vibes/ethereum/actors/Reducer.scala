package com.vibes.ethereum.actors
import akka.actor.Actor
import akka.stream.scaladsl.SourceQueueWithComplete
import com.vibes.ethereum.models.Stats

import scala.collection.mutable
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global


case class StatsJson(blockNum : Int, blockTime : Double, difficulty : Double, txCost : Double,
                     gasSpending : Double, gasLimit : Double, uncleCount : Double, peers : Double,
                     pendingTx : Double, propTime : Long, timestamp : Long)


case class LocalStatsJson(clientId: String, blockNum : Int, timestamp : Long, blockTime : Double, avgBlockTime : Double,
                           difficulty : Double, avgDifficulty : Double, txCost : Double, avgTxCost : Double,
                           gasSpending : Double, avgGasSpending : Double, gasLimit : Double, avgGasLimit : Double,
                           uncleCount : Double, avgUncleCount : Double, peers : Double, avgPeers : Double,
                           pendingTx : Double, avgPendingTx : Double, poolGasAcc : Double, propTime : Long, avgPropTime : Long)




object Reducer {
  case class StatsUpdate(clientId: String, stats: Stats)
  case class NodeUpdate(clientId: String, stats: Stats, nodeAddr: String, peerRemoved: Boolean)
  case class NodeCreated(clientId: String)
}

class Reducer(globalStream: SourceQueueWithComplete[StatsJson], localStream: SourceQueueWithComplete[LocalStatsJson]) extends Actor with akka.actor.ActorLogging {
  log.info("Starting Reducer")
  var nodeStatsMap = new mutable.HashMap[String, Stats]
  var globalStats = new Stats()
  import Reducer._

  override def receive: Receive = {
    case StatsUpdate(clientId, stats) => statsUpdate(clientId, stats)
    case NodeCreated(clientId) => nodeCreated(clientId)
  }

  def updateNodeStatsMap(clientId: String, stats:Stats) = {
    nodeStatsMap.put(clientId, stats)
  }

  def getLatestBlockNum() = {
    var max = -1
    for(stat <- nodeStatsMap.valuesIterator) {
      max = scala.math.max(max, stat.blockNum)
    }
    max
  }


  def getAvgStats() = {
    var avgStats = new Stats()
    for(stat <- nodeStatsMap.valuesIterator) {
      avgStats.avgBlockTime_= (avgStats.avgBlockTime + stat.blockTime)
      avgStats.avgDifficulty_= (avgStats.avgDifficulty + stat.avgDifficulty)
      avgStats.avgTxCost_= (avgStats.avgTxCost + stat.avgTxCost)
      avgStats.avgGasSpending_= (avgStats.avgGasSpending + stat.avgGasSpending)
      avgStats.avgGasLimit_= (avgStats.avgGasLimit + stat.avgGasLimit)
      avgStats.avgUncleCount_= (avgStats.avgUncleCount + stat.avgUncleCount)
      avgStats.avgPeers_= (avgStats.avgPeers + stat.avgPeers)
      avgStats.avgPendingTx_= (avgStats.avgPendingTx + stat.avgPendingTx)
      avgStats.avgPropTime_= (avgStats.avgPropTime + stat.avgPropTime)
    }

    val noOfNodes = nodeStatsMap.size
    avgStats.avgBlockTime_= (avgStats.avgBlockTime / noOfNodes)
    avgStats.avgDifficulty_= (avgStats.avgDifficulty / noOfNodes)
    avgStats.avgTxCost_= (avgStats.avgTxCost / noOfNodes)
    avgStats.avgGasSpending_= (avgStats.avgGasSpending / noOfNodes)
    avgStats.avgGasLimit_= (avgStats.avgGasLimit / noOfNodes)
    avgStats.avgUncleCount_= (avgStats.avgUncleCount / noOfNodes)
    avgStats.avgPeers_= (avgStats.avgPeers / noOfNodes)
    avgStats.avgPendingTx_= (avgStats.avgPendingTx / noOfNodes)
    avgStats.avgPropTime_= (avgStats.avgPropTime / noOfNodes)
    avgStats
  }


  def statsUpdate(clietnID: String, stats:Stats) = {

    localStream offer LocalStatsJson(
      clietnID, stats.blockNum , stats.timestamp, stats.blockTime, stats.avgBlockTime, stats.difficulty, stats.avgDifficulty,
      stats.txCost, stats.avgTxCost, stats.gasSpending, stats.avgGasSpending, stats.gasLimit, stats.avgGasLimit, stats.uncleCount,
      stats.avgUncleCount, stats.peers, stats.avgPeers, stats.pendingTx, stats.avgPendingTx, stats.poolGasAcc, stats.propTime,
      stats.avgPropTime)

    updateNodeStatsMap(clietnID, stats)
    globalStats.blockNum_= (getLatestBlockNum())
    globalStats = getAvgStats()

    //print("Stats Update received")
  }

  def nodeCreated(clientId: String) = {
    nodeStatsMap.put(clientId, new Stats())
  }


  context.system.scheduler.schedule(10 second, 5 second, new Runnable {
    override def run(): Unit = {
      //log.info("SENDING REDUCER DATA OVER THE STREAM")
      globalStream offer StatsJson(
        globalStats.blockNum,
        globalStats.avgBlockTime,
        globalStats.avgDifficulty,
        globalStats.avgTxCost,
        globalStats.avgGasSpending,
        globalStats.avgGasLimit,
        globalStats.avgUncleCount,
        globalStats.avgPeers,
        globalStats.avgPendingTx,
        globalStats.avgPropTime,
        System.currentTimeMillis() / 1000)
    }
  })

}
