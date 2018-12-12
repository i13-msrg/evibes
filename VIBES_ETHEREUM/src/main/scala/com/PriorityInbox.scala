package com

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.typesafe.config.Config
import com.vibes.ethereum.actors.ethnode.EvmPrimary.{CreateAccountEVM, GetGhostDepth, UpdateGhostDepth}
import com.vibes.ethereum.models.{Account, GHOST_DepthSet}


class PriorityInbox(settings: ActorSystem.Settings, config: Config) extends UnboundedStablePriorityMailbox(
  // Create a new PriorityGenerator, lower prio means more important
  PriorityGenerator {
    case GetGhostDepth() => 0
    case UpdateGhostDepth(depth: GHOST_DepthSet) => 0
    case CreateAccountEVM(account: Account) => 1
    case _ => 2
})