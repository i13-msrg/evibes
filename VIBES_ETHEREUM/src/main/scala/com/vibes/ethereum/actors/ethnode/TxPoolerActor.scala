package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef}
import com.vibes.ethereum.models.Transaction
import akka.event.Logging
import com.vibes.ethereum.Setting
import com.vibes.ethereum.actors.ethnode.AccountingActor.PoolerStarted
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable



object TxPoolerActor {
  case class AddTxToPool(tx: Transaction)
  case class InternalBlockCreated(txList: mutable.ListBuffer[Transaction])
}

class TxPoolerActor(evmPrimary: ActorRef, accountingActor: ActorRef, setting: Setting.type ,clientID: String) extends Actor with akka.actor.ActorLogging {
  import TxPoolerActor._
  import AccountingActor._
  accountingActor ! PoolerStart
  //log.info("Starting TxPoolerActor")
  //log.info("Starting TxPoolerActor :" + this.clientID)
  accountingActor ! PoolerStarted

  // Send more transactions than are allowed in blockGasLimit. As there might be some tx that might get rejected
  // This change is to speed up the execution.
  private val blockGasLimit: Double =  setting.blockGasLimit + (setting.blockGasLimit * 0.20)
  private val poolSize: Int = setting.poolSize
  private val txPool = mutable.PriorityQueue.empty[Transaction](Ordering.by(txOrder))
  private var gasInPool: Float = 0
  accountingActor ! PoolerInitialized
  scheduleHeartbeatMsg()

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
    //log.info(f"Received Transaction ID:  $tx.id")
    if (txPool.length !=(poolSize)) {
      for(t <- txPool) {if(tx.equals(t)) return} //Don't add same tx twice
      txPool.enqueue(tx)
      //log.info(f"Added to the pool. Transaction ID: $tx.id")
      var size = txPool.length

    //log.info(f"Pool size : $size")
      gasInPool += tx.gasLimit
    //log.info(f"Gas in Pool : $gasInPool")
    if (isGasLimitReached) {
      // TODO: Change this to reflect real ethereaum PoW probability
      if (scala.util.Random.nextInt(10)%3 == 0) {
        processTxGroup()
      }
    }
  } else {
    // If the pool is full drop the transaction
    // ref : https://medium.com/blockchannel/life-cycle-of-an-ethereum-transaction-e5c66bae0f6e
      accountingActor ! TxPoolFull
      //log.info("Transaction pool full")
      //log.info(f"Dropping Transaction ID: $tx.id")
  }
}

def isGasLimitReached: Boolean = {
  if (gasInPool >= blockGasLimit)  true
  else false
}

  def processTxGroup(): Boolean = {
    var txList = new mutable.ListBuffer[Transaction]()
    if(isGasLimitReached) {
      var gasCount: Float = 0
      //log.info("GasLimit reached")
      // Dequeue all the transactions from the queue

      while (gasCount <= blockGasLimit) {
        // Create a list of transactions
        if(txPool.isEmpty == false) {
          var tx: Transaction = txPool.dequeue() //should we check empty dequeue ??
          gasCount += tx.gasLimit
          txList += tx
        } else {//log.info("Pool is empty.")
          return false}
        //log.info(f"Added transaction to Block. ID: $tx.id")
      }
      //log.info("Send the Transaction LIst to EVMPrimaryActor")
      evmPrimary ! EvmPrimary.InternalBlockCreated(txList)
        txPool.clear()
      gasCount = 0
      return true
    }
    else {return false}
  }

  def scheduleHeartbeatMsg() = {
    context.system.scheduler.schedule(10 second, 15 second, new Runnable {
      override def run(): Unit = {
      accountingActor ! PoolerHeartbeat(txPool.length, gasInPool)
      }
    })
  }

  override def unhandled(message: Any): Unit = {
    log.info("Message type not handled in TxPooler")
  }
}

