package beam.utils

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging._

import scala.collection.mutable

class AkkaMessageLogger extends Actor {

  def receive = {

    case InitializeLogger(bus) => sender ! LoggerInitialized
    case Error(cause, logSource, logClass, message: String) =>
    //println(format(logSource, message, Console.RED))
    case Warning(logSource, logClass, message: String) =>
    //println(format(logSource, message, Console.YELLOW))
    //mon ! format(logSource, message, Console.YELLOW)
    case Info(logSource, logClass, message: String) =>
    //println(format(logSource, message, Console.GREEN))
    //mon ! format(logSource, message, Console.GREEN)
    case Debug(logSource, logClass, message: String) =>
      //println(">>debug -> " + message)
      println(format(logSource, message, Console.GREEN))
      AkkaMessageLogger.collect(logSource, logClass, message)
    //mon ! format(logSource, message, Console.GREEN)
    case AkkaMessageLogger.GET_LIST => {
      println("Printing the list before sending it back to " + sender.path + " - " + sender)
      sender ! AkkaMessageLogger.debugMessages
    }
  }

  def format(src: String, msg: String, code: String) = s"${Console.BLUE}R> $code[${src.split('/').last}] ${Console.RESET}$msg"
}

object AkkaMessageLogger{

  val MAP_KEY_RECEIVED_HANDLED: String = "received handled message"
  case object GET_LIST

  val debugMessages: mutable.Map[String, mutable.MutableList[DebugMessage]] = mutable.Map()


  def collect(logSource: String, logClass: Class[_], message: String): Unit = {
    if (message.contains(MAP_KEY_RECEIVED_HANDLED)) {

      val mParts = message.split(" from ")
      val dm = DebugMessage(mParts(0).split(MAP_KEY_RECEIVED_HANDLED)(1).trim, mParts(1), logSource, logClass.getName, sequenceNumber(MAP_KEY_RECEIVED_HANDLED))

      collect(dm)
    }
  }

  def collect(m: DebugMessage): Unit = {

    if(debugMessages.contains(MAP_KEY_RECEIVED_HANDLED) )
      debugMessages(MAP_KEY_RECEIVED_HANDLED) += m
    else{
      debugMessages += (MAP_KEY_RECEIVED_HANDLED  -> mutable.MutableList(m))
    }
  }

  def printList(args: TraversableOnce[_]): Unit = {
    args.foreach(println)
  }

  def sequenceNumber(key: String) = {

    if(debugMessages.contains(key)){
      val messageList = debugMessages(key)
      messageList.size + 1
    }else{
      1
    }
  }
}

case class DebugMessage(message: String, sender: String, sourceActor: String, logActor: String, id: Int)
