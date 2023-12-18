/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.internal

import models.RequestWithUpdatedSession
import models.upscan._
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.{Action, MessagesControllerComponents}
import services.FileValidatorSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadCallbackController @Inject() (val mcc: MessagesControllerComponents,
                                              val sessionService: FileValidatorSessionService)
                                             (implicit val ec: ExecutionContext) extends FrontendController(mcc) with Logging {

  def callback(sessionId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body
      .validate[UpscanCallback]
      .fold(
        invalid = errors => {
          logger.error(
            s"[FileUploadCallbackController][callback] Failed to validate UpscanCallback json with errors: $errors"
          )
          Future.successful(BadRequest)
        },
        valid = callback => {
          val uploadStatus = callback match {
            case callback: UpscanReadyCallback    =>
              UploadedSuccessfully(callback.uploadDetails.fileName, callback.downloadUrl.toExternalForm)
            case UpscanFailedCallback(_, details) =>
              logger.warn(
                s"[FileUploadCallbackController][callback] Callback for session id: $sessionId failed. Reason: ${details.failureReason}. Message: ${details.message}"
              )
              Failed
          }

          logger.info(s"Updating callback for session: $sessionId to ${uploadStatus.getClass.getSimpleName}")
          sessionService.updateCallbackRecord(uploadStatus)(RequestWithUpdatedSession(request, sessionId)).map(_ => Ok) recover {
            case e: Throwable =>
              logger.error(
                s"[FileUploadCallbackController][callback] Failed to update callback record for session: $sessionId, timestamp: ${System
                  .currentTimeMillis()}.",
                e
              )
              InternalServerError("Exception occurred when attempting to update callback data")
          }
        }
      )
  }
}
