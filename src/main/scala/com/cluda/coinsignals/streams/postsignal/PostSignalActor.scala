package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.cluda.coinsignals.protocol.SendReceiveHelper
import com.cluda.coinsignals.streams.model.{SStream, Signal}
import com.cluda.coinsignals.streams.protocoll.{FatalStreamCorruptedException, StreamDoesNotExistException, UnexpectedSignalException}
import com.cluda.coinsignals.protocol.SendReceiveHelper._

class PostSignalActor(streamID: String, tableName: String) extends Actor with ActorLogging {
  override def receive: Receive = {
    case signals: Seq[Signal] =>
      context.actorOf(CalculateStatsActor.props(streamID, tableName)) ! signals
      context.become(responder(sender()))
  }

  def responder(respondTo: ActorRef): Receive = {
    case stream: SStream =>
      respondTo ! SecureHttpResponse(StatusCodes.Accepted, entity = stream.publicJson)

    case e: StreamDoesNotExistException =>
      respondTo ! HttpResponse(StatusCodes.NoContent)

    case e: FatalStreamCorruptedException =>
      respondTo ! SecureHttpResponse(StatusCodes.NotAcceptable,
        entity = """{ "error": """" + e.info  + """", "streamId": """" + e.streamId + """" }""")

    case e: UnexpectedSignalException =>
      respondTo ! SecureHttpResponse(StatusCodes.Conflict, entity = """{ "error": """" + e.info + """ }""")
  }
}

object PostSignalActor {
  def props(streamID: String, tableName: String): Props = Props(new PostSignalActor(streamID, tableName))
}