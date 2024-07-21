package part1

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Stash
import akka.actor.Props
import akka.actor.SupervisorStrategy
import akka.actor.OneForOneStrategy
import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ParserSettings.ErrorLoggingVerbosity.Simple
import akka.actor.PoisonPill
import akka.util.Timeout

object AkkaRecap /*extends App */{
  class SimpleActor extends Actor with ActorLogging with Stash {
    override def receive: Receive = {
      case "createChild" =>
        val childActor = context.actorOf(Props[SimpleActor], "myChild")
        childActor ! "hello"
      case "stashThis" =>
        stash()
      case "change handler NOW" =>
        unstashAll()
        context.become(anotherHandler)
      case "change" =>
        context.become(anotherHandler)
      case x: Any =>
        val path = context.self.path
        println(s"$path received: $x")
    }

    def anotherHandler: Receive = {
        case message =>
            println(s"in another receive handler $message")
    }

    override def preStart(): Unit = {
        log.info(s"starting")
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
        case _: RuntimeException => SupervisorStrategy.Restart
        case _ => SupervisorStrategy.Stop
    }

  }

  def main(arggs: Array[String]): Unit = {
    val system = ActorSystem("AkkaRecap")
    val actor = system.actorOf(Props[SimpleActor], "simpleActor")

    actor ! "hello"

    //actor ! PoisonPill

    import system.dispatcher
    import scala.concurrent.duration._
    system.scheduler.scheduleOnce(1.seconds) {
        actor ! "delayed happy birthday!"
    }

    Thread.sleep(2000)

    // Akka patterns including FSM + ask pattern
    import akka.pattern.ask
    implicit val timeout: Timeout = Timeout(2.seconds)

    val future = actor ? "question"

    // the pipe pattern
    import akka.pattern.pipe
    val anotherActor = system.actorOf(Props[SimpleActor], "anotherSimpleActor")
    future.mapTo[String].pipeTo(anotherActor)
  }
}
