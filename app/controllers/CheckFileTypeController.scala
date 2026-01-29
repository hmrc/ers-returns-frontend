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

package controllers

import config.ApplicationConfig
import controllers.auth.{AuthActionGovGateway, RequestWithOptionalAuthContext}
import models._
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckFileTypeController @Inject() (
  val mcc: MessagesControllerComponents,
  val sessionService: FrontendSessionService,
  globalErrorView: views.html.global_error,
  checkFileTypeView: views.html.check_file_type,
  authActionGovGateway: AuthActionGovGateway
)(implicit val ec: ExecutionContext, val ersUtil: ERSUtil, val appConfig: ApplicationConfig)
    extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging {

  def checkFileTypePage(): Action[AnyContent] = authActionGovGateway.async { implicit request =>
    sessionService.fetch[ErsMetaData](ersUtil.ERS_METADATA).map { ele =>
      logger.info(
        s"[CheckFileTypeController][checkFileTypePage()] Fetched request object with SAP Number: ${ele.sapNumber} " +
          s"and schemeRef: ${ele.schemeInfo.schemeRef}"
      )
    }
    showCheckFileTypePage()(request)
  }

  def showCheckFileTypePage()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      fileType      <- sessionService
                         .fetch[CheckFileType](ersUtil.FILE_TYPE_CACHE)
                         .recover { case _: NoSuchElementException => CheckFileType(Some("")) }
    } yield Ok(
      checkFileTypeView(requestObject, fileType.checkFileType, RsFormMappings.checkFileTypeForm().fill(fileType))
    )).recover { case e: Throwable =>
      logger.error(
        s"[CheckFileTypeController][showCheckFileTypePage] Rendering CheckFileType view failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}."
      )
      getGlobalErrorPage
    }

  def checkFileTypeSelected(): Action[AnyContent] = authActionGovGateway.async { implicit request =>
    showCheckFileTypeSelected()(request)
  }

  def showCheckFileTypeSelected()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
      RsFormMappings
        .checkFileTypeForm()
        .bindFromRequest()
        .fold(
          errors => Future.successful(Ok(checkFileTypeView(requestObject, Some(""), errors))),
          formData =>
            sessionService
              .cache(ersUtil.FILE_TYPE_CACHE, formData)
              .map { _ =>
                if (formData.checkFileType.contains(ersUtil.OPTION_ODS)) {
                  Redirect(routes.FileUploadController.uploadFilePage())
                } else {
                  Redirect(routes.CheckCsvFilesController.checkCsvFilesPage())
                }
              }
              .recover { case e: Exception =>
                logger.error(
                  "[CheckFileTypeController][showCheckFileTypeSelected] Unable to save file type. Error: " + e.getMessage
                )
                getGlobalErrorPage
              }
        )
    }

  def getGlobalErrorPage(implicit request: RequestHeader, messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )

}
