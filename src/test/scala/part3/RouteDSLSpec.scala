package part3

import org.scalatest.wordspec.AnyWordSpecLike
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import org.scalatest.matchers.should.Matchers._



case class Book(id: Int, author: String, title: String)

trait BookJsonProtocol extends DefaultJsonProtocol {
  implicit val bookFormat: RootJsonFormat[Book] = jsonFormat3(Book)
}

object RouteDSL extends BookJsonProtocol with SprayJsonSupport {

  var books = List(
    Book(1, "Harper Lee", "To Kill a Mockingbird"),
    Book(2, "JRR Tolkien", "The Lord of the Rings"),
    Book(3, "GRR Marting", "A Song of Ice and Fire"),
    Book(4, "Tony Robbins", "Awaken the Giant Within")
  )

  def libraryRoute: Route = 
    pathPrefix("api" / "book") {
        get {
            pathEndOrSingleSlash {
                complete(books)
            }
        }
    }
}

class RouteDSLSpec 
  extends AnyWordSpecLike
  with ScalatestRouteTest {

  import RouteDSL._

  "A digital library backend" should {
    "return all the books in the library" in {
      Get("/api/book") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[Book]] shouldBe books
      }
    }
  }

}
