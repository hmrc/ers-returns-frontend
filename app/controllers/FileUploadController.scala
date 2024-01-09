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
import akka.actor.ActorSystem
import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models.upscan.{Failed, UploadStatus, UploadedSuccessfully}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{FileValidatorSessionService, FrontendSessionService, UpscanService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadController @Inject() (val mcc: MessagesControllerComponents,
                                      val ersConnector: ErsConnector,
                                      val fileValidatorSessionService: FileValidatorSessionService,
                                      val sessionService: FrontendSessionService,
                                      val upscanService: UpscanService,
                                      globalErrorView: views.html.global_error,
                                      fileUploadErrorsView: views.html.file_upload_errors,
                                      upscanOdsFileUploadView: views.html.upscan_ods_file_upload,
                                      fileUploadProblemView: views.html.file_upload_problem,
                                      authAction: AuthAction)
                                     (implicit val ec: ExecutionContext,
                                      val ersUtil: ERSUtil,
                                      val appConfig: ApplicationConfig,
                                      val actorSystem: ActorSystem)
  extends FrontendController(mcc) with I18nSupport with Retryable with Logging {

  def uploadFilePage(): Action[AnyContent] = authAction.async { implicit request =>
    val requestObjectFuture = sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
    val upscanFormFuture = upscanService.getUpscanFormDataOds()
    (for {
      requestObject <- requestObjectFuture
      response <- upscanFormFuture
      _ <- fileValidatorSessionService.createCallbackRecord
    } yield Ok(upscanOdsFileUploadView(requestObject, response))) recover { case e: Throwable =>
      logger.error(s"[FileUploadController][uploadFilePage] failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
      getGlobalErrorPage
    }
  }

  def success(): Action[AnyContent] = authAction.async { implicit request =>
    val futureCallbackData: Future[Option[UploadStatus]] =
      fileValidatorSessionService.getCallbackRecord.withRetry(appConfig.odsSuccessRetryAmount) { opt =>
        opt.fold(true) {
          case _: UploadedSuccessfully | Failed => true
          case _ => false
        }
      }

    (for {
      file <- futureCallbackData
    } yield file match {
      case Some(file: UploadedSuccessfully) =>
        if (file.name.contains(".csv")) {
          logger.info("[FileUploadController][success] User uploaded a csv file instead of an ods file")
          Future(getFileUploadProblemPage())
        } else if (!file.name.contains(".ods")) {
          logger.info("[FileUploadController][success] User uploaded a non ods file")
          Future(getFileUploadProblemPage())
        } else {
          sessionService.cache[String](ersUtil.FILE_NAME_CACHE, file.name).map { _ =>
            Redirect(routes.FileUploadController.validationResults())
          }
        }
      case Some(Failed) =>
        logger.warn("[FileUploadController][success] Upload status is failed")
        Future.successful(getGlobalErrorPage)
      case None =>
        logger.warn(s"[FileUploadController][success] Failed to verify upload. No data found in cache")
        throw new Exception("Upload data missing in cache for ODS file.")
    }).flatMap(identity) recover {
      case e: LoopException[Option[UploadStatus] @unchecked] =>
        logger.error(s"[FileUploadController][success] Failed to verify upload. Upload status: ${e.finalFutureData.flatten}", e)
        getGlobalErrorPage
      case e: Exception =>
        logger.error(s"[FileUploadController][success] failed to save ods file with exception ${e.getMessage}.", e)
        getGlobalErrorPage
    }
  }

  def validationResults(): Action[AnyContent] = authAction.async { implicit request =>
    val futureCallbackData  = fileValidatorSessionService.getCallbackRecord.withRetry(appConfig.odsValidationRetryAmount)(
      _.exists(_.isInstanceOf[UploadedSuccessfully])
    )

    (for {
      all <- sessionService.fetch[ErsMetaData](ersUtil.ERS_METADATA)
      connectorResponse <- ersConnector.removePresubmissionData(all.schemeInfo)
      callbackData <- futureCallbackData
      validationResponse <-
        if (connectorResponse.status == OK) {
          handleValidationResponse(callbackData.get.asInstanceOf[UploadedSuccessfully], all.schemeInfo)
        } else {
          logger.error(s"[FileUploadController][validationResults] removePresubmissionData failed with status ${connectorResponse.status}, " +
              s"timestamp: ${System.currentTimeMillis()}.")
          Future.successful(getGlobalErrorPage)
        }
    } yield validationResponse) recover {
      case e: LoopException[Option[UploadStatus] @unchecked] =>
        logger.error(s"[FileUploadController][validationResults] Failed to validate as file is not yet successfully uploaded. Current cache data: ${e.finalFutureData.flatten}", e)
        getGlobalErrorPage
      case e: Throwable =>
        logger.error(s"[FileUploadController][validationResults] validationResults failed with Exception ${e.getMessage}", e)
        getGlobalErrorPage
    }
  }

  def handleValidationResponse(callbackData: UploadedSuccessfully, schemeInfo: SchemeInfo)
                              (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    val schemeRef = schemeInfo.schemeRef
    ersConnector.validateFileData(callbackData, schemeInfo).map { res =>
      logger.info(s"[FileUploadController][handleValidationResponse] Response from validator: ${res.status}, timestamp: ${System.currentTimeMillis()}.")
      res.status match {
        case OK =>
          logger.info(s"[FileUploadController][handleValidationResponse] Validation is successful for schemeRef: $schemeRef, timestamp: ${System.currentTimeMillis()}.")
          sessionService.cache(ersUtil.VALIDATED_SHEETS, res.body)
          Redirect(routes.SchemeOrganiserController.schemeOrganiserPage())
        case ACCEPTED =>
          logger.warn(s"[FileUploadController][handleValidationResponse] Validation is not successful for schemeRef: $schemeRef, timestamp: ${System.currentTimeMillis()}.")
          Redirect(routes.FileUploadController.validationFailure())
        case _ =>
          logger.error(s"[FileUploadController][handleValidationResponse] Validate file data failed with Status ${res.status}, timestamp: ${System.currentTimeMillis()}.")
          getGlobalErrorPage
      }
    }
  }

  def validationFailure(): Action[AnyContent] = authAction.async { implicit request =>
    logger.info("[FileUploadController][validationFailure] Validation Failure: " + (System.currentTimeMillis() / 1000))
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      fileType      <- sessionService.fetch[CheckFileType](ersUtil.FILE_TYPE_CACHE)
    } yield {
      Ok(fileUploadErrorsView(requestObject, fileType.checkFileType.getOrElse("")))
    }) recover {
      case e: Throwable =>
        logger.error(s"[FileUploadController][validationFailure] failed with Exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
        getGlobalErrorPage
    }
  }

  def failure(): Action[AnyContent] = authAction.async { implicit request =>
    val errorCode      = request.getQueryString("errorCode").getOrElse("Unknown")
    val errorMessage   = request.getQueryString("errorMessage").getOrElse("Unknown")
    val errorRequestId = request.getQueryString("errorRequestId").getOrElse("Unknown")
    logger.error(s"Upscan Failure. errorCode: $errorCode, errorMessage: $errorMessage, errorRequestId: $errorRequestId")
    Future.successful(getFileUploadProblemPage())
  }

  def getFileUploadProblemPage()(implicit request: Request[_], messages: Messages): Result =
    BadRequest(
      fileUploadProblemView(
        "ers.file_problem.title"
      )(request, messages, appConfig)
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
