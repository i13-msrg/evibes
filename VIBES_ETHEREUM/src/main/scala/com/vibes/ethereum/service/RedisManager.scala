
package com.vibes.ethereum.service

import com.vibes.ethereum.models.{Account, Block, Client, Transaction}
import com.redis._
import java.io._
import java.util.Base64
import java.nio.charset.StandardCharsets.UTF_8

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try




class RedisManager(
   private val clientID: String) {

  private val redis: RedisClient = new RedisClient("localhost", 6379)
  def getClient(id: String) : Option[Client] ={
    val client = redis.get("CLIENT-" + id)
    client match {
      case None => {return None}
      case Some(clt) => {return Try(deserialise(clt.toString).asInstanceOf[Client]).toOption}
    }
  }

  def putClient(client:Client) : Option[String] ={
      val value = serialise(client)
      val key = "CLIENT-" + clientID
      if (redis.set(key, value)) {return Try(key).toOption}
      else {return None}
  }
/*
  def getTx(txId: String) : Option[Transaction] = {
    val tx = redis.get(clientID + "-TX-" + txId)

    if(tx == None) {return None}
    else {return Try(deserialise(tx.toString).asInstanceOf[Transaction]).toOption}
  }

  // Only transactions that are a part of a block are added in the database
  def putTx(tx: Transaction, blockID: String) : Option[String] = {
    val value = serialise(tx)
    val key = clientID + "-TX-" + tx.id + "-" + blockID
    if (redis.set(key, value)) {return Try(key).toOption}
    else {return None}
  }
*/


  //UPDATED
  def getTx(txId: String, blockId: String) : Option[Transaction] = {
    val tx = redis.get(clientID + "-" + blockId + "-TX-" + txId)
    tx match {
      case None => {return None}
      case Some(txObj) => {return Try(deserialise(txObj.toString).asInstanceOf[Transaction]).toOption}
    }
  }


  // Only transactions that are a part of a block are added in the database
  //UPDATED
  def putTx(tx: Transaction, blockID: String) : Option[String] = {
    val value = serialise(tx)
    val key = clientID + "-" + blockID + "-TX-" + tx.id
    if (redis.set(key, value)) {
      // Add Tx ID to the TXSTATE for the block
      redis.lpush(clientID + "-TXSTATE-" + blockID, tx.id)
      return Try(key).toOption
    }
    else {return None}
  }

  // Copies all the tx state from the parent node. This way the child has an idea of all the tx executed before on the blockchain branch
  def createTxState(parentId: String, childId: String) = {
    val key = clientID + "-TXSTATE-" + parentId
    val childKey = clientID + "-TXSTATE-" + childId
    // Copy parent state to child state
    val txList = redis.lrange(key, 0, -1).get
    for(txKey <- txList) {
      redis.lpush(childKey, txKey)
    }
  }

  def getTxState(blockId: String): ListBuffer[String] = {
    val key = clientID + "-TXSTATE-" + blockId
    val txIdList = redis.lrange(key, 0, -1).get
    var txList = new ListBuffer[String]
    for (txId <- txIdList) {
      if (txId != None) {txList += txId.get}
    }
    txList
  }

  def getTxState(blockId: String, clientId: String): ListBuffer[String] = {
    val key = clientId + "-TXSTATE-" + blockId
    val txIdList = redis.lrange(key, 0, -1).get
    var txList = new ListBuffer[String]
    for (txId <- txIdList) {
      if (txId != None) {txList += txId.get}
    }
    txList
  }

  def putTxState(blockId: String, state: ListBuffer[String]) ={
    val key = clientID + "-TXSTATE-" + blockId
    for(txId <- state) {
      redis.lpush(key, txId)
    }
  }

  // UPDATED
  def getAccount(accAddr: String, blockId: String) : Option[Account] = {
    val acc = redis.get(clientID + "-" + blockId + "-ACC-" + accAddr)
    acc match {
      case None => {return None}
      case Some(account) => {return Try(deserialise(account.toString).asInstanceOf[Account]).toOption}
    }
  }

  // UPDATED
  def putAccount(account: Account, blockId: String, updateState: Boolean) : Option[String] = {
    val key = clientID + "-" + blockId + "-ACC-" + account.address.toString
    // Add the account to the current blocks worldState
    if (redis.set(key, serialise(account))) {
      if(updateState) {redis.lpush(clientID + "-WORLD-" + blockId, key)}
      return Try(key).toOption
    }
    else {return None}
  }

  //All the affected accounts have to undergo an Update
  def updateWorldState(account: Account, parentBlockId: String, childBlockId: String): Option[String] = {
    val key = clientID + "-WORLD-" + childBlockId
    val prevStateKey = clientID + "-" + parentBlockId + "-ACC-" + account.address
    val rem = redis.lrem(key,1, prevStateKey)
    if(rem.get == 1) {
      val accKey = clientID + "-" + childBlockId + "-ACC-" + account.address
      redis.lpush(key, accKey)
    }
    return Try(key).toOption
  }


  def getWorldState(blockId: String): mutable.HashMap[String, Account] = {
    val key = clientID + "-WORLD-" + blockId
    val accKeyList = redis.lrange(key, 0, -1).get
    var accMap = new mutable.HashMap[String, Account]
    for (acKey <- accKeyList) {
      val acc = redis.get(acKey.get)
      acc match {
        case Some(acc) => {
          val account = deserialise(acc).asInstanceOf[Account]
          accMap.put(account.address, account)
        }
        case None => {}
      }
    }
    accMap
  }

  // Deviation from the standard Redis client implementation for simplification
  def getWorldState(blockId: String, clientId: String): mutable.HashMap[String, Account] = {
    val key = clientId + "-WORLD-" + blockId
    val accKeyList= redis.lrange(key, 0, -1).get
    var accMap = new mutable.HashMap[String, Account]
    for (acKey <- accKeyList) {
      val account = deserialise(redis.get(acKey.get).get).asInstanceOf[Account]
      accMap.put(account.address, account)
    }
    accMap
  }


  // Merge world state from 2 blocks. Useful for merging states of the parent into the child block
  def createWorldState(parentId: String, childId: String) = {
    val key = clientID + "-WORLD-" + parentId
    val childKey = clientID + "-WORLD-" + childId
    val accKeyList = redis.lrange(key, 0, -1).get
    for(accountKey <- accKeyList) {
      redis.lpush(childKey, accountKey.get)
    }
  }

  /*
  def createInitialWorldState(blockId: String, accounts: ListBuffer[String]) ={
    val key = clientID + "-WORLD-" + blockId
    for(txId <- accounts) {
      redis.lpush(key, txId)
    }
  }
  */

  def putTxEntry(txID: String, accAddr: String, balance: Float): Option[String] = {
    val key = clientID + "-TX-ENTRY-" + accAddr
    redis.lpush(key, txID + ":" + balance.toString)
    return Try(key).toOption
  }

  def getBlock(blockId: String) : Option[Block] = {
    val key = clientID + "-BLOCK-" + blockId
    val block = redis.get(key)
    block match {
      case None => {return None}
      case Some(x) => {return Try(deserialise(x.toString).asInstanceOf[Block]).toOption}
    }
  }

  def getBlockByKey(key: String): Option[Block] = {
    val block = redis.get(key).get
    if (block == None) {return None}
    else {return Try(deserialise(block.toString).asInstanceOf[Block]).toOption}
  }

  def getAllBlocks(): ListBuffer[Block] = {
    val key = clientID + "-BLOCK-*"
    var cursor = 0
    var result = new ListBuffer[Block]
    var blockList = new mutable.ListBuffer[String]
    do {
      var result: Option[(Option[Int], Option[List[Option[String]]])] = redis.scan(cursor, key,10)
      var list = result.get._2.get
      if (list.length > 0) {
        for (key <- list) {
          blockList += key.get
        }
      }
      cursor = result.get._1.get
    } while(cursor != 0)

    for (bkey <- blockList) {
      var blk = getBlockByKey(bkey)
      blk match {
        case Some(blk) => {result += blk}
        case None => {}
      }
    }
    result
  }

  def putBlock(block: Block) : Option[String] = {
    val key = clientID + "-BLOCK-" + block.id
    if(redis.set(key, serialise(block))) {return None}
    else {return Try(key).toOption}
  }

  // This part of the code is taken from : https://gist.github.com/laughedelic/634f1a1e5333d58085603fcff317f6b4
  def serialise(value: Any): String = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(value)
    oos close()
    new String(
      Base64.getEncoder.encode(stream.toByteArray),
      UTF_8
    )
  }

  // This part of the code is taken from : https://gist.github.com/laughedelic/634f1a1e5333d58085603fcff317f6b4
  def deserialise(str: String): Any = {
    val bytes = Base64.getDecoder.decode(str.getBytes(UTF_8))
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val value = ois.readObject
    ois.close()
    value
  }

  // This part is taken from https://gist.github.com/navicore/6234040bbfce3aa58f866db314c07c15
  def sha256Hash(text: String) : String = String.format("%064x", new java.math.BigInteger(1, java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"))))

}
