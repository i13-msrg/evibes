package com.vibes.ethereum.models


class LightBlock(_blockId: String, _parentBlockId: String, _depth: Int) {
  //Getter
  def blockId = _blockId
  def parentBlockId = _parentBlockId
  def depth = _depth

}
