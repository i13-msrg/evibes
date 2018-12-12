package com.vibes.ethereum.actors.ethnode

import akka.actor.{Actor, ActorRef}
import com.vibes.ethereum.actors.ethnode.AccountingActor._
import com.vibes.ethereum.models._
import com.vibes.ethereum.actors.ethnode.TxPoolerActor._
import com.vibes.ethereum.actors.Node

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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

class EvmPrimary(client: Client, redis: RedisManager, accountingActor: ActorRef, nodeActor: ActorRef) extends Actor with akka.actor.ActorLogging {
  accountingActor ! EvmStart
  log.info("Starting EvmPrimary")
  log.info("Starting EvmPrimary : " + client.id)
  accountingActor ! EvmStarted
  var accountsAffected = new HashMap[String, Account]
  var txListLocalContext = new HashMap[String, HashMap[String, Float]]
  //val miner: Account = client.account

  //TODO: Defining a Genesis block and deciding on how the eth architecture will be created.: 1 node and then multiple or many nodes at once all having genesis block
  private var _parentBlock: Block = new Block(_transactionList = new mutable.ListBuffer[Transaction]) // There will be a genesis block here
  private var _worldState = new  mutable.HashMap[String, Account]
  private var _txState = new ListBuffer[String]
  //Getter
  def parentBlock = _parentBlock
  def worldState: mutable.HashMap[String, Account] = _worldState
  def txState: ListBuffer[String] = _txState

