package part4

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import scala.util.Success
import scala.util.Failure

object RequestLevel extends App {
  implicit val system: ActorSystem = ActorSystem("RequestLevel")
  import system.dispatcher

  val responseFuture = Http().singleRequest(HttpRequest(uri = "http://cn.bing.com"))

  responseFuture.onComplete {
    case Success(value) => 
        value.discardEntityBytes()
        println(value)
    case Failure(exception) => 
        println(exception)
  }

  scala.io.StdIn.readLine("press ENTER to exit\r\n")

  system.terminate()
}
