package com.vibes.ethereum.models

import com.vibes.ethereum.helpers.GuidExtension


@SerialVersionUID(123L)
class Stats(
   private var _blockNum : Int = 0,
   private var _timestamp : Long = 0,
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
  def poolGasAcc = _poolGasAcc


  //Setter
  def blockNum_= (value : Int): Int = blockNum = value
  def timestamp_= (value: Long)=_timestamp = value
  def blockTime_= (value: Double)=_blockTime = value
  def avgBlockTime_= (value: Double): Double= avgBlockTime= value
  def difficulty_= (value: Double) = _difficulty = value
  def avgDifficulty_= (value: Double) =  _avgDifficulty = value
  def txCost_= (value: Double) =  _txCost = value
  def avgTxCost_= (value: Double) = _avgTxCost = value
  def gasSpending_= (value: Double) = _gasSpending = value
  def avgGasSpending_= (value: Double) = _avgGasSpending = value
  def gasLimit_= (value: Double) = _gasLimit = value
  def avgGasLimit_= (value: Double) = _avgGasLimit = value
  def uncleCount_= (value: Double) = _uncleCount = value
  def avgUncleCount_= (value: Double) = _avgUncleCount = value
  def peers_= (value: Double) = _peers = value
  def avgPeers_= (value: Double) = _avgPeers = value
  def pendingTx_= (value: Double) = _pendingTx = value
  def avgPendingTx_= (value: Double) = _avgPendingTx = value
  def propTime_= (value: Long) = _propTime = value
  def avgPropTime_= (value: Long) = _avgPropTime = value
  def poolGasAcc_= (value: Double) = _poolGasAcc = value


  //Utility function: Running average calculation
  def calcAvg(n: Int, oldAvg:Double, newVal:Double): Double = {
    val avg = ((n-1)*oldAvg + newVal)/n
    return avg
  }
}
