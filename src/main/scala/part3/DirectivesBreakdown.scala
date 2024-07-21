package part3

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpRequest
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http

object DirectivesBreakdown extends App {
  implicit val system: ActorSystem = ActorSystem("DirectivesBreakdown")

  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  val simpleHttpMethodRoute =
    post {
      complete(StatusCodes.Forbidden)
    }

  val simplePathRoute = 
    path("about") {
        complete(
            HttpEntity(
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
    }

  val complexPathRoute =
    path("api" / "myEndpoint") {
        println("complexPathRoute")
        complete(StatusCodes.OK)
    }

  val dontConfuse =
    path("api/myEndpoint") {
        println("dontConfuse")
        host()
        complete(StatusCodes.OK)
    }

  val pathEndRoute = 
    pathEndOrSingleSlash{
        complete(StatusCodes.OK)
    }

  val pathExtractionRoute = //GET /api/item/44
    path("api" / "item" / IntNumber) { (itemNumber: Int) =>
        println(s"itemNumber: $itemNumber")
        complete(StatusCodes.OK)
    }

  val pathMultiExtractRoute =
    path("api" / "order" / IntNumber / IntNumber) {(id, inventory) =>
        println(s"id: $id, inventory $inventory")
        complete(StatusCodes.OK)
    }
  
  val extractRequestRoute = 
    path("controlEndpoint") {
        extractRequest { (httpRequest: HttpRequest) =>
            extractLog { log =>
                log.info(s"http request: $httpRequest")
                complete(StatusCodes.OK)
            }
        }
    }

  val queryParamExtractionRoute =
    path("api" / "item") { // api/item?id=44
        parameter('id.as[Int]) {(itemId: Int) =>
            println(s"itemId $itemId")
            complete(StatusCodes.OK)
        }
    }

  val dryRoute = 
    (path("about") | path("aboutUs")) {
        complete(StatusCodes.OK)
    }

  val routes = queryParamExtractionRoute ~
    extractRequestRoute ~ 
    complexPathRoute ~
    dontConfuse ~
    dryRoute

  Http().bindAndHandle(routes, "localhost", 8000)


  scala.io.StdIn.readLine("press ENTRY to exit\r\n")

  system.terminate()
}

