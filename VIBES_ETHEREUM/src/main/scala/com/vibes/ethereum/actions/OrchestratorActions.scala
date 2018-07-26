package com.vibes.ethereum.actions

object OrchestratorActions {
  sealed trait Action

  case class StartSimulation(data:String) extends Action
}
