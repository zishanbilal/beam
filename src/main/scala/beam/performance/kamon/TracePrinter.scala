package beam.performance.kamon

import akka.actor.{Actor, ActorLogging}
import kamon.trace.TraceInfo

class TracePrinter extends Actor with ActorLogging {
  def receive = {
    case traceInfo: TraceInfo =>

      log.debug("#################################################")
      log.debug("Trace Name: " + traceInfo.name)
      log.debug("Timestamp: " + traceInfo.elapsedTime)
      log.debug("Elapsed Time: " + traceInfo.elapsedTime)
      log.debug("Segments: ")

      traceInfo.segments.foreach { segmentInfo =>
        log.debug("    ------------------------------------------")
        log.debug("    Name: " + segmentInfo.name)
        log.debug("    Category: " + segmentInfo.category)
        log.debug("    Library: " + segmentInfo.library)
        log.debug("    Timestamp: " + segmentInfo.timestamp)
        log.debug("    Elapsed Time: " + segmentInfo.elapsedTime)
      }
  }
}
