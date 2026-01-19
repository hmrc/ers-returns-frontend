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
import forms.YesNoFormProvider
import models.RequestObject
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{FrontendSessionService, TrusteeService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TrusteeRemoveController @Inject()(val mcc: MessagesControllerComponents,
                                        val authAction: AuthAction,
                                        trusteeRemoveView: views.html.trustee_remove_yes_no,
                                        yesNoFormProvider: YesNoFormProvider,
                                        globalErrorView: views.html.global_error,
                                        trusteeService: TrusteeService,
                                        val sessionService: FrontendSessionService)
                                       (implicit executionContext: ExecutionContext, appConfig: ApplicationConfig, ersUtil: ERSUtil)
  extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with I18nSupport with Logging {

  private val form: Form[Boolean] = yesNoFormProvider.withPrefix("ers_trustee_remove")

  def onPageLoad(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      trusteeDetailsList <- sessionService.fetchTrusteesOptionally()
    } yield {
      Ok(trusteeRemoveView(form, requestObject, trusteeDetailsList.trustees(index).name, index))
    }).recover {
      case _: IndexOutOfBoundsException =>
        logger.warn(s"[TrusteeRemoveController][onPageLoad] Requested index $index not found")
        Redirect(controllers.trustees.routes.TrusteeSummaryController.trusteeSummaryPage())
      case e: Throwable =>
        logger.error(s"[TrusteeRemoveController][onPageLoad] Get data from cache failed with exception ${e.getMessage}")
        getGlobalErrorPage
    }
  }

  def onSubmit(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    val requestObjectWithTrusteeList = for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      trusteeDetailsList <- sessionService.fetchTrusteesOptionally()
    } yield (requestObject, trusteeDetailsList)

    requestObjectWithTrusteeList.flatMap { case (requestObject, trusteeDetailsList) =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(
          trusteeRemoveView(
            formWithErrors,
            requestObject,
            trusteeDetailsList.trustees(index).name,
            index
          )
        )),
        {
          case true if trusteeDetailsList.trustees.size == 1 =>
            Future.successful(Redirect(controllers.trustees.routes.TrusteeRemoveProblemController.onPageLoad()))
          case true =>
            trusteeService.deleteTrustee(index).map {
              case true => Redirect(controllers.trustees.routes.TrusteeSummaryController.trusteeSummaryPage())
              case _ =>
                logger.error("s[TrusteeRemoveController][onSubmit] Error on submit")
                getGlobalErrorPage
            }
          case _ =>
            Future.successful(Redirect(controllers.trustees.routes.TrusteeSummaryController.trusteeSummaryPage()))
        }
      )
    }
  }

  def getGlobalErrorPage(implicit request: RequestHeader, messages: Messages): Result = {
    Ok(globalErrorView(
      "ers.global_errors.title",
      "ers.global_errors.heading",
      "ers.global_errors.message"
    )(request, messages, appConfig))
  }
}
