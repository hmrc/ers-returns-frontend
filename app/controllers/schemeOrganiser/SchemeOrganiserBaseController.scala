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
import models.{CompanyDetails, CompanyDetailsList, RequestObject}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Format
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import scala.concurrent.{ExecutionContext, Future}

trait SchemeOrganiserBaseController[A] extends FrontendController with I18nSupport with Logging {

  val ersUtil: ERSUtil
  val authAction: AuthAction
  val globalErrorView: views.html.global_error
  val appConfig: ApplicationConfig
  val cacheKey: String
  implicit val ec: ExecutionContext
  implicit val format: Format[A]

  def nextPageRedirect(index: Int, edit: Boolean = false)(implicit hc: HeaderCarrier): Future[Result]

  def form(implicit request: Request[AnyContent]): Form[A]

  def view(requestObject: RequestObject, index: Int, form: Form[A], edit: Boolean = false)
          (implicit request: Request[AnyContent], hc: HeaderCarrier): Html


  def questionPage(index: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
        showQuestionPage(requestObject, index)
      }
  }

  def showQuestionPage(requestObject: RequestObject, index: Int, edit: Boolean = false)
                      (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetchPartFromCompanyDetails[A](requestObject.getSchemeReference).map { previousAnswer: Option[A] =>
      val preparedForm = previousAnswer.fold(form)(form.fill(_))
      Ok(view(requestObject, index, preparedForm, edit))
    } recover {
      case e: Exception =>
        logger.error(s"[SubsidiariesController][showSubsidiariesNamePage] Get data from cache failed with exception ${e.getMessage}")
        getGlobalErrorPage
    }
  }


  def questionSubmit(index: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
        submissionHandler(requestObject, index)(request, hc)
      }
  }

  def submissionHandler(requestObject: RequestObject, index: Int, edit: Boolean = false)
                       (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      errors => {
        logger.error(errors.errors.mkString)
        Future.successful(BadRequest(view(requestObject, index, errors, edit)))
      },
      result => {
        if (edit) {
          ersUtil.fetch[CompanyDetails](ersUtil.SCHEME_ORGANISER_CACHE, requestObject.getSchemeReference).flatMap { schemeOrganiser =>
            println(s"schemeOrganiser: $schemeOrganiser")
            val updatedSchemeOrganiser = schemeOrganiser.updatePart(result)
            println(s"updatedSchemeOrganiser: $updatedSchemeOrganiser")
            ersUtil.cache[CompanyDetails](ersUtil.SCHEME_ORGANISER_CACHE, updatedSchemeOrganiser, requestObject.getSchemeReference).flatMap { _ =>
              nextPageRedirect(index, edit)
            }
          }
        } else {
          ersUtil.cache[A](cacheKey, result, requestObject.getSchemeReference).flatMap { _ =>
            nextPageRedirect(index, edit)
          }
        }
      }
    ).recover {
      case e: Exception =>
        logger.error(s"Exception: ${e.getStackTrace.mkString("\n", "\n", "\n")}\n")
        logger.error(s"[${this.getClass.getSimpleName}][submissionHandler] Error occurred while updating company cache")
        getGlobalErrorPage
    }
  }

  def editCompany(index: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      println(s"\n\n[${this.getClass.getSimpleName}] index is $index ")
      ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
        showQuestionPage(requestObject, index, edit = true)(request, hc)
      }
  }

  def editQuestionSubmit(index: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
        submissionHandler(requestObject, index, edit = true)(request, hc)
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
