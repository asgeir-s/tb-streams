package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.cluda.coinsignals.streams.model.{SStream, Signal}
import com.cluda.coinsignals.streams.protocoll.{FatalStreamCorruptedException, StreamDoesNotExistException, UnexpectedSignalException}

class PostSignalActor(globalRequestID: String, streamID: String, tableName: String) extends Actor with ActorLogging {
  override def receive: Receive = {
    case signals: Seq[Signal] =>
      log.info(s"[$globalRequestID]: Received " + signals.length + " new signals.")
      context.actorOf(CalculateStatsActor.props(globalRequestID, streamID, tableName)) ! signals
      context.become(responder(sender()))
  }

  def responder(respondTo: ActorRef): Receive = {
    case stream: SStream =>
      log.info(s"[$globalRequestID]: Got back stream with id: " + stream.id.get)
      respondTo ! HttpResponse(StatusCodes.Accepted, entity = stream.publicJson)

    case e: StreamDoesNotExistException =>
      log.warning(s"[$globalRequestID]: Got back 'StreamDoesNotExistException' for stream with id: $streamID. Returning 'NoContent'.")
      respondTo ! HttpResponse(StatusCodes.NoContent)

    case e: FatalStreamCorruptedException =>
      log.error(s"[$globalRequestID]: Got back 'FatalStreamCorruptedException' for stream with id: $streamID. Returning 'NotAcceptable'.")
      respondTo ! HttpResponse(StatusCodes.NotAcceptable,
        entity = """{ "error": """" + e.info  + """", "streamId": """" + e.streamId + """" }""")

    case e: UnexpectedSignalException =>
      log.error(s"[$globalRequestID]: Got back 'UnexpectedSignalException' for stream with id: $streamID. Returning 'Conflict'.")
      respondTo ! HttpResponse(StatusCodes.Conflict, entity = """{ "error": """" + e.info + """ }""")

    case e: Throwable =>
      log.error(s"[$globalRequestID]: Got back 'Exception' for stream with id: $streamID. Returning 'InternalServerError'. Error: " + e.toString)
      respondTo ! HttpResponse(StatusCodes.InternalServerError)
  }
}

object PostSignalActor {
  def props(globalRequestID: String, streamID: String, tableName: String): Props = Props(new PostSignalActor(globalRequestID, streamID, tableName))
}