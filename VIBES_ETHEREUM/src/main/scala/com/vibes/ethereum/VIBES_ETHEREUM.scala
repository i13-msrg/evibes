package com.vibes.ethereum


import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
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
import com.vibes.ethereum.actors.{LocalStatsJson, Orchestrator, StatsJson}
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.headers.{HttpOrigin, HttpOriginRange}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.vibes.ethereum.models.{Account, Block, Stats, Transaction}
import spray.json.DefaultJsonProtocol
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.io.StdIn
import spray.json._

object Setting {
  var bootNodes: Int = 3
  var nodesNum: Int = 3
  var txNum: Int = 20000
  var accountsNum: Int = 10
  var bootAccNum: Int = 10
  var txBatch: Int = 20
  var blockGasLimit: Float = 10
  var poolSize: Int = 100
  var minConn: Int = 2
  var maxConn: Int = 5
  var GenesisBlock: Block = new Block(_transactionList = new ListBuffer[Transaction])
  GenesisBlock.parentHash_=("0x0000000000000000000000000000000000000000000000000000000000000000")
  GenesisBlock.ommersHash_= ("0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347")
  GenesisBlock.beneficiary_= ("0x0000000000000000000000000000000000000000")
  GenesisBlock.difficulty_= (17179869184L)
  GenesisBlock.gasLimit_= (63000)
  GenesisBlock.gasUsed_= (0)
  GenesisBlock.number_= (0)
}


case class InputJson(bootNodes:Int, nodesNum:Int, txNum:Int, accountsNum:Int, bootAccNum:Int,
                           txBatch:Int, blockGasLimit: Float, poolSize:Int, minConn:Int, maxConn:Int,
                           difficulty: Long, gasLimit: Float, gasUsed: Float)

object CustomJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val eventJsonFormat = jsonFormat3(EventJson)
}

object StatsJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val statsJsonFormat = jsonFormat11(StatsJson)
}

object LocalStatsJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val localStatsJsonFormat = jsonFormat22(LocalStatsJson)
}

trait InputJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val InputJsonFormat = jsonFormat13(InputJson)
}


object VIBES_ETHEREUM extends App with DefaultJsonProtocol with InputJsonProtocol {
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

    val route: Route = {
      import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
      val orchestrator = system.actorOf(Props(new Orchestrator(evmQueue, localStatsQueue, globalStatsQueue)), "orchestrator")
      // CORS settings [hardcoded;-)]

      val corsSettings = CorsSettings.defaultSettings.copy(
        allowedOrigins = HttpOriginRange(HttpOrigin("http://localhost:8080"))
      )

      // Rejection handler
      val rejectionHandler = corsRejectionHandler withFallback RejectionHandler.default

      // Exception handler
      val exceptionHandler = ExceptionHandler {
        case e: NoSuchElementException => complete(StatusCodes.NotFound -> e.getMessage)
      }

      // Combining the two handlers only for convenience
      val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

      val timeoutResponse = HttpResponse(StatusCodes.EnhanceYourCalm,
        entity = "Unable to serve response within time limit, please enchance your calm.")

      // Note how rejections and exceptions are handled *before* the CORS directive (in the inner route).
      // This is required to have the correct CORS headers in the response even when an error occurs.
      handleErrors {
        cors(corsSettings) {
          handleErrors {
            get {
              pathEndOrSingleSlash {
                getFromResource("build/index.html")
              } ~ {
                getFromResourceDirectory("build")
              }
            }~
            path("input") {
                post {
                  entity(as[InputJson]) { inp =>
                    Setting.bootNodes = inp.bootNodes
                    Setting.nodesNum = inp.nodesNum
                    Setting.txNum = inp.txNum
                    Setting.accountsNum = inp.accountsNum
                    Setting.bootAccNum = inp.bootAccNum
                    Setting.txBatch = inp.txBatch
                    Setting.blockGasLimit = inp.blockGasLimit
                    Setting.poolSize = inp.poolSize
                    Setting.minConn = inp.minConn
                    Setting.maxConn = inp.maxConn
                    Setting.GenesisBlock.difficulty_=(inp.difficulty)
                    Setting.GenesisBlock.gasLimit_=(inp.gasLimit)
                    Setting.GenesisBlock.gasUsed_=(inp.gasUsed)

                    orchestrator ! StartSimulation(Setting)
                    complete(HttpEntity(ContentTypes.`application/json`, "{\"text\": \"Success\"}"))
                    complete(inp.toString)
                  }
                }
            }~
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
              }~
              path(pm="stop") {
                get {
                  orchestrator ! PoisonPill
                  complete(HttpEntity(ContentTypes.`application/json`, "{\"text\": \"Success\"}"))
                }
              }
          }
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
}
