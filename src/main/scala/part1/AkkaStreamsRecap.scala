package part1

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep

object AkkaStreamsRecap extends App {
  implicit val system: ActorSystem = ActorSystem("AkkaStreamsRecap")
  import system.dispatcher

  val source = Source(1 to 10)
  val sink = Sink.foreach[Int](println)
  val flow = Flow[Int].map(x => x * 2)

  val runnableGraph = source.via(flow).to(sink)
  runnableGraph.run()

  Thread.sleep(500)

  source.viaMat(flow)(Keep.right).toMat(sink)(Keep.left).run()

  Thread.sleep(1000)
}
