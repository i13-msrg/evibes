package com.vibes.ethereum



import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BroadcastHub, Keep, Source, SourceQueueWithComplete}
import akka.util.Timeout
import com.vibes.ethereum.actors.Orchestrator
import com.vibes.ethereum.actors.Orchestrator._
import com.vibes.ethereum.actors.ethnode.{AccountingActor, EventJson}
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.io.StdIn

object Setting {
  val nodesNum: Int = 10
  val txNum: Int = 100
  val accountsNum: Int = 10
  val txBatch: Int = 10
  val blockGasLimit: Float = 50
  val poolSize: Int = 10
  val minConn: Int = 8
  val maxConn: Int = 25
}

object CustomJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val eventJsonFormat = jsonFormat3(EventJson)
}


object VIBES_ETHEREUM extends DefaultJsonProtocol{
  def main(args: Array[String]) {
    println("Welcome to Ethereum simulation")
    implicit val system = ActorSystem("Ethereum")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher
    implicit val askTimeout = Timeout(5.seconds)
    import spray.json._
    import CustomJsonProtocol._

    // Create a SSE events stream for EVM state update events
    val (evmQueue, evmSseSource): (SourceQueueWithComplete[EventJson], Source[ServerSentEvent, NotUsed])
    = Source.queue[EventJson](100, akka.stream.OverflowStrategy.backpressure)
      .map(obj => obj.toJson)
      .map(obj => obj.prettyPrint)
      .map(s => ServerSentEvent(s))
      .keepAlive(5.second, () => ServerSentEvent.heartbeat)
      .toMat(BroadcastHub.sink[ServerSentEvent])(Keep.both)
      .run()

    val route = {
      path("events") {
        get {
          complete(evmSseSource)
        }
      }
    }

    val orchestrator = system.actorOf(Props(new Orchestrator(evmQueue)), "orchestrator")
    orchestrator ! StartSimulation(Setting)

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
