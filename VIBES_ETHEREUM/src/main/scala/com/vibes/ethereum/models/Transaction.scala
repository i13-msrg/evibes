package com.vibes.ethereum.models

import com.vibes.ethereum.helpers.GuidExtension

@SerialVersionUID(123L)
class Transaction(
  private var _nonce: Int = 0,
  private var _gasPrice: Float = 1,
  private var _gasLimit: Float= 22000,
  private var _receiver: String,
  private var _value: Float = 5,
  private var _ttl: Int=10,
  private var _sender: String) extends GuidExtension with Serializable {

  private val _id: String = generateGUID()
  override def toString: String = {
    f"Transaction ID: $id. Sender : $sender Receiver: $receiver"
  }

  def isValid: Boolean = {
    return false
  }


  //Getter
  def id = _id
  def nonce = _nonce
  def gasPrice = _gasPrice
  def gasLimit = _gasLimit
  def receiver = _receiver
  def value = _value
  def sender = _sender
  def ttl = _ttl


  //Setter
  def nonce_= (value:Int) = _nonce = value
  def gasPrice_= (value:Float) = _gasPrice = value
  def gasLimit_=(value:Float) = _gasLimit = value
  def receiver_= (value:String) = _receiver = value
  def value_= (value:Float) = _value = value
  def sender_= (value:String) = _sender = value
  def ttl_= (value: Int) = _ttl = value

}