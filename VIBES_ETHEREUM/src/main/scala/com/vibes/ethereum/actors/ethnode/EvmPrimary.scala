package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.vibes.ethereum.actors.ethnode.AccountingActor._
import com.vibes.ethereum.models._
import com.vibes.ethereum.actors.ethnode.TxPoolerActor._
import com.vibes.ethereum.actors.Node

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random


object EvmPrimary {
  case class InternalBlockCreated(txList : mutable.ListBuffer[Transaction])
  case class NewExtBlock(block: Block)
  case class InitializeBlockchain(block: Block, accountList: ListBuffer[Account])
  case class CreateAccountEVM(account: Account)
  case class GetGhostDepth()
  case class UpdateGhostDepth(depth: GHOST_DepthSet)
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
  private var _parentBlock: Block = new Block(_transactionList = new mutable.ListBuffer[Transaction]) // There will be a genesis block here
  private var _worldState = new  mutable.HashMap[String, Account]
  //Getter
  def parentBlock = _parentBlock
  def worldState: mutable.HashMap[String, Account] = _worldState
  //Setter
  def parentBlock_= (value:Block) = _parentBlock = value
  def worldState_= (value: mutable.HashMap[String, Account]) = _worldState = value

  accountingActor ! EvmInitialized

  var DepthSet = new GHOST_DepthSet()

  override def postStop(): Unit = {
    accountingActor ! EvmStopped
      super.postStop()
  }

  import EvmPrimary._
  import Node._

  override def receive: Receive = {
    case InternalBlockCreated(txList : mutable.ListBuffer[Transaction]) => {accountingActor ! StartMining; blockCreate(txList)}
    case NewExtBlock(block: Block) => blockVerify(block)
    case InitializeBlockchain(block: Block, accountList: ListBuffer[Account]) => initializeBlockchain(block, accountList)
    case CreateAccountEVM(account: Account) => createAccount(account)
    case GetGhostDepth() => {println("GET GHOST DEPTH") ; sender ! DepthSet}
    case  UpdateGhostDepth(depth: GHOST_DepthSet) => {DepthSet = depth; println("DEPTH-SET INitialized")}
    case _ => unhandled(message = AnyRef)
  }


  def createAccount(acc:Account) = {
    // Crate account with the blockID as parent
    // Append the account to the world state of the blockID
    val bId = DepthSet.getLeafBlock()
    val pBlk = redis.getBlock(bId.blockId)
    pBlk match {case Some(pBlk) => {redis.putAccount(acc, pBlk.id)}}
  }

  def initializeBlockchain(block: Block, accountList: ListBuffer[Account]) = {
    val lb = new LightBlock(block.id, block.parentHash, 1)
    DepthSet.addLightBlock(lb)
    redis.putBlock(block)
    for (acc <- accountList) {
      redis.putAccount(acc, block.id)
    }
    println("Node BLOCKCHAIN Initialized")
  }


  def blockVerify(block: Block) = {
    accountingActor ! BlockVerificationStarted
    val propTime = ((System.currentTimeMillis() / 1000) - block.timestamp)
    val startTime = System.currentTimeMillis() / 1000
    val anscBlock = redis.getBlock(block.parentHash)
    anscBlock match {
      case Some(anscBlock) => {
        parentBlock_= (anscBlock)
        worldState_= (redis.getWorldState(parentBlock.id))
        if (blockComputation(block)) {accountingActor ! BlockVerified(startTime, parentBlock, propTime)}
      }
      case _ => {println("Invalid Block. DROPPING")}
    }
  }

