/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.testonly

import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// moved from SubmissionDataController as part of DDCE-2159
@Singleton
class TestOnlySubmissionDataController @Inject()(val mcc: MessagesControllerComponents,
                                                 val ersConnector: ErsConnector,
                                                 globalErrorView: views.html.global_error,
                                                 authAction: AuthAction)
                                                (implicit val ec: ExecutionContext,
                                                 val ersUtil: ERSUtil,
                                                 val appConfig: ApplicationConfig) extends FrontendController(mcc)
  with I18nSupport with Logging {

  def retrieveSubmissionData(): Action[AnyContent] = authAction.async { implicit request =>
    getRetrieveSubmissionData()
  }

  def getRetrieveSubmissionData()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    logger.debug("Retrieve Submission Data Request")

    if (appConfig.enableRetrieveSubmissionData) {
      logger.debug("Retrieve SubmissionData Enabled")

      val data: Option[JsObject] = createSchemeInfoFromURL(request)
      if (data.isDefined) {
        ersConnector
          .retrieveSubmissionData(data.get)
          .map { res =>
            res.status match {
              case OK => Ok(res.body)
              case _ =>
                logger.error(s"[SubmissionDataController][getRetrieveSubmissionData] retrieve status: ${res.status}")
                getGlobalErrorPage()
            }
          }
          .recover { case ex: Exception =>
            logger.error(s"[SubmissionDataController][getRetrieveSubmissionData] retrieve Exception: ${ex.getMessage}")
            getGlobalErrorPage()
          }

      } else {
        notFoundGlobalErrorView()
      }
    } else {
      logger.debug("Retrieve SubmissionData Disabled")
      notFoundGlobalErrorView()
    }
  }

  def createSchemeInfoFromURL(request: Request[Any]): Option[JsObject] =
    (request.getQueryString("schemeRef"), request.getQueryString("confTime")) match {
      case (Some(schemeRef), Some(confTime)) =>
        Some(
          Json.obj(
            "schemeRef" -> schemeRef,
            "confTime" -> confTime
          )
        )
      case _ => None
    }

  private def getGlobalErrorPage()(implicit request: Request[_], messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )

  private def notFoundGlobalErrorView()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    Future.successful(
      NotFound(
        globalErrorView(
          "ers_not_found.title",
          "ers_not_found.heading",
          "ers_not_found.message"
        )
      )
    )
}
