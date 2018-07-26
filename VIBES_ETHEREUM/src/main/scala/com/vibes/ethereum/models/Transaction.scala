package com.vibes.ethereum.models

import com.vibes.ethereum.helpers.GuidExtension

class Transaction(
  private var _nonce: Int,
  private var _gasPrice: Float,
  private var _gasLimit: Float,
  private var _receiver: String,
  private var _value: Float,
  private var _sender: String) extends GuidExtension {

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


  //Setter
  def nonce_= (value:Int) = _nonce = value
  def gasPrice_= (value:Float) = _gasPrice = value
  def gasLimit_=(value:Float) = _gasLimit = value
  def receiver_= (value:String) = _receiver = value
  def value_= (value:Float) = _value = value
  def sender_= (value:String) = _sender = value

}