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

import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models._
import models.upscan.{NotStarted, UploadId, UpscanCsvFilesList, UpscanIds}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckCsvFilesController @Inject() (val mcc: MessagesControllerComponents,
                                         val sessionService: FrontendSessionService,
                                         globalErrorView: views.html.global_error,
                                         checkCsvFileView: views.html.check_csv_file,
                                         authAction: AuthAction)
                                        (implicit val ec: ExecutionContext,
                                         val ersUtil: ERSUtil,
                                         val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging {

  def checkCsvFilesPage(): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[ErsMetaData](ersUtil.ERS_METADATA).map { ele =>
      logger.info(s"[CheckCsvFilesController][checkCsvFilesPage()] Fetched request object with SAP Number: ${ele.sapNumber} " +
        s"and schemeRef:${ele.schemeInfo.schemeRef}")
         }
    showCheckCsvFilesPage()(request)
  }

  def showCheckCsvFilesPage()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] = {
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      _ <- sessionService.remove(ersUtil.CSV_FILES_UPLOAD)
    } yield {
      val csvFilesList: List[CsvFiles] = ersUtil.getCsvFilesList(requestObject.getSchemeType)
      Ok(checkCsvFileView(requestObject, CsvFilesList(csvFilesList)))
    }) recover { case e: Throwable =>
      logger.error(s"[CheckCsvFilesController][showCheckCsvFilesPage] Error while CSV file check: ${e.getMessage}, timestamp: ${System.currentTimeMillis()}")
      getGlobalErrorPage
    }
  }

  def checkCsvFilesPageSelected(): Action[AnyContent] = authAction.async { implicit request =>
    validateCsvFilesPageSelected()
  }

  def validateCsvFilesPageSelected()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    RsFormMappings
      .csvFileCheckForm()
      .bindFromRequest()
      .fold(
        _ => reloadWithError(),
        formData => performCsvFilesPageSelected(formData)
      )

  def performCsvFilesPageSelected(formData: CsvFilesList)
                                 (implicit request: Request[_]): Future[Result] = {
    val csvFilesCallbackList: UpscanCsvFilesList = createCacheData(formData.files)
    if (csvFilesCallbackList.ids.isEmpty) {
      reloadWithError()
    } else {
      (for {
        _ <- sessionService.cache(ersUtil.CSV_FILES_UPLOAD, csvFilesCallbackList)
      } yield Redirect(routes.CsvFileUploadController.uploadFilePage())).recover { case e: Throwable =>
        logger.error(
          s"[CheckCsvFilesController][performCsvFilesPageSelected] Save data to cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.",
          e
        )
        getGlobalErrorPage
      }
    }
  }

  def createCacheData(csvFilesList: List[CsvFiles]): UpscanCsvFilesList = {
    val ids = for (fileData <- csvFilesList) yield UpscanIds(UploadId.generate, fileData.fileId, NotStarted)
    UpscanCsvFilesList(ids)
  }

  def reloadWithError()(implicit messages: Messages): Future[Result] =
    Future.successful(
      Redirect(routes.CheckCsvFilesController.checkCsvFilesPage())
        .flashing("csv-file-not-selected-error" -> messages(ersUtil.PAGE_CHECK_CSV_FILE + ".err.message"))
    )

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
