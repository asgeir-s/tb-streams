package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.cluda.coinsignals.streams.model.{SStream, Signal}
import com.cluda.coinsignals.streams.protocoll.{DuplicateSignal, StreamDoesNotExistException}

class PostSignalActor(streamID: String, tableName: String) extends Actor with ActorLogging {
  override def receive: Receive = {
    case signals: Seq[Signal] =>
      context.actorOf(CalculateStatsActor.props(streamID, tableName)) ! signals
      context.become(responder(sender()))
  }

  def responder(respondTo: ActorRef): Receive = {
    case stream: SStream =>
      respondTo ! HttpResponse(StatusCodes.Accepted, entity = stream.publicJson)

    case e: StreamDoesNotExistException =>
      respondTo ! HttpResponse(StatusCodes.NoContent)

    case e: DuplicateSignal =>
      respondTo ! HttpResponse(StatusCodes.Conflict)
  }
}

object PostSignalActor {
  def props(streamID: String, tableName: String): Props = Props(new PostSignalActor(streamID, tableName))
}