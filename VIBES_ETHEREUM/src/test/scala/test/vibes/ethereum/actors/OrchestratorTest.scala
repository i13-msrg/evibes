package test.vibes.ethereum.actors

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import com.vibes.ethereum.actors.Orchestrator.StartSimulation
import org.scalatest.{BeforeAndAfterAll, MustMatchers, Suite, WordSpecLike}

trait StopSystemAfterAll extends BeforeAndAfterAll{
  this: TestKit with Suite =>
  override protected def afterAll() {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }
}

class OrchestratorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {
    "A Orchestrator Actor" must {
      "start a simulation when SimulationStart msg is received" in {
        import com.vibes.ethereum.actors.Orchestrator
        val orchestrator = TestActorRef[Orchestrator]
        //orchestrator ! StartSimulation("Woopie!!")

        //orchestrator.underlyingActor.state must (contain("Woopie!!"))
      }
      "" in {

      }
    }
}
