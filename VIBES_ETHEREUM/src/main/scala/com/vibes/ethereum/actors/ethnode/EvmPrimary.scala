package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.vibes.ethereum.actors.ethnode.AccountingActor._
import com.vibes.ethereum.models.{Account, Block, Client, Transaction}
import com.vibes.ethereum.actors.ethnode.TxPoolerActor._
import com.vibes.ethereum.actors.Node

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random


object EvmPrimary {
  case class InternalBlockCreated(txList : mutable.ListBuffer[Transaction])
  case class NewExtBlock(block: Block)
  case class InitializeGenesisBlock(block: Block)
}

import scala.collection.mutable
import com.vibes.ethereum.service.RedisManager
import scala.collection.mutable.HashMap

class EvmPrimary(client: Client, redis: RedisManager, accountingActor: ActorRef, nodeActor: ActorRef) extends Actor{
  val log = Logging(context.system, this)
  accountingActor ! EvmStart
  log.debug("Starting EvmPrimary")
  println("Starting EvmPrimary : " + client.id)
  accountingActor ! EvmStarted
  var accountsAffected = new HashMap[String, Account]
  var txListLocalContext = new HashMap[String, HashMap[String, Float]]
  val miner: Account = client.account
  //TODO: Defining a Genesis block and deciding on how the eth architecture will be created.: 1 node and then multiple or many nodes at once all having genesis block
  var parent: Block = new Block(_transactionList = new mutable.ListBuffer[Transaction]) // There will be a genesis block here
  accountingActor ! EvmInitialized


  override def postStop(): Unit = {
    accountingActor ! EvmStopped
      super.postStop()
  }

  import EvmPrimary._
  import Node._

  override def receive: Receive = {
    case InternalBlockCreated(txList : mutable.ListBuffer[Transaction]) => {accountingActor ! StartMining; blockCreate(txList)}
    case NewExtBlock(block: Block) => handleNewBlock(block)
    case InitializeGenesisBlock(block: Block) => initializeGenesisBlock(block)
    case _ => unhandled(message = AnyRef)
  }



  def initializeGenesisBlock(block: Block) = {
    parent = block
  }


  def handleNewBlock(block: Block) = {
    accountingActor ! BlockVerificationStarted
    var anscBlock = redis.getBlock(block.parentHash)
    anscBlock match {
      case Some(anscBlock) => {
        parent = anscBlock
        if (blockCreate(block.transactionList)) {accountingActor ! BlockVerified(System.currentTimeMillis() / 1000, parent)}
      }
      case _ => {println("Invalid Block. DROPPING")}
    }
  }

