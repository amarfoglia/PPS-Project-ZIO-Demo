import it.unibo.chatapp.Message
import it.unibo.chatapp.Room
import it.unibo.chatapp.RoomId
import it.unibo.chatapp.UUID
import it.unibo.chatapp.User
import it.unibo.chatapp.UserId
import it.unibo.chatapp.http.Controller
import it.unibo.chatapp.repository.Repository
import it.unibo.chatapp.repository.RepositoryInMemory
import it.unibo.chatapp.repository.RepositoryInMemory.Config
import it.unibo.chatapp.repository.RepositoryInMemory.apply
import it.unibo.chatapp.service.Auth
import it.unibo.chatapp.service.Chat
import it.unibo.chatapp.service.Lobby
import zio.Clock
import zio.Console
import zio.Queue
import zio.Random
import zio.Ref
import zio.Schedule
import zio.Scope
import zio.UIO
import zio.ZIO
import zio.ZLayer
import zio.durationInt
import zio.stream.ZSink
import zio.stream.ZStream
import zio.test.assert
import zio.test.Assertion.equalTo
import zio.test.Assertion.fails
import zio.test.Assertion.failsWithA
import zio.test.Assertion.isEmpty
import zio.test.Assertion.isGreaterThan
import zio.test.Sized
import zio.test.TestAspect._
import zio.test.ZIOSpecDefault
import zio.test.test

import java.util.NoSuchElementException
import scala.concurrent.duration.fromNow
import zio.test.Gen
import zio.test.TestClock
import zio.test.TestRandom

object ExampleSpec extends ZIOSpecDefault {
  def indexedNameGen(prefix: String): Gen[Random, String] =
    Gen.long(1, 1000).map(i => s"$prefix $i")

  val roomGen: Gen[Random, Room] =
    for
      uuid <- Gen.uuid.map(UUID.fromJavaUUID)
      name <- indexedNameGen("Room")
    yield Room(uuid, name, "room for testing")

  val userGen: Gen[Random, User] =
    for
      uuid <- Gen.uuid.map(UUID.fromJavaUUID)
      name <- indexedNameGen("User")
    yield User(uuid, name)

  extension [R, A](gen: Gen[R, A])
    def toStream: ZStream[R, Nothing, A] =
      gen.sample.map(_.get.value).forever

  def spec = suite("Services")(
    suite("Auth service") {
      test("signup user") {
        val username = "John Smith"
        for user <- Auth.register(username)
        yield assert(user.name)(equalTo(username))
      }.provide(Auth.live, Repository.test[UserId, User])
    },
    suite("Room service") {
      val producer = roomGen.toStream
        .foreach(r => Lobby.addRoom(r.name, r.description))
        .delay(1.seconds)
        .forever

      test("collecting all the available rooms") {
        for
          _ <- producer.fork
          queue <- Queue.unbounded[Room]
          _ <- TestClock.adjust(5.seconds)
          _ <- Lobby.availableRooms.foreach(queue.offer).fork
          consumer <- ZIO.collectAll(ZIO.replicate(5)(queue.take)).fork
          results <- consumer.join
          storage <- ZIO.service[Repository[RoomId, Room]]
          rooms <- storage.all
        yield assert(rooms)(equalTo(results)) &&
          assert(rooms.size)(isGreaterThan(0))
      }

      test("tracking connected users") {
        for
          users <- indexedNameGen("User").toStream
            .take(3)
            .mapZIO(Auth.register(_))
            .runCollect
          roomId <- Lobby.addRoom("Test Room", "...").map(_.id)
          queue <- Queue.unbounded[Long]
          _ <- Lobby.connectedUsers(roomId).foreach(queue.offer(_)).forever.fork
          _ <- ZIO
            .foreach(users)(u => Lobby.joinRoom(roomId, u.id).delay(1.seconds))
            .fork
          _ <- TestClock.adjust(3.seconds)
          _ <- ZIO
            .foreach(users)(u => Lobby.leaveRoom(u.id))
            .delay(1.seconds)
            .fork
          _ <- TestClock.adjust(3.seconds)
          results <- queue.takeAll
        yield assert(results.toList)(equalTo(List(0, 1, 2, 3, 2, 1, 0)))
      }

      test("attempting to connect to a non-existent room") {
        for
          user <- Auth.register("John Smith")
          invalidRoomId <- Random.nextUUID.map(UUID.fromJavaUUID)
          exit <- Lobby.joinRoom(invalidRoomId, user.id).exit
        yield assert(exit)(failsWithA[NoSuchElementException])
      }

    }.provide(
      Lobby.live,
      Auth.live,
      Repository.test[RoomId, Room],
      Repository.test[UserId, User]
    ) @@ nonFlaky(10),
    suite("Chat service") {
      def publishers(roomId: RoomId) =
        for
          user <- userGen.toStream.schedule(Schedule.spaced(1.second))
          publication <- Chat.publishMessage(roomId, user, "Hello!")
        yield publication

      test("checking message flow") {
        for
          queue <- Queue.unbounded[Message]
          roomId <- Lobby.addRoom("Test Room", "...").map(_.id)
          _ <- Chat.roomMessages(roomId).foreach(queue.offer(_)).forever.fork
          _ <- publishers(roomId).runDrain.fork
          _ <- TestClock.adjust(5.seconds)
          res1 <- queue.takeAll
          _ <- TestClock.adjust(1.seconds)
          res2 <- queue.takeAll
          res3 <- queue.takeAll
        yield assert(res1.size)(equalTo(5)) &&
          assert(res2.size)(equalTo(1)) &&
          assert(res3)(isEmpty)
      }
    }.provide(
      Chat.live,
      Lobby.live,
      Repository.test[RoomId, Room],
      TestRandom.deterministic,
      Scope.default
    ) @@ nonFlaky(10)
  )
}
