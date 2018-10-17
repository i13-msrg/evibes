
package com.vibes.ethereum.service

import com.vibes.ethereum.models.{Account, Block, Client, Transaction}
import com.redis._
import java.io._
import java.util.Base64
import java.nio.charset.StandardCharsets.UTF_8

import scala.util.Try


class RedisManager(
   private val clientID: String) {

  private val redis: RedisClient = new RedisClient("localhost", 6379)

  def getClient(id: String) : Option[Client] ={
    val client = redis.get("CLIENT-" + id)
    if(client == None) {return None}
    else {return Try(deserialise(client.toString).asInstanceOf[Client]).toOption}
  }

  def putClient(client:Client) : Option[String] ={
      val value = serialise(client)
      val key = "CLIENT-" + clientID
      if (redis.set(key, value)) {return Try(key).toOption}
      else {return None}
  }

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

  def getAccount(accAddr: String) : Option[Account] = {
    val acc = redis.get(clientID + "-ACC-" + accAddr)
    if(acc == None) {return None}
    else {return Try(deserialise(acc.toString).asInstanceOf[Account]).toOption}
  }

  def putAccount(account: Account) : Option[String] = {
    val key = clientID + "-ACC-" + account.address.toString
    if (redis.set(key, serialise(account))) {return Try(key).toOption}
    else {return None}
  }

  def putTxEntry(txID: String, accAddr: String, balance: Float): Option[String] = {
    val key = clientID + "-TX-ENTRY-" + accAddr
    if (redis.lpush(key, txID + ":" + balance.toString) == None) {return None}
    else {return Try(key).toOption}
  }

  def getBlock(blockId: String) : Option[Block] = {
    val block = redis.get(blockId)
    if (block == None) {return None}
    else {return Try(deserialise(block.toString).asInstanceOf[Block]).toOption}
  }

  def putBlock(block: Block) : Option[String] = {
    val key = clientID + "-BLOCK-" + block.number.toString
    if(redis.set(key, serialise(block)) == None) {return None}
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
