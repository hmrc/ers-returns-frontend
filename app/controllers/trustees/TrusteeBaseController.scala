/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import scala.concurrent.{ExecutionContext, Future}

trait TrusteeBaseController[A] extends FrontendController with I18nSupport with Logging {

  val ersUtil: ERSUtil
  val authAction: AuthAction
  val globalErrorView: views.html.global_error
  val appConfig: ApplicationConfig
  val cacheKey: String
  implicit val ec: ExecutionContext
  implicit val format: Format[A]

  val nextPageRedirect: Result

  def form(implicit request: Request[AnyContent]): Form[A]

  def view(requestObject: RequestObject, groupSchemeActivity: String, index: Int, form: Form[A])
          (implicit request: Request[AnyContent], hc: HeaderCarrier): Html


  def questionPage(index: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
        showQuestionPage(requestObject, index)
      }
  }

  def showQuestionPage(requestObject: RequestObject, index: Int)
                      (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetch[GroupSchemeInfo](ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, requestObject.getSchemeReference).map { groupSchemeActivity =>
      Ok(view(requestObject, groupSchemeActivity.groupScheme.getOrElse(ersUtil.DEFAULT), index, form))
    } recover {
      case e: Exception =>
        logger.error(s"[TrusteeController][showTrusteeNamePage] Get data from cache failed with exception ${e.getMessage}")
        getGlobalErrorPage
    }
  }

  def questionSubmit(index: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
        showQuestionSubmit(requestObject, index)(request, hc)
      }
  }

  def showQuestionSubmit(requestObject: RequestObject, index: Int)
                        (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      errors => {
        ersUtil.fetch[GroupSchemeInfo](ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, requestObject.getSchemeReference).map { groupSchemeActivity =>
          Ok(view(requestObject, groupSchemeActivity.groupScheme.getOrElse(ersUtil.DEFAULT), index, errors))
        } recover {
          case e: Exception =>
            logger.error(s"[${this.getClass.getSimpleName}][showQuestionSubmit] Get data from cache failed with exception ${e.getMessage}, " +
              s"timestamp: ${System.currentTimeMillis()}.")
            getGlobalErrorPage
        }
      },
      result =>
        ersUtil.cache[A](cacheKey, result, requestObject.getSchemeReference).map { _ =>
          nextPageRedirect
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
