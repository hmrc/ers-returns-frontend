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

package controllers.trustees

import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models.{RequestObject, RsFormMappings}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.TrusteeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{CountryCodes, ERSUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TrusteeSummaryController @Inject()(val mcc: MessagesControllerComponents,
                                         val ersConnector: ErsConnector,
                                         val trusteeService: TrusteeService,
                                         implicit val countryCodes: CountryCodes,
                                         implicit val ersUtil: ERSUtil,
                                         implicit val appConfig: ApplicationConfig,
                                         globalErrorView: views.html.global_error,
                                         trusteeSummaryView: views.html.trustee_summary,
                                         authAction: AuthAction
                                        ) extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def deleteTrustee(id: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      for {
        deleted <- trusteeService.deleteTrustee(id)
      } yield if (deleted) {
        Redirect(controllers.trustees.routes.TrusteeSummaryController.trusteeSummaryPage())
      } else {
        getGlobalErrorPage
      }
  }

  def trusteeSummaryPage(): Action[AnyContent] = authAction.async {
    implicit request =>
      showTrusteeSummaryPage()(request, hc)
  }

  def showTrusteeSummaryPage()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    (for {
      requestObject      <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      trusteeDetailsList <- ersUtil.fetchTrusteesOptionally(requestObject.getSchemeReference)
    } yield {
      if (trusteeDetailsList.trustees.isEmpty) {
        Redirect(controllers.trustees.routes.TrusteeNameController.questionPage())
      } else {
        Ok(trusteeSummaryView(requestObject, trusteeDetailsList))
      }
    }) recover {
      case e: Exception =>
        logger.error(s"[TrusteeController][showTrusteeSummaryPage] Get data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        getGlobalErrorPage
    }
  }

  def trusteeSummaryContinue(): Action[AnyContent] = authAction.async {
    implicit request =>
      continueFromTrusteeSummaryPage()(request, hc)
  }

  def continueFromTrusteeSummaryPage()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    RsFormMappings.addTrusteeForm().bindFromRequest().fold(
      _ => {
        for {
          requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
          trusteeDetailsList <- ersUtil.fetchTrusteesOptionally(requestObject.getSchemeReference)
        } yield {
          BadRequest(trusteeSummaryView(requestObject, trusteeDetailsList, formHasError = true))
        }
      },
      addTrustee => {
        if (addTrustee.addTrustee) {
          Future.successful(Redirect(controllers.trustees.routes.TrusteeNameController.questionPage()))
        } else {
          Future.successful(Redirect(controllers.routes.AltAmendsController.altActivityPage()))
        }
      }
    ).recover {
      _ =>
        getGlobalErrorPage
    }
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
    Ok(globalErrorView(
      "ers.global_errors.title",
      "ers.global_errors.heading",
      "ers.global_errors.message"
    )(request, messages, appConfig))
  }
}
