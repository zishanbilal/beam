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
      AkkaMessageLogger.logMessage(logSource, logClass, message)
    //mon ! format(logSource, message, Console.GREEN)
    case AkkaMessageLogger.GetList(filter: String) => {
      println("Printing the list before sending it back to " + sender.path + " - " + sender)
      sender ! AkkaMessageLogger.getMessagesWithString(filter)
    }
  }

  def format(src: String, msg: String, code: String) = s"${Console.BLUE}R> $code[${src.split('/').last}] ${Console.RESET}$msg"
}

object AkkaMessageLogger{

  case class GetList(filter: String)

  val MAP_KEY_RECEIVED_HANDLED: String = "received handled message"
  val debugMessages: mutable.Map[String, mutable.MutableList[AkkaDebugMessage]] = mutable.Map()

  def logMessage(logSource: String, logClass: Class[_], message: String): Unit = {
    if (message.contains(MAP_KEY_RECEIVED_HANDLED)) {

      val mParts = message.split(" from ")
      val keyWithMessageText = mParts(0)
      val sender = mParts(1)
      val keyAndText = keyWithMessageText.split(MAP_KEY_RECEIVED_HANDLED)
      val key = keyAndText(0)
      val text = keyAndText(1)

      val dm = AkkaDebugMessage(text, sender, logSource, logClass.getName, sequenceNumber(MAP_KEY_RECEIVED_HANDLED))

      logMessage(dm)
    }
  }

  def logMessage(m: AkkaDebugMessage): Unit = {

    if(debugMessages.contains(MAP_KEY_RECEIVED_HANDLED) )
      debugMessages(MAP_KEY_RECEIVED_HANDLED) += m
    else{
      debugMessages += (MAP_KEY_RECEIVED_HANDLED  -> mutable.MutableList(m))
    }
  }

  def getMessagesWithString(filter: String): mutable.MutableList[AkkaDebugMessage] = {

    val list: mutable.MutableList[AkkaDebugMessage] = debugMessages.get(MAP_KEY_RECEIVED_HANDLED).get
    list.filter((item: AkkaDebugMessage) => item.message.contains(filter))
  }

  def sequenceNumber(key: String) = {

    if(debugMessages.contains(key)){
      val messageList = debugMessages(key)
      messageList.size + 1
    }else{
      1
    }
  }

  def printList(args: TraversableOnce[_]): Unit = {
    args.foreach(println)
  }
}

case class AkkaDebugMessage(message: String, sender: String, logSourceActor: String, logSourceActorClass: String, id: Int)
