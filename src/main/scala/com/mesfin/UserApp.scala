package com.mesfin

import akka.actor._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

object UserApp {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("myuser")

    system.actorOf(Props(new Master), "user-app-master")

    Await.ready(system.whenTerminated, Duration.Inf)
  }
}

class Master extends Actor with ActorLogging {
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  private val userRepository = context.watch(createUserRepository())
  context.watch(createHttpService(userRepository))

  log.info("Up and running")

  override def receive = {
    case Terminated(actor) => onTerminated(actor)
  }

  protected def createUserRepository(): ActorRef = {
    context.actorOf(UserRepository.props(), UserRepository.Name)
  }

  protected def createHttpService(userRepositoryActor: ActorRef): ActorRef = {
    context.actorOf(
      HttpService.props("0.0.0.0", 9001, 10.seconds, userRepositoryActor),
      HttpService.Name
    )
  }

  protected def onTerminated(actor: ActorRef): Unit = {
    log.error("Terminating the system because {} terminated!", actor)
    context.system.terminate()
  }
}
