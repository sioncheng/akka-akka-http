package part3

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.Rejection
import akka.http.scaladsl.server.MethodRejection

object HandlingRejections extends App {
  
  implicit val system: ActorSystem = ActorSystem("HandlingRejections")
  import system.dispatcher

  val simpleRoute = 
    path ("api" / "myEndpoint") {
    //   get {
    //     complete(StatusCodes.OK)
    //   } ~ 
      get {
        parameter('id) { id =>
            complete(StatusCodes.OK,
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, id)
            )
        } 
      } ~
      get {
          complete(StatusCodes.OK)
      } 
    }

  val badRequestHandler: RejectionHandler = {rejections: Seq[Rejection] =>
    println(s"rejections: $rejections")
    Some(complete(StatusCodes.Forbidden))
  }
//   val badRequestHandler = RejectionHandler.newBuilder()
//     .handleAll((rejections:Seq[Rejection]) => { 
//         println(s"rejections: $rejections")
//         complete(StatusCodes.BadRequest)
//     })
//     .result()

  val otherRoute =
    handleRejections(badRequestHandler) {
      path("api" / "canyou") {
        (get | post) {
            complete(StatusCodes.OK)
        }
      }
    }

  val routes = simpleRoute ~ otherRoute

  Http().bindAndHandle(routes, "localhost", 8080)

  scala.io.StdIn.readLine("press ENTER to exit\r\n")

  system.terminate()
}
