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
import models.upscan.{NotStarted, UploadId, UpscanCsvFilesList, UpscanIds}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckCsvFilesController @Inject() (
  val mcc: MessagesControllerComponents,
  val authConnector: DefaultAuthConnector,
  implicit val ersUtil: ERSUtil,
  implicit val appConfig: ApplicationConfig,
  globalErrorView: views.html.global_error,
  checkCsvFileView: views.html.check_csv_file,
  authAction: AuthAction
) extends FrontendController(mcc)
    with I18nSupport
    with WithUnsafeDefaultFormBinding
    with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def checkCsvFilesPage(): Action[AnyContent] = authAction.async { implicit request =>
    showCheckCsvFilesPage()(request, hc)
  }

  def showCheckCsvFilesPage()(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[Result] = {
    val requestObjectFuture = ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
    ersUtil.remove(ersUtil.CSV_FILES_UPLOAD)
    (for {
      requestObject <- requestObjectFuture
    } yield {
      val csvFilesList: List[CsvFiles] = ersUtil.getCsvFilesList(requestObject.getSchemeType)
      Ok(checkCsvFileView(requestObject, CsvFilesList(csvFilesList)))
    }) recover { case _: Throwable =>
      getGlobalErrorPage
    }
  }

  def checkCsvFilesPageSelected(): Action[AnyContent] = authAction.async { implicit request =>
    validateCsvFilesPageSelected()
  }

  def validateCsvFilesPageSelected()(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[Result] =
    RsFormMappings
      .csvFileCheckForm()
      .bindFromRequest()
      .fold(
        _ => reloadWithError(),
        formData => performCsvFilesPageSelected(formData)
      )

  def performCsvFilesPageSelected(
    formData: CsvFilesList
  )(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    val csvFilesCallbackList: UpscanCsvFilesList = createCacheData(formData.files)
    if (csvFilesCallbackList.ids.isEmpty) {
      reloadWithError()
    } else {
      (for {
        requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
        _             <- ersUtil.cache(ersUtil.CSV_FILES_UPLOAD, csvFilesCallbackList, requestObject.getSchemeReference)
      } yield Redirect(routes.CsvFileUploadController.uploadFilePage())).recover { case e: Throwable =>
        logger.error(
          s"[CheckCsvFilesController][performCsvFilesPageSelected] Save data to cache failed with exception ${e.getMessage}.",
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
