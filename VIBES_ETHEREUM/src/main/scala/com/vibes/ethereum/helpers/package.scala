package com.vibes.ethereum

package object helpers {
  object EvmState extends Enumeration {
    type EvmState = Value
    val STARTING, INITIALIZING, IDLE, MINING, VERFYING, STOPPED = Value
  }

  object NodeState extends Enumeration {
    type NodeState = Value
    val STARTING, INITIALIZING, ACCEPTING_CONN, DISCOVER, STOPPED = Value
  }

  object TxPoolState extends Enumeration {
    type TxPoolState = Value
    val STARTING, INITIALIZING, IDLE, ACCEPTING_TX, REJECTING_TX, STOPPED = Value
  }
}
