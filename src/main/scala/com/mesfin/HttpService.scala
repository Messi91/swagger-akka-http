package com.mesfin

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.pattern.{ask, pipe}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import io.swagger.annotations._

import scala.concurrent.ExecutionContext

object HttpService extends CorsSupport {

  // $COVERAGE-OFF$
  final val Name = "http-service"
  // $COVERAGE-ON$

  def props(address: String, port: Int, internalTimeout: Timeout, userRepository: ActorRef): Props =
    Props(new HttpService(address, port, internalTimeout, userRepository))

  private def route(httpService: ActorRef, address: String, port: Int, internalTimeout: Timeout,
                    userRepository: ActorRef, system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) = {
    import akka.http.scaladsl.server.Directives._
    import io.circe.generic.auto._

    def assets = pathPrefix("swagger") {
      getFromResourceDirectory("swagger") ~ pathSingleSlash(get(redirect("index.html", StatusCodes.PermanentRedirect))) }

    assets ~ corsHandler(new UserService(userRepository, internalTimeout).route) ~ corsHandler(new SwaggerDocService(address, port, system).routes)

  }
}

class HttpService(address: String, port: Int, internalTimeout: Timeout, userRepository: ActorRef)
  extends Actor with ActorLogging {
  import HttpService._
  import context.dispatcher

  private implicit val mat = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(route(self, address, port, internalTimeout, userRepository, context.system), address, port)
    .pipeTo(self)

  override def receive = binding

  private def binding: Receive = {
    case serverBinding @ Http.ServerBinding(address) =>
      log.info("Listening on {}", address)

    case Status.Failure(cause) =>
      log.error(cause, s"Can't bind to $address:$port")
      context.stop(self)
  }
}

@Api(value = "/users", produces = "application/json")
class UserService(userRepository: ActorRef, internalTimeout: Timeout)(implicit executionContext: ExecutionContext) extends Directives {
  import de.heikoseeberger.akkahttpcirce.CirceSupport._
  import io.circe.generic.auto._

  implicit val timeout = internalTimeout

  val route = pathPrefix("users") { usersGetAll ~ userPost }

  @ApiOperation(value = "Get list of all users", nickname = "getAllUsers", httpMethod = "GET", response = classOf[UserRepository.User], responseContainer = "Set")
  def usersGetAll = get {
    complete {
      (userRepository ? UserRepository.GetUsers).mapTo[Set[UserRepository.User]]
    }
  }

  @ApiOperation(value = "Create new user", nickname = "userPost", httpMethod = "POST", produces = "text/plain")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "user", dataType = "UserRepository$User", paramType = "body", required = true)
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "User created"),
    new ApiResponse(code = 409, message = "User already exists")
  ))
  def userPost = post {
    entity(as[UserRepository.User]) { user =>
      onSuccess(userRepository ? UserRepository.AddUser(user.name)) {
        case UserRepository.UserAdded(_)  => complete(StatusCodes.Created)
        case UserRepository.UserExists(_) => complete(StatusCodes.Conflict)
      }
    }
  }
}
