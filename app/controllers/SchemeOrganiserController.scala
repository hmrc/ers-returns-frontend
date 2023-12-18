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

package controllers

import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models._
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SchemeOrganiserController @Inject()(val mcc: MessagesControllerComponents,
                                          val sessionService: FrontendSessionService,
                                          globalErrorView: views.html.global_error,
                                          schemeOrganiserView: views.html.scheme_organiser,
                                          authAction: AuthAction)
                                         (implicit val ec: ExecutionContext,
                                          val ersUtil: ERSUtil,
                                          val appConfig: ApplicationConfig,
                                          val countryCodes: CountryCodes)
  extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging {

  def schemeOrganiserPage(): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
      showSchemeOrganiserPage(requestObject)(request)
    }
  }

  def showSchemeOrganiserPage(requestObject: RequestObject)
                             (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] = {
    logger.info(s"[SchemeOrganiserController][showSchemeOrganiserPage] schemeRef: ${requestObject.getSchemeReference}.")
    lazy val form = SchemeOrganiserDetails.emptyForm

    sessionService.fetch[ReportableEvents](ersUtil.REPORTABLE_EVENTS).flatMap {
      reportableEvent =>
        sessionService.fetchOption[CheckFileType](ersUtil.FILE_TYPE_CACHE, requestObject.getSchemeReference).flatMap {
          fileType =>
            sessionService
              .fetch[SchemeOrganiserDetails](ersUtil.SCHEME_ORGANISER_CACHE)
              .map { res =>
                val FileType = if (fileType.isDefined) {
                  fileType.get.checkFileType.get
                } else {
                  ""
                }
                Ok(
                  schemeOrganiserView(
                    requestObject,
                    FileType,
                    RsFormMappings.schemeOrganiserForm().fill(res),
                    reportableEvent.isNilReturn.get
                  )
                )
              } recover { case _: NoSuchElementException =>
              Ok(
                schemeOrganiserView(
                  requestObject,
                  fileType.get.checkFileType.get,
                  RsFormMappings.schemeOrganiserForm().fill(form),
                  reportableEvent.isNilReturn.get
                )
              )
            }
        } recover { case _: NoSuchElementException =>
          Ok(
            schemeOrganiserView(
              requestObject,
              "",
              RsFormMappings.schemeOrganiserForm().fill(form),
              reportableEvent.isNilReturn.get
            )
          )
        }
    } recover { case e: Exception =>
      logger.error(s"[SchemeOrganiserController][showSchemeOrganiserPage] Get reportableEvent.isNilReturn failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
      getGlobalErrorPage
    }
  }

  def schemeOrganiserSubmit(): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
      showSchemeOrganiserSubmit(requestObject)(request)
    }
  }

  def showSchemeOrganiserSubmit(requestObject: RequestObject)
                               (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] = {
    RsFormMappings.schemeOrganiserForm().bindFromRequest().fold(
      errors => {
        val correctOrder = errors.errors.map(_.key).distinct
        val incorrectOrderGrouped = errors.errors.groupBy(_.key).map(_._2.head).toSeq
        val correctOrderGrouped = correctOrder.flatMap(x => incorrectOrderGrouped.find(_.key == x))
        val firstErrors: Form[models.SchemeOrganiserDetails] = new Form[SchemeOrganiserDetails](errors.mapping, errors.data, correctOrderGrouped, errors.value)
        Future.successful(Ok(schemeOrganiserView(requestObject, "", firstErrors)))
      },
      successful => {
        sessionService.cache(ersUtil.SCHEME_ORGANISER_CACHE, successful).map {
          _ => Redirect(routes.GroupSchemeController.groupSchemePage())
        } recover {
          case e: Exception =>
            logger.error(s"[SchemeOrganiserController][showSchemeOrganiserSubmit] Save scheme organiser details failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
            getGlobalErrorPage
        }
      }
    )
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
