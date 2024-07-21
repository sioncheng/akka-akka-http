package part2

import akka.actor.ActorLogging
import akka.actor.Actor
import spray.json.DefaultJsonProtocol
import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.Timeout
import akka.http.scaladsl.model.Uri.Query
import scala.concurrent.Future
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.Http

case class Guitar(make: String, model: String, quantity: Int = 0)

object GuitarDB {
  case class CreateGuitar(guitar: Guitar)
  case class GuitarCreated(id: Int)
  case class FindGuitar(id: Int)
  case object FindAllGuitars
  case class AddQuantity(id: Int, quantity: Int)
  case class FindGuitarsInStock(inStock: Boolean)
}

class GuitarDB extends Actor with ActorLogging {
  import GuitarDB._

  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarId: Int = 0

  override def receive: Receive = {
    case FindAllGuitars =>
        log.info("Searching for all guitars")
        sender() ! guitars.values.toList
    case FindGuitar(id) =>
        log.info(s"searching guitar by id: $id")
        sender() ! guitars.get(id)
    case CreateGuitar(guitar) =>
        log.info(s"Adding guitar $guitar with id $currentGuitarId")
        guitars = guitars + (currentGuitarId -> guitar)
        sender() ! GuitarCreated(currentGuitarId)
        currentGuitarId += 1
    case AddQuantity(id, quantity) =>
        log.info(s"trying to add $quantity items for guitar $id")
        val guitar: Option[Guitar] = guitars.get(id)
        val newGuitar: Option[Guitar] = guitar.map {
            case Guitar(make, model, q) => 
                Guitar(make, model, quantity + q) 
        }
        newGuitar.foreach(g => guitars = guitars + (id -> g))
        sender() ! newGuitar
    case FindGuitarsInStock(inStock) =>
        log.info(s"searching for all guitars" +
          s" ${if(inStock) "in" else "out of"} stock")
        if (inStock) {
            sender() ! guitars.values.filter(_.quantity > 0)
        } else {
            sender() ! guitars.values.filter(_.quantity == 0)
        }
  }
}


trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
implicit val guitarFormat: spray.json.RootJsonFormat[part2.Guitar] = jsonFormat3(Guitar)
}

object LowLevelRest extends App with GuitarStoreJsonProtocol {
    implicit val system: ActorSystem = ActorSystem("LowLevelRest")
    import system.dispatcher
    import GuitarDB._
    import spray.json._

    val simpleGuitar = Guitar("Fender", "Stratocaster")
    print(simpleGuitar.toJson.prettyPrint)

    val simpleGuitarJsonString =
    """
      |{
      |  "make": "Fender",
      |  "model": "Stratocaster",
      |  "quantity": 3
      |}
    """.stripMargin
    println(simpleGuitarJsonString.parseJson.convertTo[Guitar])
  
    val guitarDb = system.actorOf(Props[GuitarDB], "guitarDB")
    guitarDb ! CreateGuitar(Guitar("Fender", "Stratocaster"))
    guitarDb ! CreateGuitar(Guitar("Gibson", "Les Paul"))
    guitarDb ! CreateGuitar(Guitar("Martin", "LX1"))

    import scala.concurrent.duration._
    implicit val defaultTimeout: Timeout = Timeout(2.seconds)
    import akka.pattern.ask

    def getGuitar(query: Query): Future[HttpResponse] = {
      val guitarId = query.get("id").map(_.toInt)

      guitarId match {
        case None => 
          Future(HttpResponse(StatusCodes.NotFound))
        case Some(id) => 
          val guitarFuture: Future[Option[Guitar]] = 
            (guitarDb ? FindGuitar(id)).mapTo[Option[Guitar]]
          guitarFuture.map {
            case None => 
              HttpResponse(StatusCodes.NotFound)
            case Some(value) => 
              HttpResponse(
                entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    value.toJson.prettyPrint
                )
              )
          }
      }
    }

    val reqHandler: HttpRequest => Future[HttpResponse] = {
      case HttpRequest(HttpMethods.POST, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
        val query = uri.query()
        val guitarId: Option[Int] = query.get("id").map(_.toInt)
        val guitarQuantity: Option[Int] = query.get("quantity").map(_.toInt)

        val validGuitarResponse: Option[Future[HttpResponse]] = for {
          id <- guitarId
          quantity <- guitarQuantity
        } yield {
          val newGuitarFuture: Future[Option[Guitar]] =
             (guitarDb ? AddQuantity(id, quantity)).mapTo[Option[Guitar]]
          newGuitarFuture.map(_ => HttpResponse(StatusCodes.OK))
        }

        validGuitarResponse.getOrElse(Future(HttpResponse(StatusCodes.BadRequest)))
    
      case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
        val query = uri.query()
        val inStockOption = query.get("inStock").map(_.toBoolean)

        inStockOption match {
            case Some(inStock) =>
            val guitarsFuture: Future[List[Guitar]] = (guitarDb ? FindGuitarsInStock(inStock)).mapTo[List[Guitar]]
            guitarsFuture.map { guitars =>
                HttpResponse(
                entity = HttpEntity(
                    ContentTypes.`application/json`,
                    guitars.toJson.prettyPrint
                )
                )
            }
            case None => Future(HttpResponse(StatusCodes.BadRequest))
        }

      case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>
        val query = uri.query() // query object <=> Map[String, String]
        if (query.isEmpty) {
            val guitarsFuture: Future[List[Guitar]] = (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
            guitarsFuture.map { guitars =>
                HttpResponse(
                    entity = HttpEntity(
                    ContentTypes.`application/json`,
                    guitars.toJson.prettyPrint
                    )
                )
            }
        } else {
            // fetch guitar associated to the guitar id
            // localhost:8080/api/guitar?id=45
            getGuitar(query)
        }
      case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
      // entities are a Source[ByteString]
      val strictEntityFuture = entity.toStrict(3.seconds)
      strictEntityFuture.flatMap { strictEntity =>

        val guitarJsonString = strictEntity.data.utf8String
        val guitar = guitarJsonString.parseJson.convertTo[Guitar]

        val guitarCreatedFuture: Future[GuitarCreated] = (guitarDb ? CreateGuitar(guitar)).mapTo[GuitarCreated]
        guitarCreatedFuture.map { _ =>
          HttpResponse(StatusCodes.OK)
        }
      }

    case request: HttpRequest =>
      println(s"not foound $request ${request.uri}")
      request.discardEntityBytes()
      Future {
        HttpResponse(status = StatusCodes.NotFound)
      }
    }

    Http().bindAndHandleAsync(reqHandler, "localhost", 8080)

    scala.io.StdIn.readLine("press ENTRY to exit")

    system.terminate()
  }