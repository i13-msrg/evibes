
package com.vibes.ethereum.service

import com.vibes.ethereum.models.{Account, Block, Transaction, Client}
import com.redis._
import java.io._
import java.util.Base64
import java.nio.charset.StandardCharsets.UTF_8


class RedisManager(
   private val clientID: String) {

  private val redis: RedisClient = new RedisClient("localhost", 6379)

  def getClient(id: String) : Client ={
    deserialise(redis.get("CLIENT-" + id).map(_.toString).getOrElse("")).asInstanceOf[Client]
  }

  def putClient(client:Client) : String ={
      val value = serialise(client)
      val key = "CLIENT-" + clientID
      redis.set(key, value)
      key
  }

  def getTx(txId: String) : Transaction = {
    deserialise(redis.get(clientID + "-TX-" + txId).toString).asInstanceOf[Transaction]
  }

  // Only transactions that are a part of a block are added in the database
  def putTx(tx: Transaction, blockID: String) : String = {
    val value = serialise(tx)
    val key = clientID + "-TX-" + tx.id + "-" + blockID
    redis.set(key, value)
    key
  }

  def getAccount(accAddr: String) : Account = {
    deserialise(redis.get(clientID + "-ACC-" + accAddr).toString).asInstanceOf[Account]
  }

  def putAccount(account: Account) : String = {
    val key = clientID + "-ACC-" + account.address.toString
    redis.set(key, serialise(account))
    key
  }

  def putTxEntry(txID: String, accAddr: String, balance: Float): String = {
    val key = clientID + "-TX-ENTRY-" + accAddr
    redis.lpush(key, txID + ":" + balance.toString)
    key
  }

  def getBlock(blockId: String) : Block = {
    deserialise(redis.get(blockId).toString).asInstanceOf[Block]
  }

  def putBlock(block: Block) : String = {
    val value = serialise(block)
    val key = clientID + "-BLOCK-" + block.number.toString
    redis.set(key, value)
    key
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
