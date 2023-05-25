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
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.pdf.ErsReceiptPdfBuilderService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import javax.inject.{Inject, Singleton}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PdfGenerationController @Inject()(val mcc: MessagesControllerComponents,
                                        val authConnector: DefaultAuthConnector,
                                        val pdfBuilderService: ErsReceiptPdfBuilderService,
                                        implicit val ersUtil: ERSUtil,
                                        implicit val appConfig: ApplicationConfig,
                                        globalErrorView: views.html.global_error,
                                        authAction: AuthAction
                                       ) extends FrontendController(mcc) with I18nSupport with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def buildPdfForBundle(bundle: String, dateSubmitted: String): Action[AnyContent] = authAction.async {
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
          generatePdf(requestObject, bundle, dateSubmitted)
        }
  }

  def generatePdf(requestObject: RequestObject, bundle: String, dateSubmitted: String)
                 (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {

    logger.debug("getting into the controller to generate the pdf")
    val cache: Future[ErsMetaData] = ersUtil.fetch[ErsMetaData](ersUtil.ersMetaData, requestObject.getSchemeReference)
    cache.flatMap { all =>
      logger.debug("pdf generation: got the metadata")
      ersUtil.getAllData(bundle, all).flatMap { alldata =>
        logger.debug("pdf generation: got the cache map")

        ersUtil.fetchAll(requestObject.getSchemeReference).map { all =>
          val filesUploaded: ListBuffer[String] = ListBuffer()
          if (all.getEntry[ReportableEvents](ersUtil.reportableEvents).get.isNilReturn.get == ersUtil.OPTION_UPLOAD_SPREEDSHEET) {
            val fileType = all.getEntry[CheckFileType](ersUtil.FILE_TYPE_CACHE).get.checkFileType.get
            if (fileType == ersUtil.OPTION_CSV) {
              val csvCallback = all.getEntry[UpscanCsvFilesCallbackList](ersUtil.CHECK_CSV_FILES).getOrElse (
                throw new Exception(s"Cache data missing for key: ${ersUtil.CHECK_CSV_FILES} in CacheMap")
              )
              val csvFilesCallback: List[UpscanCsvFilesCallback] = if(csvCallback.areAllFilesSuccessful()) {
                csvCallback.files.collect{
                  case successfulFile@UpscanCsvFilesCallback(_, _, _: UploadedSuccessfully) => successfulFile
                }
              } else {
                throw new Exception("Not all files have been complete")
              }

              for (file <- csvFilesCallback) {
                filesUploaded += ersUtil.getPageElement(requestObject.getSchemeId, ersUtil.PAGE_CHECK_CSV_FILE, file.fileId + ".file_name")
              }
            } else {
              filesUploaded += all.getEntry[String](ersUtil.FILE_NAME_CACHE).get
            }
          }
          val pdf = pdfBuilderService.createPdf(alldata, Some(filesUploaded), dateSubmitted).toByteArray
          Ok(pdf)
            .as("application/pdf")
            .withHeaders(CONTENT_DISPOSITION -> s"inline; filename=$bundle-confirmation.pdf")
        } recover {
          case e: Throwable =>
            logger.error(s"[PdfGenerationController][generatePdf] Problem fetching file list from cache ${e.getMessage}.", e)
            getGlobalErrorPage
        }
      }.recover {
        case e: Throwable =>
          logger.error(s"[PdfGenerationController][generatePdf] Problem saving Pdf Receipt ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
          getGlobalErrorPage
      }
    }
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
    Ok(globalErrorView(
      "ers.global_errors.title",
      "ers.global_errors.heading",
      "ers.global_errors.message"
    )(request, messages, appConfig))
  }
}