  //Setter
  def parentBlock_= (value:Block) = _parentBlock = value
  def worldState_= (value: mutable.HashMap[String, Account]) = _worldState = value
  def txState_= (value: ListBuffer[String]) = _txState = value

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
    case GetGhostDepth() => {log.info("GET GHOST DEPTH") ; sender ! DepthSet}
    case UpdateGhostDepth(depth: GHOST_DepthSet) => {DepthSet = depth; log.info("DEPTH-SET INitialized : " + DepthSet.nodeMap.keySet.size.toString); sender ! true}
    case _ => unhandled(message = AnyRef)
  }


  def createAccount(acc:Account) = {
    // Crate account with the blockID as parent
    // Append the account to the world state of the blockID
    val bId = DepthSet.getLeafBlock()
    val pBlk = redis.getBlock(bId.blockId)
    pBlk match {
      case Some(pBlk) => {
        redis.putAccount(acc, pBlk.id, true)
        log.info("====ACCOUNT CREATED =====")
      }
      case None => {
        log.info("Depth Node not yet Initialized. Schedule a sleep of 2 sec and trying again ...")
        context.system.scheduler.scheduleOnce(1 second) {
          val bId = DepthSet.getLeafBlock()
          val pBlk = redis.getBlock(bId.blockId)
          pBlk match {
            case Some(pBlk) => {
              redis.putAccount(acc, pBlk.id, true)
              log.info("====ACCOUNT CREATED =====")
            }
            case None => {
              log.info("NO BLOCK INITIALIZED STILL. SKIPPING")
            }}
        }
      }}
  }

  def initializeBlockchain(block: Block, accountList: ListBuffer[Account]) = {
    log.info("[" +  client.id + "]" +  " [initializeBlockchain] ===== STARTED BLOCKCHAIN INITIALIZATION FOR CLIENT ======")
    val lb = new LightBlock(block.id, block.parentHash, 1)
    DepthSet.addLightBlock(lb)
    redis.putBlock(block)
    for (acc <- accountList) {
      redis.putAccount(acc, block.id, true)
    }
    log.info("[" +  client.id + "]" +  " [initializeBlockchain] ===== BLOCKCHAIN INITIALITED FOR CLIENT ======")
  }


  def blockVerify(block: Block) = {
    log.info( "[" +  client.id + "]"  + "===========BLOCK VERIFICATION STARTED===========")
    log.info("[" +  client.id + "]" +  " [blockVerify] VERIFIY BLOCK: " + block.toString)
    accountingActor ! BlockVerificationStarted
    val propTime = ((System.currentTimeMillis() / 1000) - block.timestamp)
    val startTime = System.currentTimeMillis() / 1000
    val anscBlock = redis.getBlock(block.parentHash)
    anscBlock match {
      case Some(anscBlock) => {
        log.info("[" +  client.id + "]" +  " [blockVerify] Ancestor Block Found in Blockchain." + anscBlock.id)
        parentBlock_= (anscBlock)
        worldState_= (redis.getWorldState(parentBlock.id))
        //txState_= (redis.getTxState(parentBlock.id))
        log.info("[" +  client.id + "]" +  " [blockVerify] Fetched World state of parent block")
        log.info("[" +  client.id + "]" +  " [blockVerify] ## PARENT WORLD STATE:" + worldState.keySet.size)
        if (blockComputation(block)) {
          log.info("[" +  client.id + "]" +  " [blockVerify] SUCCESSFULLY VERIFIED : " + block.id)
          accountingActor ! BlockVerified(startTime, parentBlock, propTime)
        } else {
          log.info("[" +  client.id + "]" +  " [blockVerify] FAILURE WHILE VERIFYING : " + block.id)
        }
      }
      case _ => {
        log.info("[" +  client.id + "]" +  " [blockVerify] Parent block not found in the Blockchain. DROPPING BLOCK" + block.toString)
      }
    }
    log.info( "[" +  client.id + "]"  + "===========BLOCK VERIFICATION ENDED===========")
  }

  def blockComputation(block: Block): Boolean = {
    log.info("[" +  client.id + "]" +  "%%%%% BLOCK COMPUTATION STARTED %%%%%")
    redis.createWorldState(parentBlock.id,block.id)
    redis.createTxState(parentBlock.id,block.id)
    // List of all the transactions originating here. Once the block is generated these transactions are propogated to neighbours.
    // This is done to update the nonce of the transaction
    val TxOrignatingHere = new ListBuffer[Transaction]

    log.info("[" +  client.id + "]" +  " [blockComputation] Created new World state")

    val starttime = System.currentTimeMillis() / 1000
    val txList = block.transactionList
    log.info("[" +  client.id + "]" +  " [blockComputation] Fetch tx list from BLOCK ID: " + block.id)


    if (txList.isEmpty) {
      log.info("[" +  client.id + "]" +  " [blockComputation] Empty tx list received from BLOCK ID: " + block.id); return false
    }
    for (tx <- txList) {
      log.info("[" +  client.id + "]" +  " [blockComputation] TX-NONCE:" + tx.nonce)
      log.info("[" +  client.id + "]" +  " [blockComputation] Tx under consideration: " + tx.id)
      val nonce: (Boolean, Int) = executeTx(tx, calculateBlockGasLimit(parentBlock.gasLimit), block.id)
      if (tx.nonce != nonce._2 && tx.nonce == 0 && nonce._1)  {
        //New transaction. Originates here
        tx.nonce = nonce._2
        TxOrignatingHere += tx
      }
    }

    log.info("[" +  client.id + "]" +  " [blockComputation] Add affected accounts to redisDB")
    log.info("[" +  client.id + "]" +  " [blockComputation] Affected Accounts : " + accountsAffected.toString())
    for(key <- accountsAffected.keysIterator) {
      log.info("[" +  client.id + "]" +  " [blockComputation] ****** UPDATE WORLD STATE BEGIN ******")
      val account = accountsAffected.get(key).get
      redis.putAccount(account, block.id, false)
      log.info("[" +  client.id + "]" +  " [blockComputation] Added account: " + account.address)
      // Update the WorldState with Updated Account
      redis.updateWorldState(account, parentBlock.id, block.id)
      log.info("[" +  client.id + "]" +  " [blockComputation] World State updated in redisDB ")
      log.info("[" +  client.id + "]" +  " [blockComputation] ****** UPDATE WORLD STATE END ******")
    }
    accountsAffected.clear()
    log.info("[" +  client.id + "]" +  " [blockComputation] ****** ADD TX TO BLOCK BEGIN ******")
    for (tx <- txList){
      redis.putTx(tx, block.id)
      log.info("[" +  client.id + "]" +  " [blockComputation] Add tx in Block. TX: " + tx.id)
    }

    log.info("[" +  client.id + "]" +  " [blockComputation] ****** ADD TX TO BLOCK END ******")


    log.info("[" +  client.id + "]" +  " [blockComputation] ****** ADD TX ENTRY TO DB BEGIN ******")
    for (key <- txListLocalContext.keysIterator) {
      val addrMap = txListLocalContext.get(key)
      for (addr <-addrMap.get.keysIterator ) {
        val bal = addrMap.get(addr)
        redis.putTxEntry(key, addr, bal)
        log.info("[" +  client.id + "]" +  " [blockComputation] TX entry added. (key, addr, balance) = (" + key + "," + addr + "," + bal  + ")")
      }
    }
    txListLocalContext.clear()
    log.info("[" +  client.id + "]" +  " [blockComputation] ****** ADD TX ENTRY TO DB END ******")

    //TODO: Update other fields in the block
    block.timestamp = System.currentTimeMillis() / 1000

    val lb = new LightBlock(block.id, block.parentHash, DepthSet.getDepthOfBlock(parentBlock.id) + 1)
    block.number = parentBlock.number + 1
    DepthSet.addLightBlock(lb)
    redis.putBlock(block)
    log.info("[" +  client.id + "]" +  " [blockComputation] ??????? BLOCK CREATED SUCCESSFULLY ???????")
    log.info("[" +  client.id + "]" +  " [blockComputation] NEW BLOCK : " + block.toString)
    accountingActor ! BlockGenerated(block.timestamp - starttime, block)
    parentBlock_= (block)
    worldState_= (redis.getWorldState(block.id))
    //txState_=(redis.getTxState(block.id))
    log.info("[" +  client.id + "]" +  " [blockComputation] NEW WORLD STATE : " + worldState.keySet.size)
    //TODO:Send a PropogateBlock Message to NetworkMgrActor
    accountingActor ! BlockPropogated
    nodeActor ! PropogateToNeighbours(block)
    // Send the transactions that originate here to other nodes
    for (tx <- TxOrignatingHere) {nodeActor ! PropogateToNeighboursTx(tx)}
    log.info("[" +  client.id + "]" +  "%%%%% BLOCK COMPUTATION ENDED %%%%% :" + block.transactionList.length.toString)

    return true
  }


  def blockCreate(txList : mutable.ListBuffer[Transaction]): Boolean = {
    //Update parent Block
    log.info( "[" +  client.id + "]"  + "===========MINING STARTED===========")
    var bId = DepthSet.getLeafBlock()
    log.info("[" +  client.id + "]" + "Depth of parent block : " + bId.depth)
    var pBlk = redis.getBlock(bId.blockId)
    pBlk match {
      case Some(pBlk) => {
          parentBlock_= (pBlk); worldState_= (redis.getWorldState(pBlk.id))
          txState_=(redis.getTxState(pBlk.id))
          log.info("[" +  client.id + "]" +  " [Mining] Fetched world state from parent")
          log.info("[" +  client.id + "]" +  " [Mining] WORLD STATE : " + worldState.keySet.size)
        }
      case None => {}
    }

    //Remove all the transactions that have been executed in the current blockchain subtree
    log.info("[" +  client.id + "]" +  " [Mining] Removing all the transactions that have been executed in the current blockchain subtree")
    log.info("[" +  client.id + "]" +  " [Mining] Length of Tx List Before:" + txList.length)
    for (tx <- txList) {
      if(txState.contains(tx)) { txList.remove(txList.indexOf(tx))}
    }
    log.info("[" +  client.id + "]" +  " [Mining] Length of Tx List After:" + txList.length)

    val block: Block = new Block(_transactionList = txList)

    block.parentHash = parentBlock.id
    block.gasLimit = calculateBlockGasLimit(parentBlock.gasLimit)
    block.beneficiary = client.account.address
    block.gasUsed = getGasUsed()
    block.number = getBlockNumber(parentBlock.number)
    block.difficulty = calculateDifficulty(block.number, parentBlock.difficulty, block.timestamp, parentBlock.timestamp)
    log.info("[" +  client.id + "]" +  "[" +  block.id + "]" + " [Mining] PARENT DIFFICULTY :  " + parentBlock.difficulty)
    log.info("[" +  client.id + "]" +  "[" +  block.id  + "]" + " [Mining] PARENT GASLIMIT :  " + parentBlock.gasLimit)
    log.info("[" +  client.id + "]" +  "[" +  block.id  + "]" + " [Mining] PARENT GASUSED :  " + parentBlock.gasUsed)
    log.info("[" +  client.id + "]" +  "[" +  block.id  + "]" + " [Mining] CHILD DIFFICULTY :  " + block.difficulty)
    log.info("[" +  client.id + "]" +  "[" +  block.id  + "]" + " [Mining] CHILD GASLIMIT :  " + block.gasLimit)
    log.info("[" +  client.id + "]" +  "[" +  block.id  + "]" + " [Mining] CHILD GASUSED :  " + block.gasUsed)
    block.transactionList = txList
    log.info("[" +  client.id + "]" +  " [Mining] Updated Block fields while Mining")
    log.info("[" +  client.id + "]" +  " [Mining] BLOCK " + block.toString())
    log.info("[" +  client.id + "]" +  " [Mining] Starting Block computation while mining BLOCK ID: " + block.id)
    val result = blockComputation(block)
    if(result) {log.info("[" +  client.id + "]" +  " [Mining] SUCCESSFULLY MINED " + block.id)}
    else {log.info("[" +  client.id + "]" +  " [Mining] FAILURE WHILE MINING " + block.id)}
    log.info( "[" +  client.id + "]"  + "===========MINING ENDED===========")
    result
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
    log.info("[" +  client.id + "]" +  " [getOrElseAccount] GET ACCOUNT: " + accAddr)
    val ac = accountsAffected.getOrElse(accAddr, None)
    if(ac == None) {
      log.info("[" +  client.id + "]" +  " [getOrElseAccount] ACCOUNT: " + accAddr + "ABSENT in local context.")
      //Not in local context. Get it from the WorldState
      val acWS = worldState.get(accAddr)
      if (acWS == None) {
        log.info("[" +  client.id + "]" +  " [getOrElseAccount] ACCOUNT: " + accAddr + "ABSENT in WORLD STATE.")
        return default}
      else {
        log.info("[" +  client.id + "]" +  " [getOrElseAccount] ACCOUNT: " + accAddr + "FOUND in WORLD STATE.")
        return acWS.get}
    }
    else {
      log.info("[" +  client.id + "]" +  " [getOrElseAccount] ACCOUNT: " + accAddr + "FOUND in local context.")
      return ac}
  }



  def executeTx(tx: Transaction, blockGasLimit: Float, blockId: String): (Boolean, Int) ={
    /*
    val sender = getLocalAccount(tx.sender).getOrElse(default = return )
    if (!sender.isInstanceOf[Account]) {log.info("Sender Or receiver are new. Skipping tx"); return}
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
    log.info("[" +  client.id + "]" + "TX [" +  tx.id + "]"  +  " [executeTx] %%%%% TRANSACTION EXEC STARTED %%%%%")
    log.info("[" +  client.id + "]" + "TX [" +  tx.id + "]"  +" [executeTx] Fetch sender and receiver. STARTED")
    val senderAny = getOrElseAccount(tx.sender, blockId, None)
    val receiverAny = getOrElseAccount(tx.receiver, blockId, None)
    val minerAny = getOrElseAccount(client.account.address, blockId, None)
    var nonce = -1

    log.info("[" +  client.id + "]" + "TX [" +  tx.id + "]"  + " [executeTx] Fetch sender and receiver. ENDED")
    if(senderAny.isInstanceOf[Account] & receiverAny.isInstanceOf[Account]) {
      val sender = senderAny.asInstanceOf[Account]
      val receiver = receiverAny.asInstanceOf[Account]
      val miner = minerAny.asInstanceOf[Account]

      // Update the return value of nonce
      nonce = sender.nonce

      log.info("[" +  client.id + "]" + "[" +  blockId + "]" + "TX [" +  tx.id + "]"  + " [executeTx] valid sender and receiver")
      log.info("[" +  client.id + "]" + "[" +  blockId + "]" + "TX [" +  tx.id + "]"  + " [executeTx] SENDER ADDRESS :"
        + sender.address + "BALANCE :" + sender.balance)
      log.info("[" +  client.id + "]" + "[" +  blockId + "]" + "TX [" +  tx.id + "]"  + " [executeTx] RECEIVER ADDRESS :"
        + receiver.address + "BALANCE" + receiver.balance)

      if (isValidTx(sender, tx, blockGasLimit)) {
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  + " [executeTx] valid Transaction")
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP1] Reduce TgTp from sender balance. Tg:" + tx.gasLimit.toString +
        " Tp :" + tx.gasPrice.toString )
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  + " [executeTx][STEP1-S] sender balance before -TgTp: " + sender.balance)
        sender.balance -= tx.gasLimit * tx.gasPrice
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  + " [executeTx][STEP1-E] sender balance after -TgTp: " + sender.balance)
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  + " [executeTx][STEP2-S] sender nonce before Increment: " + sender.nonce)
        sender.nonce += 1
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  + " [executeTx][STEP1-E] sender nonce after Increment: " + sender.nonce)
        val gasRemaining = tx.gasLimit - getGasUsed()
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP3] Remaining GAS : " + gasRemaining.toString)
        //Here call EVM secondary for contracts
        // Send a request to estimate gas reqd(g') if contracts

        //For transfer transactions gasRemaining is same (g == g')
        // Ar = 0 in our case... No SSTORE at this moment
        var gasRefund = gasRemaining
        val gasRefund_1 = ((tx.gasLimit - gasRemaining) / 2).floor
        if (gasRefund_1 < 0) gasRefund += gasRefund_1 else gasRefund += 0
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP6] Refund Amount : " + gasRefund.toString)
        sender.balance += (gasRefund * tx.gasPrice)
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP7] Refund Amount to sender. Sender balance Now: " + sender.balance.toString)
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP8-S] benificiary prize updated. Acc- Bal Before: " + miner.balance.toString)
        miner.balance += (tx.gasLimit - gasRefund) * tx.gasPrice
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP8-E] benificiary prize updated. Acc- Bal After: " + miner.balance.toString)

        //Transaction value transfer
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP9] Transaction Transfer")
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP9-S] SENDER: " + sender.balance.toString + "  | RECEIVER :" +receiver.balance.toString)
        sender.balance -= tx.value
        receiver.balance += tx.value
        log.info("[" +  client.id + "]" +  "TX [" +  tx.id + "]"  +" [executeTx][STEP9-S] SENDER: " + sender.balance.toString + "  | RECEIVER :" +receiver.balance.toString)

        accountsAffected.put(sender.address, sender)
        accountsAffected.put(receiver.address, receiver)
        accountsAffected.put(miner.address, miner)

        val accAffected = new HashMap[String, Float]
        accAffected.put(sender.address, sender.balance)
        accAffected.put(receiver.address, receiver.balance)
        accAffected.put(miner.address, miner.balance)
        txListLocalContext.put(tx.id, accAffected)
        log.info("[" +  client.id + "]" +  " [executeTx] TRANSACTION EXEC SUCCESSFULLY. TX:" + tx.id)
        log.info("[" +  client.id + "]" +  " [executeTx] ===== %%%%% TRANSACTION EXEC ENDED %%%%%======")
        return (true, nonce)
      }
      else {
        log.info("[" +  client.id + "]" +  " [executeTx] invalid Transaction : " + tx.id + "added back to the txpool")
        //Send the transaction to the tx pool for executing it later, when the conditions are met
        context.sender ! AddTxToPool(tx)
        log.info("[" +  client.id + "]" +  " [executeTx] @@@@@ TX NOT EXECUTED. REASON : INVALID TX: " + tx.id)
        log.info("[" +  client.id + "]" +  " [executeTx] ===== %%%%% TRANSACTION EXEC ENDED %%%%%======")
        return (false, nonce)
      }
    }
    log.info("[" +  client.id + "]" +  " [executeTx] WORLD STATE COUNT:" + worldState.keySet.size.toString)
    log.info("[" +  client.id + "]" +  " [executeTx] @@@@@ TX NOT EXECUTED. REASON : SENDER or RECEIVER not present in WORLD STATE. TX: " + tx.id)
    log.info("[" +  client.id + "]" +  " [executeTx] ===== %%%%% TRANSACTION EXEC ENDED %%%%%======")
    return (false, nonce)
  }

  def isValidTx(sender: Account, tx: Transaction, blockGasLimit: Float): Boolean = {
    val g0 = getGasUsed()
    val v0 = calculateUpfrontCost(tx) //Upfront cost

    //Handling the case for new transactions
    if (tx.nonce == 0) {
      tx.nonce_= (sender.nonce)
    }
    /*
    if ((sender.nonce != 0) && (tx.nonce == sender.nonce) && (g0 <= tx.gasLimit)
      && (v0 <= sender.balance) && (tx.gasLimit <= (blockGasLimit - g0))) return true else return false
    */
    if ((sender.nonce != 0)) {
      if(tx.nonce == sender.nonce) {
        if(g0 <= tx.gasLimit) {
          if(v0 <= sender.balance) {
            if(tx.gasLimit <= (blockGasLimit - g0)) {
              log.info("[" +  client.id + "]" +  " [isValidTx][SUCCESS]")
              return true
            } else {log.info("[" +  client.id + "]" +  " [isValidTx][FAILURE-5]")}
          } else {log.info("[" +  client.id + "]" +  " [isValidTx][FAILURE-4]")}
        } else {log.info("[" +  client.id + "]" +  " [isValidTx][FAILURE-3]")}
      } else {log.info("[" +  client.id + "]" +  " [isValidTx][FAILURE-2]" + sender.nonce.toString + " , " + tx.nonce.toString )}
    } else {log.info("[" +  client.id + "]" +  " [isValidTx][FAILURE-1]")}
    return false
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
