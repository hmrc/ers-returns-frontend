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

import config.ApplicationConfig
import connectors.ErsConnector
import javax.inject.{Inject, Singleton}
import models.upscan._
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.{Action, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class CsvFileUploadCallbackController @Inject()(val mcc: MessagesControllerComponents,
                                                val ersConnector: ErsConnector,
                                                val authConnector: DefaultAuthConnector,
                                                implicit val ersUtil: ERSUtil,
                                                implicit val appConfig: ApplicationConfig
                                               ) extends FrontendController(mcc) with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def callback(uploadId: UploadId, scRef: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      request.body.validate[UpscanCallback].fold (
        invalid = errors => {
          logger.error(s"[CsvFileUploadCallbackController][callback] Failed to validate UpscanCallback json with errors: $errors")
          Future.successful(BadRequest)
        },
        valid = callback => {
          val uploadStatus: UploadStatus = callback match {
            case callback: UpscanReadyCallback =>
              UploadedSuccessfully(callback.uploadDetails.fileName, callback.downloadUrl.toExternalForm)
            case UpscanFailedCallback(_, details) =>
              logger.warn(s"[CsvFileUploadCallbackController][callback] CSV Callback for upload id: ${uploadId.value} failed. Reason: ${details.failureReason}. Message: ${details.message}")
              Failed
          }
          logger.info(s"[CsvFileUploadCallbackController][callback] Updating CSV callback for " +
            s"upload id: ${uploadId.value} to ${uploadStatus.getClass.getSimpleName}")
          ersUtil.cache(s"${ersUtil.CHECK_CSV_FILES}-${uploadId.value}", uploadStatus, scRef).map {
            _ => Ok
          } recover {
            case NonFatal(e) =>
              logger.error(s"[CsvFileUploadCallbackController][callback] Failed to update cache after Upscan callback for UploadID: ${uploadId.value}, ScRef: $scRef", e)
              InternalServerError("Exception occurred when attempting to store data")
          }
        }
      )
  }
}
