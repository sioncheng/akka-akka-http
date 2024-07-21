package part3

import akka.actor.ActorSystem
import part2.GuitarStoreJsonProtocol
import akka.actor.Props
import part2.Guitar
import akka.util.Timeout
import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import scala.concurrent.Future
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.pattern.ask
import spray.json._
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.Http

object HighLevelExample extends App with GuitarStoreJsonProtocol{

  implicit val system: ActorSystem = ActorSystem("highLevelExample")
  import system.dispatcher
  import part2.GuitarDB._

  val guitarDb = system.actorOf(Props[part2.GuitarDB], "lowLevelGuitarDB")
  val guitarList = List(
    Guitar("Fender", "Stratocaster"),
    Guitar("Gibson", "Les Paul"),
    Guitar("Martin", "LX1")
  )

  guitarList.foreach {g =>
    guitarDb ! CreateGuitar(g)
  }

  implicit val timeout: Timeout = Timeout(2.seconds)
  val guitarServerRoute =
    path("api" / "guitar") {
      parameter('id.as[Int]) {guitarId =>
        get {
          val guitarFuture: Future[Option[Guitar]] =
            (guitarDb ? FindGuitar(guitarId)).mapTo[Option[Guitar]]
          val entityFuture = guitarFuture.map {guitarOption =>
            HttpEntity(
              ContentTypes.`application/json`,
              guitarOption.toJson.prettyPrint
            )
          }
          complete(entityFuture)
        }// ~ 
        // get {
        //   val guitarFuture: Future[List[Guitar]] = 
        //     (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
        //   val entityFuture = guitarFuture.map {guitars =>
        //     HttpEntity(
        //       ContentTypes.`application/json`,
        //       guitars.toJson.prettyPrint
        //     )
        //   }
        //   complete(entityFuture)
        // }
      }
    }

  implicit def toHttpEntity1(x: String): akka.http.scaladsl.model.HttpEntity.Strict = 
    HttpEntity(ContentTypes.`application/json`, x.toJson.prettyPrint)
  
  val simplifiedGuitarServerRoute = 
    (pathPrefix("api" / "guitar") & get) {
      val guitarFuture: Future[List[Guitar]] = 
            (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
      val httpEntity = guitarFuture.mapTo[List[Guitar]]
        .map(_.toJson.prettyPrint)
      complete(httpEntity)
    }

  val routes = guitarServerRoute ~
    simplifiedGuitarServerRoute

  Http().bindAndHandle(routes, "localhost", 8080)

  scala.io.StdIn.readLine("press ENTER to exit\r\n")

  system.terminate()
}
