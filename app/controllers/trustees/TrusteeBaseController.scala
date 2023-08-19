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
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models.{GroupSchemeInfo, RequestObject}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Format
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.Html
import services.TrusteeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import scala.concurrent.{ExecutionContext, Future}

trait TrusteeBaseController[A] extends FrontendController with I18nSupport with Logging {

  val ersUtil: ERSUtil
  val trusteeService: TrusteeService
  val authAction: AuthAction
  val globalErrorView: views.html.global_error
  val appConfig: ApplicationConfig
  val cacheKey: String
  implicit val ec: ExecutionContext
  implicit val format: Format[A]

  def nextPageRedirect(index: Int)(implicit hc: HeaderCarrier): Future[Result]

  def form(implicit request: Request[AnyContent]): Form[A]

  def view(requestObject: RequestObject, index: Int, form: Form[A])
          (implicit request: Request[AnyContent], hc: HeaderCarrier): Html


  def questionPage(index: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
        showQuestionPage(requestObject, index)
      }
  }

  def showQuestionPage(requestObject: RequestObject, index: Int)
                      (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetchPartFromTrusteeDetailsList[A](index, requestObject.getSchemeReference).map { previousAnswer: Option[A] =>
      val preparedForm = if (previousAnswer.isDefined) form.fill(previousAnswer.get) else form
      if (previousAnswer.isDefined) {
        logger.error(s"\n\n[${this.getClass.getSimpleName}][showQuestionPage] Here's the answers we pulled from the cache innit: \n\n Prev answer: ${previousAnswer.get}\n") // TODO Remove before merge innit
      }
      Ok(view(requestObject, index, preparedForm))
    } recover {
      case e: Throwable =>
        logger.error(s"[${this.getClass.getSimpleName}][showQuestionPage] Get data from cache failed with exception ${e.getMessage}")
        getGlobalErrorPage
    }
  }

  def questionSubmit(index: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
        handleQuestionSubmit(requestObject, index)(request, hc)
      }
  }

  def handleQuestionSubmit(requestObject: RequestObject, index: Int)
                          (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      errors => {
          Future.successful(BadRequest(view(requestObject, index, errors)))
      },
      result => {
        ersUtil.cache[A](cacheKey, result, requestObject.getSchemeReference).flatMap { _ =>
          nextPageRedirect(index)
        }
      }
    )
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
    Ok(globalErrorView(
      "ers.global_errors.title",
      "ers.global_errors.heading",
      "ers.global_errors.message"
    )(request, messages, appConfig))
  }
}
