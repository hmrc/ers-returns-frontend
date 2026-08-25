/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import config.ApplicationConfig
import controllers.auth.AuthAction
import models._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class IncorrectFileNameController @Inject() (
  val mcc: MessagesControllerComponents,
  val sessionService: FrontendSessionService,
  incorrectFileNameView: views.html.incorrect_file_name,
  authAction: AuthAction
)(implicit
  val ec: ExecutionContext,
  val ersUtil: ERSUtil,
  val appConfig: ApplicationConfig
) extends FrontendController(mcc) with I18nSupport with Logging {

  def incorrectFileNamePage(): Action[AnyContent] =
    authAction.async { implicit request =>
      for {
        requestObject: RequestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      } yield BadRequest(
        incorrectFileNameView(requestObject)(request, request2Messages, appConfig)
      )
    }

}
