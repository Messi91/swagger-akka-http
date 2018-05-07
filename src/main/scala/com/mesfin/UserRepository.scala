package com.mesfin

import akka.actor.{Actor, ActorLogging, Props}

object UserRepository {

  case class User(name: String)
  case object GetUsers
  case class AddUser(name: String)
  case class UserAdded(user: User)
  case class UserExists(name: String)

  final val Name = "user-repository"
  def props(): Props = Props[UserRepository]
}

class UserRepository extends Actor with ActorLogging {
  import UserRepository._

  private var users = Set.empty[User]

  override def receive = {
    case GetUsers =>
      log.debug("received GetUsers command")
      sender() ! users
    case AddUser(name) if users.exists(_.name == name) =>
      sender() ! UserExists(name)
    case AddUser(name) =>
      log.info(s"Adding new user with name; $name")
      val user = User(name)
      users += user
      sender() ! UserAdded(user)
  }
}
