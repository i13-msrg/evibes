package com.vibes.ethereum.actors
import akka.actor.Actor
import akka.stream.scaladsl.SourceQueueWithComplete
import com.vibes.ethereum.models.Stats
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
}

class Reducer(globalStream: SourceQueueWithComplete[StatsJson], localStream: SourceQueueWithComplete[LocalStatsJson]) extends Actor{
  println("Starting Reducer")
  var statsQueue = new ListBuffer[Stats]
  var globalStats = new Stats()
  import Reducer._

  override def receive: Receive = {
    case StatsUpdate(clientId, stats) => statsUpdate(clientId, stats)
  }


  def statsUpdate(clietnID: String, stats:Stats) = {
    statsQueue += stats
    globalStats.blockNum = stats.blockNum
    globalStats.avgBlockTime_= (stats.calcAvg(statsQueue.length, globalStats.avgBlockTime, stats.blockTime))
    globalStats.avgDifficulty_= (stats.calcAvg(statsQueue.length, globalStats.avgDifficulty, stats.difficulty))
    globalStats.avgTxCost_= (stats.calcAvg(statsQueue.length, globalStats.avgTxCost, stats.txCost))
    globalStats.avgGasSpending_= (stats.calcAvg(statsQueue.length, globalStats.avgGasSpending, stats.gasSpending))
    globalStats.avgGasLimit_= (stats.calcAvg(statsQueue.length, globalStats.avgGasLimit, stats.gasLimit))
    globalStats.avgUncleCount_= (stats.calcAvg(statsQueue.length, globalStats.avgUncleCount, stats.uncleCount))
    globalStats.avgPeers_= (stats.calcAvg(statsQueue.length, globalStats.avgPeers, stats.peers))
    globalStats.avgPendingTx_= (stats.calcAvg(statsQueue.length, globalStats.avgPendingTx, stats.pendingTx))
    globalStats.avgPropTime_= (stats.calcAvg(statsQueue.length, globalStats.avgPropTime, stats.propTime).toLong)
    print("Stats Update received")

    localStream offer LocalStatsJson(
      clietnID, stats.blockNum , stats.timestamp, stats.blockTime, stats.avgBlockTime, stats.difficulty, stats.avgDifficulty,
      stats.txCost, stats.avgTxCost, stats.gasSpending, stats.avgGasSpending, stats.gasLimit, stats.avgGasLimit, stats.uncleCount,
      stats.avgUncleCount, stats.peers, stats.avgPeers, stats.pendingTx, stats.avgPendingTx, stats.poolGasAcc, stats.propTime,
      stats.avgPropTime)
  }

  context.system.scheduler.schedule(10 second, 10 second, new Runnable {
    override def run(): Unit = {
      println("SENDING REDUCER DATA OVER THE STREAM")
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

      // Reinitialize for the next cycle
      statsQueue.clear()
      globalStats = new Stats()
    }
  })

}
