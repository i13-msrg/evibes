package com.vibes.ethereum.models

import scala.collection.mutable
import scala.util.Random

class GHOST_DepthSet() {
  var depthSet = new mutable.HashSet[LightBlock]()
  var depth = 0
  var nodeMap = new mutable.HashMap[String, LightBlock]()

  def addLightBlock(lb: LightBlock) = {
    nodeMap.put(lb.blockId, lb)
    if (lb.depth > depth) {
      depthSet.clear()
      depthSet.add(lb)
      depth = lb.depth
    } else if (lb.depth == depth) {
      depthSet.add(lb)
    }
  }

  def getLeafBlock() : LightBlock = {
    if (depthSet.size == 1) {
      val blk = depthSet.toList(0)
      nodeMap.getOrElse(blk.blockId, new LightBlock("0x0", "0x0", 0))
    }
    else if(depthSet.size == 0) {
      return new LightBlock("0x0", "0x0", 0)
    }
    else {
      val blk = Random.shuffle(depthSet.toList).take(1)(0)
      nodeMap.getOrElse(blk.blockId, new LightBlock("0x0", "0x0", 0))
    }
  }

  def getDepthOfBlock(id: String) = {
    val node = nodeMap.getOrElse(id, new LightBlock("0x0", "0x0", -1))
    node.depth
  }
}

