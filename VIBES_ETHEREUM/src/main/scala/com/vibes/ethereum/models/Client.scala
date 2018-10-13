package com.vibes.ethereum.models

import com.vibes.ethereum.helpers.GuidExtension

@SerialVersionUID(123L)
class Client(
    private var _clientType: String,
    private var _lat: String,
    private var _lon: String) extends GuidExtension with Serializable {

  override def toString: String = {
    f"Client ID: $id at location ($lat, $lon)"
  }

  private val _id: String = generateGUID()
  private val _account = new Account(_lat = lat,_lon= lon, _creatorId = this.id )

  //Getter
  def id = _id
  def clientType = _clientType
  def lat = _lat
  def lon = _lon
  def account :Account = _account

  //Setter
  def clientType_= (value:String) = _clientType = value
  def lat_= (value: String) = _lat = value
  def lon_= (value: String)= _lon = value
}
