package com.vibes.ethereum.models

import com.vibes.ethereum.helpers.GuidExtension
import scala.collection.mutable


@SerialVersionUID(123L)
class Block(
  private var _parentHash: String = "None",
  private var _ommersHash: String= "None",
  private var _beneficiary: String= "None",
  private var _stateRoot: String= "None",
  private var _transactionRoot: String= "None",
  private var _receiptsRoot: String= "None",
  private var _difficulty: Long = 0,
  private var _number: Long = 0,
  private var _gasLimit: Float = 0,
  private var _gasUsed: Float = 0,
  private var _transactionList: mutable.ListBuffer[Transaction],
  private var _ttl: Int=10,
  private var _timestamp: Long= 0) extends GuidExtension with Serializable {

  override def toString: String = {
    f"Block with parent : $parentHash. Number $number. Timestamp: $timestamp"
  }

  private val _id = generateGUID()

  //Getter
  def id = _id
  def parentHash = _parentHash
  def ommersHash = _ommersHash
  def beneficiary = _beneficiary
  def stateRoot = _stateRoot
  def transactionRoot = _transactionRoot
  def receiptsRoot = _receiptsRoot
  def difficulty = _difficulty
  def number = _number
  def gasLimit = _gasLimit
  def gasUsed = _gasUsed
  def transactionList= _transactionList
  def timestamp = _timestamp
  def ttl = _ttl


  //Setter
  def parentHash_= (value:String) = _parentHash = value
  def ommersHash_=(value:String) = _ommersHash= value
  def beneficiary_=(value:String) = _beneficiary= value
  def stateRoot_=(value:String) = _stateRoot= value
  def transactionRoot_=(value:String) = _transactionRoot= value
  def receiptsRoot_=(value:String) = _receiptsRoot= value
  def difficulty_=(value:Long) = _difficulty= value
  def number_= (value:Long)= _number= value
  def gasLimit_=(value:Float) = _gasLimit= value
  def gasUsed_=(value:Float) = _gasUsed= value
  def transactionList_=(value: mutable.ListBuffer[Transaction]) = _transactionList
  def timestamp_=(value:Long) = _timestamp= value
  def ttl_= (value: Int) = _ttl = value
}
