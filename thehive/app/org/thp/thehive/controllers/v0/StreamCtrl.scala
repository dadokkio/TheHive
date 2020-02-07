package org.thp.thehive.controllers.v0

import scala.concurrent.ExecutionContext
import scala.util.Success

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{Action, AnyContent, Results}

import javax.inject.{Inject, Singleton}
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.thp.scalligraph.controllers.Entrypoint
import org.thp.scalligraph.models.Database
import org.thp.scalligraph.steps.StepsOps._
import org.thp.thehive.controllers.v0.Conversion._
import org.thp.thehive.services._

@Singleton
class StreamCtrl @Inject() (
    entrypoint: Entrypoint,
    streamSrv: StreamSrv,
    auditSrv: AuditSrv,
    val caseSrv: CaseSrv,
    val taskSrv: TaskSrv,
    val userSrv: UserSrv,
    implicit val db: Database,
    implicit val ec: ExecutionContext
) extends AuditRenderer {

  def create: Action[AnyContent] =
    entrypoint("create stream")
      .auth { implicit request =>
        val streamId = streamSrv.create
        Success(Results.Ok(streamId))
      }

  def get(streamId: String): Action[AnyContent] =
    entrypoint("get stream").async { _ =>
      streamSrv
        .get(streamId)
        .map {
          case auditIds if auditIds.nonEmpty =>
            db.roTransaction { implicit graph =>
              val audits = auditSrv
                .getMainByIds(Order.desc, auditIds: _*)
                .richAuditWithCustomRenderer(auditRenderer)
                .toIterator
                .map {
                  case (audit, obj) =>
                    audit
                      .toJson
                      .as[JsObject]
                      .deepMerge(
                        Json.obj(
                          "base"    -> Json.obj("object" -> obj, "rootId" -> audit.context._id),
                          "summary" -> jsonSummary(auditSrv, audit.requestId)
                        )
                      )
                }
              Results.Ok(JsArray(audits.toSeq))
            }
          case _ => Results.Ok(JsArray.empty)
        }
    }

  def status: Action[AnyContent] = // TODO
    entrypoint("get stream") { _ =>
      Success(
        Results.Ok(
          Json.obj(
            "remaining" -> 3600,
            "warning"   -> false
          )
        )
      )
    }
}
