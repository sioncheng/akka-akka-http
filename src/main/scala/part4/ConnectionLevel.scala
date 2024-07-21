package part4

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import scala.util.Success
import scala.util.Failure

object ConnectionLevel extends App {
  implicit val system: ActorSystem = ActorSystem("ConnectionLevel")

  import system.dispatcher

  val connectionFlow = Http().outgoingConnection("cn.bing.com")

  def oneOffRequest(request: HttpRequest) =
    Source.single(request).via(connectionFlow).runWith(Sink.head)

  oneOffRequest(HttpRequest()).onComplete {
    case Success(value) => 
        println(s"response: $value")
    case Failure(exception) => 
        println(s"failed: $exception")
  }

  scala.io.StdIn.readLine("press ENTER to exit\r\n")

  system.terminate()
}
