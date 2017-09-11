package beam.performance

import akka.actor.Actor

class StatsPrinterActor extends Actor {
  var receivedStats: Seq[List[StatisticsSummary]] = Seq()
  def receive = {
    case "print" => {
      println("!!!!!!PRINT!!!!!!!! nr=%d".format(receivedStats.size))

      receivedStats.foreach(msg => {
        msg.filter(_.actorId.contains("Entry")).foreach(entry =>
          println("ENTRY: maxQueue=%d, utilization %02f:  %s".format(entry.maxQueueLength, entry.utilization, entry.toString)))
      })

      receivedStats.foreach(msg => {
        println("Received %s".format(msg.toString))
      })
    }
    case msg: List[StatisticsSummary] => {
      receivedStats = receivedStats :+ msg
      //simulate processing
      println("Received %s".format(msg.toString))
    }
  }
}
