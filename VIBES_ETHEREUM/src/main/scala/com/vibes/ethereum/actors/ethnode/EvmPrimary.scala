package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.vibes.ethereum.models.{Account, Block, Transaction}

import scala.collection.mutable


object EvmPrimary {
  case class InternalBlockCreated(txList : mutable.ListBuffer[Transaction])
}

import scala.collection.mutable
import com.vibes.ethereum.service.RedisManager
import scala.collection.mutable.HashMap

class EvmPrimary(clientID: String, nodeActor: ActorRef) extends Actor{
  val redis: RedisManager = new RedisManager(clientID)
  var accountsAffected = new HashMap[String, Account]
  var txListLocalContext = new HashMap[String, HashMap[String, Float]]

  import EvmPrimary._
  override def receive: Receive = {
    case InternalBlockCreated(txList : mutable.ListBuffer[Transaction]) => txBlockReady(txList)
    case _ => unhandled(message = AnyRef)
  }

  val log = Logging(context.system, this)

  def txBlockReady(txList : mutable.ListBuffer[Transaction]) = {

    //TODO:A probability function that determines if the PoW is right. If False skip block creation
    for(tx <- txList){
      if(redis.getTx(tx.id) == None) {
        executeTx(tx)
      }
    }
    // Add the items in local context in the DB
    //Add the updated sender and receiver in the database
    val block = new Block(_transactionList = txList)
    for(key <- accountsAffected.keysIterator) {
      redis.putAccount(accountsAffected.get(key).get)
      println("Account added in the Block")
      accountsAffected.remove(key)
    }
    for (tx <- txList){
      redis.putTx(tx, block.id)
      println("Tx added in the Block")
    }

    for (key <- txListLocalContext.keysIterator) {
      val addrMap = txListLocalContext.get(key)
      for (addr <-addrMap.get.keysIterator ) {
        val bal = addrMap.get(addr)
        redis.putTxEntry(key, addr, bal)
        println("txEntry added in the Block")
      }
      txListLocalContext.remove(key)
    }
    redis.putBlock(block)
    println("Block Created Successfully")
    //TODO:Send a PropogateBlock Message to NetworkMgrActor
  }


  def getLocalAccount(acc: String):Option[Account]=  {
    val accLocal = accountsAffected.get(acc)
    if(accLocal == None) {
      //Not in local context
      val accDb = redis.getAccount(acc)
      if (accDb == None) {return None}
      else {return accDb}
    }
    else return accLocal
  }

  def executeTx(tx: Transaction): Any ={
    val sender = getLocalAccount(tx.sender)
    val receiver = getLocalAccount(tx.sender)
    if (sender == None | receiver == None) {println("Sender Or receiver are new. Skipping tx"); return}
    //TODO: Multiple checks should be added here
    if (sender.asInstanceOf[Account].balance > tx.value)  {
      // can be executed
      sender.get.balance = sender.get.balance - tx.value
      receiver.get.balance = receiver.get.balance + tx.value
      accountsAffected.put(sender.get.address, sender.get)
      accountsAffected.put(receiver.get.address, receiver.get)
      var accAffected = new HashMap[String, Float]
      accAffected.put(sender.get.address, sender.get.balance)
      accAffected.put(receiver.get.address, receiver.get.balance)
      txListLocalContext.put(tx.id, accAffected)
      println(f"Transaction $tx executed successfully")

    }
    //TODO: Also count the gas used for the transaction computation
  }

  def calculateDifficulty(blockNumber: Int, parentDifficulty: Float, parentTimestamp: Long, anscBlocks: Int):Double = {
    val homesteadBlock = 150000
    val D0 = 131072
    val epsilon = scala.math.pow(2,((anscBlocks/100000).floor -2)).floor
    val x = (parentDifficulty/2048)
    val currTimestamp = System.currentTimeMillis() / 1000

    if (blockNumber == 0 ) {return D0}
    else if(blockNumber < homesteadBlock) {
      var sigma1 = -1
      if (currTimestamp < (parentTimestamp + 13)) {sigma1 =  1}
      val D1 = parentDifficulty + (x*sigma1) + epsilon

      if(D0 > D1) return D0 else return D1
    }
    else {
      val sigma2_1 = 1 - ((currTimestamp - parentTimestamp)/10).floor
      val sigma2 = if (sigma2_1 > -99) sigma2_1 else -99
      val D2 = parentDifficulty + (x*sigma2) + epsilon

      if (D0 > D2) return D0 else return D2
    }
  }

  def validateGasLimit(blockGasLimit: Double, parentGasLimit : Double): Boolean = {
    val Hl_2 = (parentGasLimit/1024).floor
    if ((blockGasLimit < (parentGasLimit + Hl_2)) &
      (blockGasLimit > (parentGasLimit - Hl_2)) &
      (blockGasLimit >= 125000)) return true else false
  }

  def validateTimestamp(blockTimestamp: Long, parentTimestamp: Long) : Boolean = {
    if (blockTimestamp > parentTimestamp) return true else false
  }

  def getBlockNumber(parentNumber: Long) : Long = {parentNumber + 1}


  //https://github.com/ethereum/wiki/wiki/Design-Rationale#gas-and-fees
  //TODO: There will be some nodes increasing the limit while others decreasing it
  // Try to mimic this scenario.
  def setGasLimit(parentGasLimit: Long): Long = {return parentGasLimit + (parentGasLimit/1024)}


  override def unhandled(message: Any): Unit = {
    // This message type is not handled by the TxPoolerActor
    // Write the msg details in the log
    log.info("Message type not handled in EVMPrimary")
  }
}
