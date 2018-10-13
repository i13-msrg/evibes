package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef}
import com.vibes.ethereum.models.Transaction
import akka.event.Logging
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.AccountingActor.PoolerStarted

import scala.collection.mutable



object TxPoolerActor {
  case class AddTxToPool(tx: Transaction)
  case class InternalBlockCreated(txList: mutable.ListBuffer[Transaction])
}

class TxPoolerActor(evmPrimary: ActorRef, accountingActor: ActorRef, setting: Setting.type ,clientID: String) extends Actor{
  accountingActor ! PoolerStarted
  import TxPoolerActor._
  import AccountingActor._

  // Send more transactions than are allowed in blockGasLimit. As there might be some tx that might get rejected
  // This change is to speed up the execution.
  private val blockGasLimit: Double =  setting.blockGasLimit + (setting.blockGasLimit * 0.20)
  private val poolSize: Int = setting.poolSize
  private val txPool = mutable.PriorityQueue.empty[Transaction](Ordering.by(txOrder))
  private var gasInPool: Float = 0
  private var txList = new mutable.ListBuffer[Transaction]
  accountingActor ! PoolerInitialized

  val log = Logging(context.system, this)
  override def preStart(): Unit = {
    log.debug("Starting TxPoolerActor")
    println("Starting TxPoolerActor :" + this.clientID)
    accountingActor ! PoolerStart
  }



  override def postStop(): Unit = {
      accountingActor ! PoolerStopped
      super.postStop()
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
      for(t <- txPool) {if(tx.equals(t)) return} //Don't add same tx twice
      txPool.enqueue(tx)
      println(f"Added to the pool. Transaction ID: $tx.id")
      var size = txPool.length

    println(f"Pool size : $size")
      gasInPool += tx.gasLimit
    println(f"Gas in Pool : $gasInPool")
    if (isGasLimitReached) {
      processTxGroup()
    }
  } else {
    // If the pool is full drop the transaction
    // ref : https://medium.com/blockchannel/life-cycle-of-an-ethereum-transaction-e5c66bae0f6e
      accountingActor ! TxPoolFull
      println("Transaction pool full")
      println(f"Dropping Transaction ID: $tx.id")
  }
}

def isGasLimitReached: Boolean = {
  if (gasInPool >= blockGasLimit)  true
  else false
}

  def processTxGroup(): Boolean = {
    if(isGasLimitReached) {
      var gasCount: Float = 0
      println("GasLimit reached")
      // Dequeue all the transactions from the queue
      while(gasCount >= blockGasLimit) {
        // Create a list of transactions
        var tx: Transaction = txPool.dequeue() //should we check empty dequeue ??
        gasCount += tx.gasLimit
        txList += tx
        println(f"Added transaction to Block. ID: $tx.id")
      }
      println("Send the Transaction LIst to EVMPrimaryActor")
      gasCount = 0
      //A probability function that determines if the PoW is right. If False skip block creation

      // TODO: Change this to reflect real ethereaum PoW probability
      if (scala.util.Random.nextInt(10) == 1) {
        evmPrimary ! InternalBlockCreated(txList)
        return true
      }
      else {return false}
    }
    else {return false}
  }

  override def unhandled(message: Any): Unit = {
    // This message type is not handled by the TxPoolerActor
    // Write the msg details in the log
    println("Message type not handled in TxPooler")
  }
}

