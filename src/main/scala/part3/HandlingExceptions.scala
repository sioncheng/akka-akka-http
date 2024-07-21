package part3

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ExceptionHandler


object HandlingExceptions extends App {
  implicit val system : ActorSystem = ActorSystem("HandlingException")

  import system.dispatcher

  val simpleRoute = 
    path("api" / "people") {
      get {
        throw new RuntimeException("too long")
      } ~
      post {
        parameter('id) { id =>
          if (id.length() > 2) {
            throw new NoSuchElementException(s"not found")
          }
          complete(StatusCodes.OK)
        }
      }
    }

  implicit val customExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e2: IllegalArgumentException =>
      complete(StatusCodes.BadRequest, e2.getMessage())
    case e: RuntimeException =>
      complete(StatusCodes.NotFound, e.getMessage())
  }


  val otherRoute = 
    path("api" / "other") {
      handleExceptions(ExceptionHandler{
        case e: NoSuchElementException =>
          complete(StatusCodes.BadRequest, e.getMessage())
      }) {
        post {
          parameter('id) {id =>
            if (id.length() >= 2) {
              throw new NoSuchElementException(s"$id can not be found")
            }
            complete(StatusCodes.OK)
          }
        }
      }
    }

  Http().bindAndHandle(simpleRoute ~ otherRoute, "localhost", 8080)

  scala.io.StdIn.readLine("press ENTRY to exit \r\n")

  system.terminate()
}
