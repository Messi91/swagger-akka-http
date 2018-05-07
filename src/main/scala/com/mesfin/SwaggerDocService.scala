package com.mesfin

import com.github.swagger.akka.model.Info

import scala.reflect.runtime.{ universe => ru }
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._

class SwaggerDocService(address: String, port: Int, system: ActorSystem) extends SwaggerHttpService with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiTypes = Seq(ru.typeOf[UserService])
  override val host = address + ":" + port
  override val info = Info(version = "1.0")
}
