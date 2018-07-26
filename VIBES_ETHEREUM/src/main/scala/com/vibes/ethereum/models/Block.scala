package com.vibes.ethereum.models

@SerialVersionUID(123L)
class Block(
  private var _parentHash: String,
  private var _ommersHash: String,
  private var _beneficiary: String,
  private var _stateRoot: String,
  private var _transactionRoot: String,
  private var _receiptsRoot: String,
  private var _difficulty: String,
  private var _number: Int,
  private var _gasLimit: Float,
  private var _gasUsed: Float,
  private var _transactionList: List[String],
  private var _timestamp: String) extends Serializable {

  override def toString: String = {
    f"Block with parent : $parentHash. Number $number. Timestamp: $timestamp"
  }

  //Getter
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


  //Setter
  def parentHash_= (value:String) = _parentHash = value
  def ommersHash_=(value:String) = _ommersHash= value
  def beneficiary_=(value:String) = _beneficiary= value
  def stateRoot_=(value:String) = _stateRoot= value
  def transactionRoot_=(value:String) = _transactionRoot= value
  def receiptsRoot_=(value:String) = _receiptsRoot= value
  def difficulty_=(value:String) = _difficulty= value
  def number_= (value:Int)= _number= value
  def gasLimit_=(value:Float) = _gasLimit= value
  def gasUsed_=(value:Float) = _gasUsed= value
  def transactionList_=(value: List[String]) = _transactionList
  def timestamp_=(value:String) = _timestamp= value
}
