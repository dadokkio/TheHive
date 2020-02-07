package org.thp.thehive.controllers.v1

import scala.util.Success

import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, Results}

import javax.inject.{Inject, Singleton}
import org.thp.scalligraph.ScalligraphApplicationLoader
import org.thp.scalligraph.auth.{AuthCapability, AuthSrv, MultiAuthSrv}
import org.thp.scalligraph.controllers.Entrypoint
import org.thp.scalligraph.services.config.{ApplicationConfig, ConfigItem}
import org.thp.thehive.TheHiveModule

@Singleton
class StatusCtrl @Inject() (entrypoint: Entrypoint, appConfig: ApplicationConfig, authSrv: AuthSrv) {

  private def getVersion(c: Class[_]): String = Option(c.getPackage.getImplementationVersion).getOrElse("SNAPSHOT")

  val passwordConfig: ConfigItem[String, String] = appConfig.item[String]("datastore.attachment.password", "Password used to protect attachment ZIP")
  def password: String                           = passwordConfig.get

  def get: Action[AnyContent] =
    entrypoint("status") { _ =>
      Success(
        Results.Ok(
          Json.obj(
            "versions" -> Json.obj(
              "Scalligraph" -> getVersion(classOf[ScalligraphApplicationLoader]),
              "TheHive"     -> getVersion(classOf[TheHiveModule]),
              "Play"        -> getVersion(classOf[AbstractController])
            ),
            "connectors" -> JsObject.empty,
            "health"     -> Json.obj("elasticsearch" -> "UNKNOWN"),
            "config" -> Json.obj(
              "protectDownloadsWith" -> password,
              "authType" -> (authSrv match {
                case multiAuthSrv: MultiAuthSrv => Json.toJson(multiAuthSrv.providerNames)
                case _                          => JsString(authSrv.name)
              }),
              "capabilities" -> authSrv.capabilities.map(c => JsString(c.toString)),
              "ssoAutoLogin" -> authSrv.capabilities.contains(AuthCapability.sso)
            )
          )
        )
      )
    }

}
