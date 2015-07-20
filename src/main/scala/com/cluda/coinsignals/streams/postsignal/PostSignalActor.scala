package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.cluda.coinsignals.protocol.Sec
import com.cluda.coinsignals.streams.model.{SStream, Signal}
import com.cluda.coinsignals.streams.protocoll.{FatalStreamCorruptedException, StreamDoesNotExistException, UnexpectedSignalException}
import com.cluda.coinsignals.protocol.Sec._

class PostSignalActor(streamID: String, tableName: String) extends Actor with ActorLogging {
  override def receive: Receive = {
    case signals: Seq[Signal] =>
      context.actorOf(CalculateStatsActor.props(streamID, tableName)) ! signals
      context.become(responder(sender()))
  }

  def responder(respondTo: ActorRef): Receive = {
    case stream: SStream =>
      respondTo ! secureHttpResponse(StatusCodes.Accepted, entity = stream.publicJson)

    case e: StreamDoesNotExistException =>
      respondTo ! secureHttpResponse(StatusCodes.NoContent)

    case e: FatalStreamCorruptedException =>
      respondTo ! secureHttpResponse(StatusCodes.NotAcceptable,
        entity = """{ "error": """" + e.info  + """", "streamId": """" + e.streamId + """" }""")

    case e: UnexpectedSignalException =>
      respondTo ! secureHttpResponse(StatusCodes.Conflict, entity = """{ "error": """" + e.info + """ }""")
  }
}

object PostSignalActor {
  def props(streamID: String, tableName: String): Props = Props(new PostSignalActor(streamID, tableName))
}