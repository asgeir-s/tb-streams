package com.cluda.tradersbit.streams.unit

import akka.actor.ActorSystem
import com.cluda.tradersbit.streams.TestData
import com.cluda.tradersbit.streams.{DatabaseTestUtil, TestData}
import com.cluda.tradersbit.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

class DatabaseUtilTest extends UnitTest {

  val system = ActorSystem
  val tableName = "databaseUtilTest"
  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())
  implicit val ec = system.apply().dispatcher
  val testDatabase = DatabaseTestUtil.createStreamsTable(dynamoDB, tableName)

  "when putting a stream and getting it back from the database the stream" should
    "be the same" in {
    DatabaseUtil.putStreamNew(testDatabase, TestData.adoptedStream).map{ stream =>
      val streamId = stream.id.get
      assert(streamId == TestData.adoptedStream.id.get)
      DatabaseUtil.getStreams(testDatabase, List(streamId)).map { streamsBack =>
        assert(streamsBack.isDefined)
        assert(streamsBack.get.last == TestData.adoptedStream)
      }
    }
  }

  "when putting a stream and getting it back from the database the stream for a new stream" should
    "be the same" in {
    DatabaseUtil.putStreamNew(testDatabase, TestData.freshStream).map { stream =>
      val streamId = stream.id.get
      assert(streamId == TestData.freshStream.id.get)
      DatabaseUtil.getStreams(testDatabase, List(streamId)).map { streamsBack =>
        assert(streamsBack.isDefined)
        assert(streamsBack.get.last == TestData.freshStream)
      }

    }
  }

}
