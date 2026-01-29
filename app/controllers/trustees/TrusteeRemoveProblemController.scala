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

package controllers.trustees

import config.ApplicationConfig
import controllers.auth.AuthAction
import models.RequestObject
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.FrontendSessionService
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TrusteeRemoveProblemController @Inject() (
  val mcc: MessagesControllerComponents,
  val authAction: AuthAction,
  val ersUtil: ERSUtil,
  val sessionService: FrontendSessionService,
  trusteeRemoveProblemView: views.html.trustee_remove_problem,
  globalErrorView: views.html.global_error
)(implicit executionContext: ExecutionContext, appConfig: ApplicationConfig)
    extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
    } yield Ok(trusteeRemoveProblemView(requestObject))).recover { case e: Throwable =>
      logger.error(
        s"[TrusteeRemoveProblemController][onPageLoad] Get data from cache failed with exception ${e.getMessage}"
      )
      Ok(
        globalErrorView(
          "ers.global_errors.title",
          "ers.global_errors.heading",
          "ers.global_errors.message"
        )(request, implicitly[Messages], appConfig)
      )
    }
  }

}
