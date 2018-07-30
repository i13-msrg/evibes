package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef, Props}
import com.vibes.ethereum.models.Transaction
import akka.event.Logging
import com.vibes.ethereum.Setting


import scala.collection.mutable


object TxPoolerActor {
  case class AddTxToPool(tx: Transaction)
  case class InternalBlockCreated(txList: mutable.ListBuffer[Transaction])
}

class TxPoolerActor(setting: Setting.type, clientID: String) extends Actor{

  import TxPoolerActor._

  private val blockGasLimit: Float =  setting.blockGasLimit
  private val poolSize: Int = setting.poolSize
  private val txPool = mutable.PriorityQueue.empty[Transaction](Ordering.by(txOrder))
  private var gasInPool: Float = 0
  private var txList = new mutable.ListBuffer[Transaction]

  val log = Logging(context.system, this)
  override def preStart(): Unit = {
    log.debug("Starting TxPoolerActor")
  }

  override def receive: Receive = {
    case AddTxToPool(tx) => handleTx(tx)
    case _ => unhandled(message = AnyRef)
  }

  // Order the transactions by their gasPrice. ref : YellowPaper
  def txOrder(tx: Transaction): Float = tx.gasPrice

  def handleTx(tx: Transaction) : Unit = {
    println(f"Received Transaction ID:  $tx.id")
    if (txPool.length !=(poolSize)) {
      // TODO : Check if the transaction already exists in the txPool. If everything same, dump it else replace
      txPool.enqueue(tx)
      println(f"Added to the pool. Transaction ID: $tx.id")
      var size = txPool.length
      println(f"Pool size : $size")
      // The sum of all the gas limits on the tx cannot exceed the block gas limit
      gasInPool += tx.gasLimit
      println(f"Gas in Pool : $gasInPool")
      if (isGasLimitReached) {
        gasLimitReached()
      }
    } else {
      // If the pool is full drop the transaction
      // ref : https://medium.com/blockchannel/life-cycle-of-an-ethereum-transaction-e5c66bae0f6e
      println("Transaction pool full")
      println(f"Dropping Transaction ID: $tx.id")
    }
  }

  def isGasLimitReached: Boolean = {
    if (gasInPool >= blockGasLimit)  true
    else false
  }

  def gasLimitReached(): Unit = {
    if (isGasLimitReached) {
      var gasCount: Float = 0
      println("GasLimit reached")
      // Dequeue all the transactions from the queue
      while(gasCount != blockGasLimit) {
        // Create a list of transactions
        var tx: Transaction = txPool.dequeue() //should we check empty dequeue ??
        gasCount += tx.gasLimit
        txList += tx
        println(f"Added transaction to Block. ID: $tx.id")
      }
      println("Send the Transaction LIst to EVMPrimaryActor")
      gasCount = 0
      //evmPrimary ! InternalBlockCreated(txList)    // send the list to EVMPrimaryActor
    }
  }

  override def unhandled(message: Any): Unit = {
    // This message type is not handled by the TxPoolerActor
    // Write the msg details in the log
    println("Message type not handled in TxPooler")
  }
}

