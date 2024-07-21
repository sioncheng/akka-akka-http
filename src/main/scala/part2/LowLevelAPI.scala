package part2

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Sink
import scala.util.Success
import scala.concurrent.duration._
import scala.util.Failure
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes
import part1.ScalaRecap.future
import scala.concurrent.Future
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.Flow

object LowLevelAPI extends App {
  implicit val system: ActorSystem = 
    ActorSystem("LowLevelAPI")

  import system.dispatcher

  val serverSource = Http().bind("localhost", 8000)  

  val connSink = Sink.foreach[Http.IncomingConnection] {conn =>
    println(s"Accepted incoming connection from ${conn.remoteAddress}")
  }

  val serverBindFuture = serverSource.to(connSink).run()
  serverBindFuture.onComplete {
    case Success(value) => 
      println("Server binding successful.")
      value.terminate(1.seconds)
    case Failure(exception) => 
      println(s"Server binding failed: $exception")
  }
  //

  Thread.sleep(2000)

  val okHttp = HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            <html>
              <head>
                <title>akka http</title>
              </head>
              <body>
                Hello Akka
              </body>
            </html>
          """
        )
      )
  val oopsHttp = HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            <html>
              <head>
                <title>akka http</title>
              </head>
              <body>
                OOPS!
              </body>
            </html>
          """
        )
      )

  val reqHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      okHttp
    case req: HttpRequest =>
      oopsHttp
  }

  val httpSyncConnHandler = Sink.foreach[Http.IncomingConnection] {conn =>
    conn.handleWithSyncHandler(reqHandler)
  }

  val future = Http().bind("localhost", 8080).runWith(httpSyncConnHandler)
  println("sync handler at 8080")


  val asyncReqHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      Future(okHttp)
    case req: HttpRequest =>
      req.discardEntityBytes()
      Future(oopsHttp)
  }
  val httpAsyncConnHandler = Sink.foreach[Http.IncomingConnection] {conn =>
    conn.handleWithAsyncHandler(asyncReqHandler)
  }
  Http().bind("localhost", 8082).runWith(httpAsyncConnHandler)
  println("async handler at 8082")

  val streamsReqHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/streams"), _, _, _) =>
        okHttp
    case req: HttpRequest =>
        req.discardEntityBytes()
        oopsHttp
  }
  Http().bind("localhost", 8084).runForeach{conn =>
    conn.handleWith(streamsReqHandler)
  }

  
  scala.io.StdIn.readLine("press ENTER to exit")

  system.terminate()
}
