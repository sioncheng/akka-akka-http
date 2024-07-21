package part2

import java.security.KeyStore
import java.io.InputStream
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.SSLContext
import java.security.SecureRandom
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.ConnectionContext
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.Http

object HttpsContext {
  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val ksFile: InputStream = 
    getClass().getClassLoader().getResourceAsStream("keystore.pkcs12")
  val password = "akka-https".toCharArray()
  ks.load(ksFile, password)

  val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  trustManagerFactory.init(ks)

  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers(),
    trustManagerFactory.getTrustManagers(),
    new SecureRandom())
  
  val httpsConnCtx: HttpsConnectionContext =
    ConnectionContext.https(sslContext)

}

object LowLevelHttps extends App {
  implicit val system: ActorSystem = ActorSystem("LowLevelHttps")

  val reqHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP!
            | </body>
            |</html>
          """.stripMargin
        )
      )

    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound, // 404
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPS! The resource can't be found.
            | </body>
            |</html>
          """.stripMargin
        )
      )
  }

  val httpsBind = Http().bindAndHandleSync(reqHandler,
  "localhost",
  8443,
  HttpsContext.httpsConnCtx)

  val httpsBind2 = Http().bindAndHandleSync(reqHandler,
  "localhost",
  8080)

  scala.io.StdIn.readLine("press ENTRY to exit")

  system.terminate()

  
}