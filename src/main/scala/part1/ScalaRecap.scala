package part1

import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure

object ScalaRecap extends App {
  val aCondition: Boolean = false
  def myFunction(x: Int) = {
    if (x > 4) 42 else 65
  }

  class Animal
  trait Carnivore {
    def eat(a: Animal): Unit
  }

  object Carnivore

  abstract class MyList[+A]

  println(1 + 2)
  println(1.+(2))

  val anIncrementer: Int => Int = (x: Int) => x + 1
  anIncrementer(1)

  List(1, 2, 3).map(anIncrementer)

  val unknown: Any = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  try {
    throw new RuntimeException
  } catch {
    case e: Exception => println("I caught one exception")
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    42
  }

  future.onComplete {
    case Success(value) => 
        println(s"I found the meaning of life: $value")
    case Failure(exception) => 
        println(s"I found $exception while searching for the meaning of life")
  }

  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case _ => 999
  }

  type AkkaReceive = PartialFunction[Any, Unit]
  def receive: AkkaReceive = {
    case 1 => println("hello!")
    case 2 => println("confused...")
  }

  receive {
    1
  }

  implicit val timeout: Int = 1000
  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()

  setTimeout(() => println("timeout"))

  case class Person(name: String) {
    def greet(): Unit = println(s"Hi, my name is $name")
  }

  implicit def fromStringToPerson(name: String): Person = Person(name)
  "Peter".greet

  implicit class Dog(name: String) {
    def bark = println("Bark")
  }
  "Lassie".bark

  implicit val numberOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,3,2).sorted.foreach(println)

  object Person {
    implicit val personOrdering: Ordering[Person] =
        Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }

  List(Person("Bob"), Person("Alice")).sorted.foreach(p => print(s"$p  "))
}
