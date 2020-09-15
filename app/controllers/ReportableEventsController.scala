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

import _root_.models.{RsFormMappings, _}
import config.ApplicationConfig
import connectors.ErsConnector
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils._

import scala.concurrent.Future

@Singleton
class ReportableEventsController @Inject()(val messagesApi: MessagesApi,
																					 val authConnector: DefaultAuthConnector,
																					 val ersConnector: ErsConnector,
																					 implicit val ersUtil: ERSUtil,
																					 implicit val appConfig: ApplicationConfig
																					) extends FrontendController with Authenticator with I18nSupport {

  def reportableEventsPage(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObj =>
          updateErsMetaData(requestObj)(user, request, hc)
          showReportableEventsPage(requestObj)(user, request, hc)
        }
  }

  def updateErsMetaData(requestObject: RequestObject)(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Object] = {
		ersConnector.connectToEtmpSapRequest(requestObject.getSchemeReference).flatMap { sapNumber =>
      ersUtil.fetch[ErsMetaData](ersUtil.ersMetaData, requestObject.getSchemeReference).map { metaData =>
        val ersMetaData = ErsMetaData(
          metaData.schemeInfo, metaData.ipRef, metaData.aoRef, metaData.empRef, metaData.agentRef, Some(sapNumber))
        ersUtil.cache(ersUtil.ersMetaData, ersMetaData, requestObject.getSchemeReference).recover {
          case e: Exception =>
						Logger.error(s"[ReportableEventsController][updateErsMetaData] save failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
						getGlobalErrorPage
				}
      } recover {
        case e: NoSuchElementException =>
					Logger.error(s"[ReportableEventsController][updateErsMetaData] fetch failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
					getGlobalErrorPage
			}
    }
  }

  def showReportableEventsPage(requestObject: RequestObject)(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetch[ReportableEvents](ersUtil.reportableEvents, requestObject.getSchemeReference).map { activity =>
      Ok(views.html.reportable_events(requestObject, activity.isNilReturn, RsFormMappings.chooseForm.fill(activity)))
    } recover {
      case _: NoSuchElementException =>
        val form = ReportableEvents(Some(""))
        Ok(views.html.reportable_events(requestObject, Some(""), RsFormMappings.chooseForm.fill(form)))
    }
  }

  def reportableEventsSelected(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObj =>
          showReportableEventsSelected(requestObj)(user, request) recover {
            case e: Exception =>
              Logger.error(s"[ReportableEventsController][reportableEventsSelected] failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
              getGlobalErrorPage
          }
        }
  }

  def showReportableEventsSelected(requestObject: RequestObject)(implicit authContext: ERSAuthData, request: Request[AnyRef]): Future[Result] = {
    RsFormMappings.chooseForm.bindFromRequest.fold(
      errors => {
        Future.successful(Ok(views.html.reportable_events(requestObject, Some(""), errors)))
      },
      formData => {
        ersUtil.cache(ersUtil.reportableEvents, formData, requestObject.getSchemeReference).map { _ =>
          if (formData.isNilReturn.get == ersUtil.OPTION_NIL_RETURN) {
            Redirect(routes.SchemeOrganiserController.schemeOrganiserPage())
          } else {
            Logger.info(s"[ReportableEventsController][showReportableEventsSelected] Redirecting to FileUpload controller to get Partial, timestamp: ${System.currentTimeMillis()}.")
            Redirect(routes.CheckFileTypeController.checkFileTypePage())
          }
        } recover {
          case e: Exception =>
            Logger.error(s"[ReportableEventsController][showReportableEventsSelected] Save reportable event failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
            getGlobalErrorPage
        }

      }
    )
  }

	def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		Ok(views.html.global_error(
			"ers.global_errors.title",
			"ers.global_errors.heading",
			"ers.global_errors.message"
		)(request, messages, appConfig))
	}
}
