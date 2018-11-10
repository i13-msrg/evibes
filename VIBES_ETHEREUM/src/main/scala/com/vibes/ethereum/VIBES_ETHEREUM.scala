package com.vibes.ethereum



import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BroadcastHub, Keep, Source, SourceQueueWithComplete}
import akka.util.Timeout
import com.vibes.ethereum.actors.Orchestrator._
import com.vibes.ethereum.actors.ethnode.{AccountingActor, EventJson}
import com.vibes.ethereum.actors.{Orchestrator, StatsJson, LocalStatsJson}
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import com.vibes.ethereum.models.{Account, Block, Stats, Transaction}
import spray.json.DefaultJsonProtocol

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.io.StdIn

object Setting {
  val bootNodes: Int = 2
  val nodesNum: Int = 1
  val txNum: Int = 20
  val accountsNum: Int = 10
  val bootAccNum: Int = 10
  val txBatch: Int = 4
  val blockGasLimit: Float = 10
  val poolSize: Int = 100
  val minConn: Int = 1
  val maxConn: Int = 3
  val GenesisBlock: Block = new Block(_transactionList = new ListBuffer[Transaction])
  GenesisBlock.parentHash_=("0x0000000000000000000000000000000000000000000000000000000000000000")
  GenesisBlock.ommersHash_= ("0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347")
  GenesisBlock.beneficiary_= ("0x0000000000000000000000000000000000000000")
  GenesisBlock.difficulty_= (17179869184L)
  GenesisBlock.gasLimit_= (5000)
  GenesisBlock.gasUsed_= (0)
  GenesisBlock.number_= (0)
}



object CustomJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val eventJsonFormat = jsonFormat3(EventJson)
}

object StatsJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val statsJsonFormat = jsonFormat11(StatsJson)
}

object LocalStatsJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val localStatsJsonFormat = jsonFormat22(LocalStatsJson)
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
    import StatsJsonProtocol._
    import LocalStatsJsonProtocol._

    // Create a SSE events stream for EVM state update events
    val (evmQueue, sseSource): (SourceQueueWithComplete[EventJson], Source[ServerSentEvent, NotUsed])
    = Source.queue[EventJson](100, akka.stream.OverflowStrategy.backpressure)
      .map(obj => obj.toJson)
      .map(obj => obj.prettyPrint)
      .map(s => ServerSentEvent(s))
      .keepAlive(5.second, () => ServerSentEvent.heartbeat)
      .toMat(BroadcastHub.sink[ServerSentEvent])(Keep.both)
      .run()


    // Create a SSE events stream for Local Stats update events
    val (localStatsQueue, localStatsSseSource): (SourceQueueWithComplete[LocalStatsJson], Source[ServerSentEvent, NotUsed])
    = Source.queue[LocalStatsJson](100, akka.stream.OverflowStrategy.backpressure)
      .map(obj => obj.toJson)
      .map(obj => obj.prettyPrint)
      .map(s => ServerSentEvent(s))
      .keepAlive(5.second, () => ServerSentEvent.heartbeat)
      .toMat(BroadcastHub.sink[ServerSentEvent])(Keep.both)
      .run()


    // Create a SSE events stream forGlobal Stats update events
    val (globalStatsQueue, globalStatsSseSource): (SourceQueueWithComplete[StatsJson], Source[ServerSentEvent, NotUsed])
    = Source.queue[StatsJson](100, akka.stream.OverflowStrategy.backpressure)
      .map(obj => obj.toJson)
      .map(obj => obj.prettyPrint)
      .map(s => ServerSentEvent(s))
      .keepAlive(5.second, () => ServerSentEvent.heartbeat)
      .toMat(BroadcastHub.sink[ServerSentEvent])(Keep.both)
      .run()


    val route = {
      path("state-events") {
        get {
          complete(sseSource)
        }
      }~
      path("local-events") {
        get {
          complete(localStatsSseSource)
        }
      }~
      path("global-events") {
        get {
          complete(globalStatsSseSource)
        }
      }
    }

    val orchestrator = system.actorOf(Props(new Orchestrator(evmQueue, localStatsQueue, globalStatsQueue)), "orchestrator")
    orchestrator ! StartSimulation(Setting)

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