  def blockComputation(block: Block): Boolean = {

    redis.createWorldState(parentBlock.id,block.id)

    val starttime = System.currentTimeMillis() / 1000
    val txList = block.transactionList

    if (txList.isEmpty) {
      println("Empty tx list received"); return false
    }
    for (tx <- txList) {
      if (redis.getTx(tx.id) == None) {
        executeTx(tx, miner, calculateBlockGasLimit(parentBlock.gasLimit), block.id)
      }
    }

    for(key <- accountsAffected.keysIterator) {
      val account = accountsAffected.get(key).get
      redis.putAccount(account, block.id)
      // Update the WorldState with Updated Account
      redis.updateWorldState(account, parentBlock.id, block.id)
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
    block.timestamp = System.currentTimeMillis() / 1000

    val lb = new LightBlock(block.id, block.parentHash,DepthSet.getDepthOfBlock(parentBlock.id))
    DepthSet.addLightBlock(lb)
    redis.putBlock(block)
    println("Block Created Successfully")
    accountingActor ! BlockGenerated(block.timestamp - starttime, block)
    parentBlock_= (block)
    worldState_= (redis.getWorldState(block.id))
    //TODO:Send a PropogateBlock Message to NetworkMgrActor
    accountingActor ! BlockPropogated
    nodeActor ! PropogateToNeighbours(block)
    return true
  }


  def blockCreate(txList : mutable.ListBuffer[Transaction]): Boolean = {
    //Update parent Block
    var bId = DepthSet.getLeafBlock()
    var pBlk = redis.getBlock(bId.blockId)
    pBlk match {case Some(pBlk) => {parentBlock_= (pBlk); worldState_= (redis.getWorldState(pBlk.id))}}

    val block: Block = new Block(_transactionList = txList)

    block.parentHash = parentBlock.id
    block.gasLimit = calculateBlockGasLimit(parentBlock.gasLimit)
    block.beneficiary = miner.address
    block.gasUsed = getGasUsed()
    block.number = getBlockNumber(parentBlock.number)
    block.difficulty = calculateDifficulty(block.number, parentBlock.difficulty, block.timestamp, parentBlock.timestamp)
    block.transactionList = txList
    blockComputation(block)
  }

/*
  def getOrElseAccount(acc: String, blockId: String,default: Any): Any = {
    var ac = accountsAffected.getOrElse(acc, None)
    if(ac == None) {
      //Not in local context
      ac = redis.getAccount(acc, blockId).getOrElse(acc, None)
      if (ac == None) {return default}
      else {return ac}
    }
    else return ac
  }

  */

  def getOrElseAccount(accAddr: String, blockId: String,default: Any): Any = {
    var ac = accountsAffected.getOrElse(accAddr, None)
    if(ac == None) {
      //Not in local context. Get it from the WorldState
      ac = worldState.get(accAddr)
      if (ac == None) {return default}
      else {return ac}
    }
    else return ac
  }



  def executeTx(tx: Transaction, miner: Account, blockGasLimit: Float, blockId: String): Boolean ={
    /*
    val sender = getLocalAccount(tx.sender).getOrElse(default = return )
    if (!sender.isInstanceOf[Account]) {println("Sender Or receiver are new. Skipping tx"); return}
    */
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

      // New accounts
      //val sender = getAccount(tx.sender).getOrElse(default = return false)
      //val receiver = getAccount(tx.receiver).getOrElse(default = return false)

    val senderAny = getOrElseAccount(tx.sender, blockId, None)
    val receiverAny = getOrElseAccount(tx.receiver, blockId, None)

    if(senderAny.isInstanceOf[Account] & receiverAny.isInstanceOf[Account]) {
      val sender = senderAny.asInstanceOf[Account]
      val receiver = receiverAny.asInstanceOf[Account]

      if (isValidTx(sender, tx, blockGasLimit)) {
        sender.balance -= tx.gasLimit * tx.gasPrice
        sender.nonce += 1
        val gasRemaining = tx.gasLimit - getGasUsed()
        //Here call EVM secondary for contracts
        // Send a request to estimate gas reqd(g') if contracts

        //For transfer transactions gasRemaining is same (g == g')
        // Ar = 0 in our case... No SSTORE at this moment
        var gasRefund = gasRemaining
        val gasRefund_1 = ((tx.gasLimit - gasRemaining) / 2).floor
        if (gasRefund_1 < 0) gasRefund += gasRefund_1 else gasRefund += 0
        sender.balance += (gasRefund * tx.gasPrice)
        miner.balance += (tx.gasLimit - gasRefund) * tx.gasPrice

        //Transaction value transfer
        sender.balance -= tx.value
        receiver.balance += tx.value

        accountsAffected.put(sender.address, sender)
        accountsAffected.put(receiver.address, receiver)
        accountsAffected.put(miner.address, miner)

        val accAffected = new HashMap[String, Float]
        accAffected.put(sender.address, sender.balance)
        accAffected.put(receiver.address, receiver.balance)
        accAffected.put(miner.address, miner.balance)
        txListLocalContext.put(tx.id, accAffected)
        println(f"Transaction $tx executed successfully")
      }
      else {
        //Send the transaction to the tx pool for executing it later, when the conditions are met
        context.sender ! AddTxToPool(tx)
      }
      return true
    }
    return false
  }

  def isValidTx(sender: Account, tx: Transaction, blockGasLimit: Float): Boolean = {
    val g0 = getGasUsed()
    val v0 = calculateUpfrontCost(tx) //Upfront cost

    //Handling the case for new transactions
    if (tx.nonce == 0) {
      tx.nonce_= (sender.nonce)
    }
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
    val homesteadBlock = 10
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
