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

package controllers

import _root_.models._
import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportableEventsController @Inject() (val mcc: MessagesControllerComponents,
                                            val ersConnector: ErsConnector,
                                            val sessionService: FrontendSessionService,
                                            globalErrorView: views.html.global_error,
                                            reportableEventsView: views.html.reportable_events,
                                            authAction: AuthAction)
                                           (implicit val ec: ExecutionContext,
                                            val ersUtil: ERSUtil,
                                            val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging {

  def reportableEventsPage(): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      .flatMap { requestObj =>
          updateErsMetaData(requestObj)(request, hc)
            .flatMap {
              case Left(globalErrorPage: Result) => Future.successful(globalErrorPage)
              case Right(_) => showReportableEventsPage(requestObj)(request)
            }
      }
    }

  def updateErsMetaData(requestObject: RequestObject)
                       (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Either[Result, (String, String)]] =
    ersConnector
      .connectToEtmpSapRequest(requestObject.getSchemeReference)
      .flatMap {
        case Left(exception: Throwable) =>
          logger.error(s"[ReportableEventsController][updateErsMetaData] save failed with exception" +
            s" ${exception.getMessage}, timestamp: ${System.currentTimeMillis()}.")
          Future.successful(Left(getGlobalErrorPage))
        case Right(sapNumber: String) =>
          for {
            metaData: ErsMetaData <- sessionService.fetch[ErsMetaData](ersUtil.ERS_METADATA)
            updatedMetadata: (String, String) <- sessionService.cache(ersUtil.ERS_METADATA, metaData.copy(sapNumber = Some(sapNumber)))
          } yield Right(updatedMetadata)
      }

  def showReportableEventsPage(requestObject: RequestObject)
                              (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    sessionService.fetch[ReportableEvents](ersUtil.REPORTABLE_EVENTS).map { activity =>
      Ok(reportableEventsView(requestObject, activity.isNilReturn, RsFormMappings.chooseForm().fill(activity)))
    } recover { case _: NoSuchElementException =>
      val form = ReportableEvents(Some(""))
      Ok(reportableEventsView(requestObject, Some(""), RsFormMappings.chooseForm().fill(form)))
    }

  def reportableEventsSelected(): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObj =>
      showReportableEventsSelected(requestObj)(request) recover { case e: Exception =>
        logger.error(s"[ReportableEventsController][reportableEventsSelected] failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        getGlobalErrorPage
      }
    }
  }

  def showReportableEventsSelected(requestObject: RequestObject)
                                  (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    RsFormMappings
      .chooseForm()
      .bindFromRequest()
      .fold(
        errors => Future.successful(Ok(reportableEventsView(requestObject, Some(""), errors))),
        formData =>
          sessionService.cache(ersUtil.REPORTABLE_EVENTS, formData).map { _ =>
            if (formData.isNilReturn.get == ersUtil.OPTION_NIL_RETURN) {
              Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserBasedInUkController.questionPage())
            } else {
              logger.info(s"[ReportableEventsController][showReportableEventsSelected] Redirecting to FileUpload controller to get Partial, timestamp: ${System.currentTimeMillis()}.")
              Redirect(routes.CheckFileTypeController.checkFileTypePage())
            }
          } recover { case e: Exception =>
            logger.error(s"[ReportableEventsController][showReportableEventsSelected] Save reportable event failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
            getGlobalErrorPage
          }
      )

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    InternalServerError(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