  def blockCreate(txList : mutable.ListBuffer[Transaction]): Boolean = {
    if(txList.isEmpty) {println("Empty tx list received"); return false}
    for(tx <- txList){
      if(redis.getTx(tx.id) == None) {
        executeTx(tx, miner, calculateBlockGasLimit(parent.gasLimit))
      }
    }
    // Add the items in local context in the DB
    //Add the updated sender and receiver in the database
    val block: Block = new Block(_transactionList = txList)
    block.parentHash = parent.id
    block.gasLimit = calculateBlockGasLimit(parent.gasLimit)
    block.beneficiary = miner.address
    block.gasUsed = getGasUsed()
    block.number = getBlockNumber(parent.number)
    block.timestamp = System.currentTimeMillis() / 1000
    block.difficulty = calculateDifficulty(block.number, parent.difficulty, block.timestamp, parent.timestamp)
    block.transactionList = txList

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
    //TODO: Update other fields in the block
    redis.putBlock(block)
    println("Block Created Successfully")
    accountingActor ! BlockGenerated(System.currentTimeMillis() / 1000, parent)
    parent = block
    //TODO:Send a PropogateBlock Message to NetworkMgrActor
    accountingActor ! BlockPropogated
    nodeActor ! PropogateToNeighbours(block)
    return true
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

  def executeTx(tx: Transaction, miner: Account, blockGasLimit: Float): Any ={
    val sender = getLocalAccount(tx.sender).getOrElse(default = return )
    if (!sender.isInstanceOf[Account]) {println("Sender Or receiver are new. Skipping tx"); return}
    if(isValidTx(sender, tx, blockGasLimit)) {
      /*
      * 1. Reduce TgTp from Sender balance
      * 2. Increment nonce of sender by 1
      *
      * 3. g = Tg - g0 (gas remaining after deducting from sender = Tx Gas - intrensic gas)
      * 4. Call EVM secondary if contract
      * 5. Call estimate to get estimated gas usage
      * 6. calculate amount to be refunded
      *
      * Provisional state:
      * 7. refund the amount to sender
      * 8. Add (txGasLimit-refund gas)*TxPrice to the benificiary account
      *
      * Final State:
      * 9. Delete all acconts that appear in the sucide list // will be impelemted once the contract exec is done
      * 10. Total gas used in tx = Transaction gas limit - remaining gas (g')
      * 11. Save Al = logs created by transaction execution // not implemented
      * 10 and 11 are needed for tx Logs
      *
      * */

      sender.balance -= tx.gasLimit*tx.gasPrice
      sender.nonce += 1
      val gasRemaining = tx.gasLimit - getGasUsed()
      //Here call EVM secondary for contracts
      // Send a request to estimate gas reqd(g') if contracts

      //For transfer transactions gasRemaining is same (g == g')
      // Ar = 0 in our case... No SSTORE at this moment
      var gasRefund = gasRemaining
      val gasRefund_1 = ((tx.gasLimit - gasRemaining)/2).floor
      if (gasRefund_1 < 0) gasRefund += gasRefund_1 else gasRefund +=0
      sender.balance += (gasRefund * tx.gasPrice)
      miner.balance += (tx.gasLimit - gasRefund)*tx.gasPrice

      accountsAffected.put(sender.address, sender)
      accountsAffected.put(miner.address, miner)

      val accAffected = new HashMap[String, Float]
      accAffected.put(sender.address, sender.balance)
      accAffected.put(miner.address, miner.balance)
      txListLocalContext.put(tx.id, accAffected)
      println(f"Transaction $tx executed successfully")
    }
    else {
      //Send the transaction to the tx pool for executing it later, when the conditions are met
      context.sender ! AddTxToPool(tx)
    }

  }

  def isValidTx(sender: Account, tx: Transaction, blockGasLimit: Float): Boolean = {
    val g0 = getGasUsed()
    val v0 = calculateUpfrontCost(tx) //Upfront cost

    if ((sender.nonce != 0) && (tx.nonce == sender.nonce) && (g0 <= tx.gasLimit)
      && (v0 <= sender.balance) && (tx.gasLimit <= (blockGasLimit - g0))) return true else return false
  }


  def calculateBlockGasLimit(parentBlockLimit: Float): Float =  {
    // Miners have a right to set the gasLimit of the current block to be within ~0.0975 (1/1024) of the gas
    // limit of the last block. So the gas limit is a median of miner preferences (Yellow paper)
    val factor = parentBlockLimit/1024
    val res = Random.shuffle(List(0,1)).take(1)
    if(res == 0) return parentBlockLimit + factor else parentBlockLimit - factor
  }


  def calculateDifficulty(blockNumber: Long, parentDifficulty: Double, currTimestamp: Long, parentTimestamp: Long): Long = {
    val homesteadBlock = 150000
    val D0 = 131072L
    val epsilon = scala.math.pow(2,((blockNumber/100000).floor -2)).floor
    val x = (parentDifficulty/2048)

    if (blockNumber == 0 ) {return D0}
    else if(blockNumber < homesteadBlock) {
      var sigma1 = -1
      if (currTimestamp < (parentTimestamp + 13)) {sigma1 =  1}
      val D1 = parentDifficulty + (x*sigma1) + epsilon
      if(D0 > D1) return D0 else return D1.toLong
    }
    else {
      val sigma2_1 = 1 - ((currTimestamp - parentTimestamp)/10).floor
      val sigma2 = if (sigma2_1 > -99) sigma2_1 else -99
      val D2 = parentDifficulty + (x*sigma2) + epsilon

      if (D0 > D2) return D0 else return D2.toLong
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

  //https://medium.com/@blockchain101/estimating-gas-in-ethereum-b89597748c3f
  def getGasUsed(): Long = {return 21000}

  def calculateUpfrontCost(tx: Transaction): Float = {
    (tx.gasLimit * tx.gasPrice) + tx.value
  }



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
