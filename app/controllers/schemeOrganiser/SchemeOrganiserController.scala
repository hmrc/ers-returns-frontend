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

package controllers.schemeOrganiser

import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models._
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SchemeOrganiserController @Inject()(
                                           val mcc: MessagesControllerComponents,
                                           implicit val countryCodes: CountryCodes,
                                           implicit val ersUtil: ERSUtil,
                                           implicit val sessionService: FrontendSessionService,
                                           implicit val appConfig: ApplicationConfig,
                                           globalErrorView: views.html.global_error,
                                           schemeOrganiserSummaryView: views.html.scheme_organiser_summary,
                                           authAction: AuthAction
                                         ) extends FrontendController(mcc)
  with I18nSupport
  with WithUnsafeDefaultFormBinding
  with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def schemeOrganiserSummaryPage: Action[AnyContent] = authAction.async {
    implicit request =>
      showSchemeOrganiserSummaryPage(request, hc)
  }

  def showSchemeOrganiserSummaryPage(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      companyDetails <- sessionService.fetchSchemeOrganiserOptionally()
    } yield
      if (companyDetails.isDefined) {
        Ok(schemeOrganiserSummaryView(requestObject, companyDetails.get))
      } else {
        Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserBasedInUkController.questionPage())
  }) recover {
      case e: Exception =>
        logger.error(s"[SchemeOrganiserController][showSchemeOrganiserSummaryPage] Get data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        getGlobalErrorPage
    }
  }

  def companySummaryContinue(): Action[AnyContent] = authAction.async {
    implicit request =>
      continueFromCompanySummaryPage()(request, hc)
  }

  def continueFromCompanySummaryPage()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    Future(Redirect(controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage()))
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
