package com.vibes.ethereum.models

import com.vibes.ethereum.helpers.GuidExtension

@SerialVersionUID(123L)
class Account(
  private var _nonce: Int = 1,
  private var _balance: Float = 2200000,
  private var _address: String = "temp",
  private var _lat: String = "test",
  private var _lon: String = "test",
  private var _storageRoot: String  = "test",
  private var _creatorId: String) extends GuidExtension with Serializable {

  override def toString: String = {
    f"""Address: $address at location ($lat, $lon)"""
  }

  _address = generateGUID()

  //Getter
  def nonce = _nonce
  def balance = _balance
  def address =_address
  def lat = _lat
  def lon = _lon
  def storage_root= _storageRoot
  def creatorId=_creatorId

  //Setters
  def nonce_= (value : Int) = _nonce = value
  def balance_= (value : Float) = _balance = value
  def address_=(value : String) = _address = value
  def lat_= (value : String)=_lat= value
  def lon_= (value : String)=_lon= value
  def storage_root_= (value : String)=_storageRoot= value
  def creatorId_=(value : String)=_creatorId = value
}