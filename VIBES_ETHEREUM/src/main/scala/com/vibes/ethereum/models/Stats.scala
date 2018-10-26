package com.vibes.ethereum.models

import com.vibes.ethereum.helpers.GuidExtension
import scala.math.{abs, max}

@SerialVersionUID(123L)
case class Stats(
   private var _blockNum : Int = 1,
   private var _timestamp : Long = 0L,
   private var _blockTime : Double = 0,
   private var _avgBlockTime : Double = 0,
   private var _difficulty : Double = 0,
   private var _avgDifficulty : Double = 0,
   private var _txCost : Double = 0,
   private var _avgTxCost : Double = 0,
   private var _gasSpending : Double = 0,
   private var _avgGasSpending : Double = 0,
   private var _gasLimit : Double = 0,
   private var _avgGasLimit : Double = 0,
   private var _uncleCount : Double = 0,
   private var _avgUncleCount : Double = 0,
   private var _peers : Double = 0,
   private var _avgPeers : Double = 0,
   private var _pendingTx : Double = 0,
   private var _avgPendingTx : Double = 0,
   private var _poolGasAcc : Double = 0,
   private var _propTime : Long = 0,
   private var _nodeType : String = "",
   private var _avgPropTime : Long = 0) extends GuidExtension with Serializable {

  override def toString: String = {
    f"Stats ID : $id.  Timestamp: $timestamp"
  }

  private val _id = generateGUID()

  //Getter
  def id = _id
  def blockNum = _blockNum
  def timestamp = _timestamp
  def blockTime = _blockTime
  def avgBlockTime = _avgBlockTime
  def difficulty = _difficulty
  def avgDifficulty = _avgDifficulty
  def txCost = _txCost
  def avgTxCost = _avgTxCost
  def gasSpending = _gasSpending
  def avgGasSpending = _avgGasSpending
  def gasLimit = _gasLimit
  def avgGasLimit = _avgGasLimit
  def uncleCount = _uncleCount
  def avgUncleCount = _avgUncleCount
  def peers = _peers
  def avgPeers = _avgPeers
  def pendingTx = _pendingTx
  def avgPendingTx = _avgPendingTx
  def propTime = _propTime
  def avgPropTime = _avgPropTime
  def nodeType : String = _nodeType
  def poolGasAcc = _poolGasAcc


  //Setter
  def blockNum_= (value : Int): Unit = _blockNum = value
  def timestamp_= (value: Long): Unit = _timestamp = value
  def blockTime_= (value: Double): Unit=_blockTime = value
  def avgBlockTime_= (value: Double): Unit= _avgBlockTime = value
  def difficulty_= (value: Double): Unit = _difficulty = value
  def avgDifficulty_= (value: Double): Unit =  _avgDifficulty = value
  def txCost_= (value: Double): Unit =  _txCost = value
  def avgTxCost_= (value: Double): Unit = _avgTxCost = value
  def gasSpending_= (value: Double): Unit = _gasSpending = value
  def avgGasSpending_= (value: Double): Unit = _avgGasSpending = value
  def gasLimit_= (value: Double): Unit = _gasLimit = value
  def avgGasLimit_= (value: Double): Unit = _avgGasLimit = value
  def uncleCount_= (value: Double): Unit = _uncleCount = value
  def avgUncleCount_= (value: Double): Unit = _avgUncleCount = value
  def peers_= (value: Double): Unit = _peers = value
  def avgPeers_= (value: Double): Unit = _avgPeers = value
  def pendingTx_= (value: Double): Unit = _pendingTx = value
  def avgPendingTx_= (value: Double): Unit = _avgPendingTx = value
  def propTime_= (value: Long): Unit = _propTime = value
  def avgPropTime_= (value: Long) : Unit= _avgPropTime = value
  def nodeType_= (value: String) : Unit = _nodeType = value
  def poolGasAcc_= (value: Double): Unit = _poolGasAcc = value


  //Utility function: Running average calculation
  def calcAvg(n: Int, oldAvg:Double, newVal:Double): Double = {
    try {
      val avg = (oldAvg + newVal)/n
      return abs(avg)
    }
    catch {
      case e => {println("Exception Occoured"); return 0}
    }

  }
}
