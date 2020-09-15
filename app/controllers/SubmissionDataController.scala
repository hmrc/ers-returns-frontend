/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.ErsConnector
import javax.inject.{Inject, Singleton}
import models.ERSAuthData
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.ERSUtil

import scala.concurrent.Future

@Singleton
class SubmissionDataController @Inject()(val messagesApi: MessagesApi,
																				 val authConnector: DefaultAuthConnector,
																				 val ersConnector: ErsConnector,
																				 implicit val ersUtil: ERSUtil,
																				 implicit val appConfig: ApplicationConfig
																				) extends FrontendController with Authenticator with I18nSupport {

  def createSchemeInfoFromURL(request: Request[Any]): Option[JsObject] = {

    val schemeRef: Option[String] = request.getQueryString("schemeRef")
    val timestamp: Option[String] = request.getQueryString("confTime")

    if (schemeRef.isDefined && timestamp.isDefined) {
      Some(
        Json.obj(
          "schemeRef" -> schemeRef.get,
          "confTime" -> timestamp.get
        )
      )
    }
    else {
      None
    }

  }

  def retrieveSubmissionData(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        getRetrieveSubmissionData()(user, request, hc)
  }

  def getRetrieveSubmissionData()(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {

    Logger.debug("Retrieve Submission Data Request")

    if (appConfig.enableRetrieveSubmissionData) {

      Logger.debug("Retrieve SubmissionData Enabled")

      val data: Option[JsObject] = createSchemeInfoFromURL(request)
      if (data.isDefined) {

        ersConnector.retrieveSubmissionData(data.get).map { res =>
          res.status match {
            case OK => Ok(res.body)
            case _ =>
							Logger.error(s"[SubmissionDataController][getRetrieveSubmissionData] retrieve status: ${res.status}")
							getGlobalErrorPage
					}
        }.recover {
          case ex: Exception =>
						Logger.error(s"[SubmissionDataController][getRetrieveSubmissionData] retrieve Exception: ${ex.getMessage}")
						getGlobalErrorPage
				}

      }
      else {
        Future.successful(NotFound(views.html.global_error(
					"ers_not_found.title",
					"ers_not_found.heading",
					"ers_not_found.message"
				)))
      }
    }
    else {
      Logger.debug("Retrieve SubmissionData Disabled")
      Future.successful(NotFound(views.html.global_error(
				"ers_not_found.title",
				"ers_not_found.heading",
				"ers_not_found.message"
			)))
    }
  }

	def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		Ok(views.html.global_error(
			"ers.global_errors.title",
			"ers.global_errors.heading",
			"ers.global_errors.message"
		)(request, messages, appConfig))
	}

}
