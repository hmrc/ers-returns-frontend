/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportableEventsController @Inject()(val mcc: MessagesControllerComponents,
																					 val authConnector: DefaultAuthConnector,
																					 val ersConnector: ErsConnector,
																					 implicit val ersUtil: ERSUtil,
																					 implicit val appConfig: ApplicationConfig,
                                           globalErrorView: views.html.global_error,
                                           reportableEventsView: views.html.reportable_events,
                                           authAction: AuthAction
																					) extends FrontendController(mcc) with I18nSupport with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def reportableEventsPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObj =>
          updateErsMetaData(requestObj)(request, hc)
          showReportableEventsPage(requestObj)(request, hc)
        }
  }

  def updateErsMetaData(requestObject: RequestObject)(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Object] = {
		ersConnector.connectToEtmpSapRequest(requestObject.getSchemeReference).flatMap { sapNumber =>
      ersUtil.fetch[ErsMetaData](ersUtil.ersMetaData, requestObject.getSchemeReference).map { metaData =>
        val ersMetaData = ErsMetaData(
          metaData.schemeInfo, metaData.ipRef, metaData.aoRef, metaData.empRef, metaData.agentRef, Some(sapNumber))
        ersUtil.cache(ersUtil.ersMetaData, ersMetaData, requestObject.getSchemeReference).recover {
          case e: Exception =>
						logger.error(s"[ReportableEventsController][updateErsMetaData] save failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
						getGlobalErrorPage
				}
      } recover {
        case e: NoSuchElementException =>
					logger.error(s"[ReportableEventsController][updateErsMetaData] fetch failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
					getGlobalErrorPage
			}
    }
  }

  def showReportableEventsPage(requestObject: RequestObject)
                              (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetch[ReportableEvents](ersUtil.reportableEvents, requestObject.getSchemeReference).map { activity =>
      Ok(reportableEventsView(requestObject, activity.isNilReturn, RsFormMappings.chooseForm.fill(activity)))
    } recover {
      case _: NoSuchElementException =>
        val form = ReportableEvents(Some(""))
        Ok(reportableEventsView(requestObject, Some(""), RsFormMappings.chooseForm.fill(form)))
    }
  }

  def reportableEventsSelected(): Action[AnyContent] = authAction.async {
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObj =>
          showReportableEventsSelected(requestObj)(request) recover {
            case e: Exception =>
              logger.error(s"[ReportableEventsController][reportableEventsSelected] failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
              getGlobalErrorPage
          }
        }
  }

  def showReportableEventsSelected(requestObject: RequestObject)
                                  (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] = {
    RsFormMappings.chooseForm.bindFromRequest.fold(
      errors => {
        Future.successful(Ok(reportableEventsView(requestObject, Some(""), errors)))
      },
      formData => {
        ersUtil.cache(ersUtil.reportableEvents, formData, requestObject.getSchemeReference).map { _ =>
          if (formData.isNilReturn.get == ersUtil.OPTION_NIL_RETURN) {
            Redirect(routes.SchemeOrganiserController.schemeOrganiserPage())
          } else {
            logger.info(s"[ReportableEventsController][showReportableEventsSelected] Redirecting to FileUpload controller to get Partial, timestamp: ${System.currentTimeMillis()}.")
            Redirect(routes.CheckFileTypeController.checkFileTypePage())
          }
        } recover {
          case e: Exception =>
            logger.error(s"[ReportableEventsController][showReportableEventsSelected] Save reportable event failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
            getGlobalErrorPage
        }

      }
    )
  }

	def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		InternalServerError(globalErrorView(
			"ers.global_errors.title",
			"ers.global_errors.heading",
			"ers.global_errors.message"
		)(request, messages, appConfig))
	}
}
