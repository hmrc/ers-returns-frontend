/*
 * Copyright 2025 HM Revenue & Customs
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
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.FrontendSessionService
import services.pdf.ErsReceiptPdfBuilderService
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{CacheHelper, ERSUtil}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PdfGenerationController @Inject() (val mcc: MessagesControllerComponents,
                                         val pdfBuilderService: ErsReceiptPdfBuilderService,
                                         val sessionService: FrontendSessionService,
                                         globalErrorView: views.html.global_error,
                                         authAction: AuthAction)
                                        (implicit val ec: ExecutionContext,
                                         val ersUtil: ERSUtil,
                                         val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with CacheHelper {

  def buildPdfForBundle(bundle: String, dateSubmitted: String): Action[AnyContent] = authAction.async {
    implicit request =>
      sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
        generatePdf(bundle, dateSubmitted)
      }
  }

  def generatePdf(bundle: String, dateSubmitted: String)
                 (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] = {
    for {
      allMetaData <- sessionService.fetch[ErsMetaData](ersUtil.ERS_METADATA)
      allData <- sessionService.getAllData(bundle, allMetaData)
      allCache <- sessionService.fetchAll()
      filesUploaded = extractFilesUploaded(allCache)
      pdf = pdfBuilderService.createPdf(allData, Some(filesUploaded), dateSubmitted).toByteArray
    } yield Ok(pdf).as("application/pdf").withHeaders(CONTENT_DISPOSITION -> s"inline; filename=$bundle-confirmation.pdf")
  } recoverWith {
    case e: Exception =>
      logger.error(s"[PdfGenerationController][generatePdf] Error generating PDF: ${e.getMessage}", e)
      Future.successful(getGlobalErrorPage())
  }

  private def extractFilesUploaded(all: CacheItem): List[String] = {
    val reportableEventsOpt = getEntry[ReportableEvents](all, DataKey(ersUtil.REPORTABLE_EVENTS))

    reportableEventsOpt.flatMap(_.isNilReturn) match {
      case Some(ersUtil.OPTION_UPLOAD_SPREEDSHEET) =>
        val fileTypeOpt = getEntry[CheckFileType](all, DataKey(ersUtil.FILE_TYPE_CACHE)).flatMap(_.checkFileType)
        fileTypeOpt match {
          case Some(ersUtil.OPTION_CSV) =>
            processCsvFiles(all)
          case _ =>
            getEntry[String](all, DataKey(ersUtil.FILE_NAME_CACHE)).toList
        }

      case _ => List.empty
    }
  }

  private def processCsvFiles(all: CacheItem): List[String] = {
    val csvCallbackOpt = getEntry[UpscanCsvFilesCallbackList](all, DataKey(ersUtil.CHECK_CSV_FILES))

    csvCallbackOpt match {
      case Some(csvCallback) if csvCallback.areAllFilesSuccessful() =>
        val csvFilesCallback = csvCallback.files.collect {
          case successfulFile @ UpscanCsvFilesCallback(_, _, _: UploadedSuccessfully) => successfulFile
        }
        csvFilesCallback.collect {
          case UpscanCsvFilesCallback(_, _, status: UploadedSuccessfully) => status.name
        }
      case Some(_) => throw new Exception("Not all CSV files have been completed")
      case None => throw new Exception(s"Cache data missing for key: ${ersUtil.CHECK_CSV_FILES} in CacheMap")
    }
  }

  def getGlobalErrorPage(status: Status = InternalServerError)(implicit request: RequestHeader, messages: Messages): Result =
    status(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
