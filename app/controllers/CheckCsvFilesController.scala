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

import config.ApplicationConfig
import javax.inject.{Inject, Singleton}
import models._
import models.upscan.{NotStarted, UploadId, UpscanCsvFilesList, UpscanIds}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.ERSUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CheckCsvFilesController @Inject()(val messagesApi: MessagesApi,
																				val authConnector: DefaultAuthConnector,
																				implicit val ersUtil: ERSUtil,
																				implicit val appConfig: ApplicationConfig
																			 ) extends FrontendController with Authenticator with I18nSupport {

  def checkCsvFilesPage(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        showCheckCsvFilesPage()(user, request, hc)
  }

  def showCheckCsvFilesPage()(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    val requestObjectFuture = ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
    ersUtil.remove(ersUtil.CSV_FILES_UPLOAD)
    (for {
      requestObject <- requestObjectFuture
    } yield {
      val csvFilesList: List[CsvFiles] = ersUtil.getCsvFilesList(requestObject.getSchemeType)
      Ok(views.html.check_csv_file(requestObject, CsvFilesList(csvFilesList)))
    }) recover {
      case _: Throwable => getGlobalErrorPage
    }
  }

  def checkCsvFilesPageSelected(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        validateCsvFilesPageSelected()
  }

  def validateCsvFilesPageSelected()(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    RsFormMappings.csvFileCheckForm.bindFromRequest.fold(
      _ =>
        reloadWithError(),
      formData =>
        performCsvFilesPageSelected(formData)
    )
  }

  def performCsvFilesPageSelected(formData: CsvFilesList)(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    val csvFilesCallbackList: UpscanCsvFilesList = createCacheData(formData.files)
    if(csvFilesCallbackList.ids.isEmpty) {
      reloadWithError()
    } else {
      (for{
        requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
        _             <- ersUtil.cache(ersUtil.CSV_FILES_UPLOAD, csvFilesCallbackList, requestObject.getSchemeReference)
      } yield {
        Redirect(routes.CsvFileUploadController.uploadFilePage())
      }).recover {
        case e: Throwable =>
          Logger.error(s"[CheckCsvFilesController][performCsvFilesPageSelected] Save data to cache failed with exception ${e.getMessage}.", e)
          getGlobalErrorPage
      }
    }
  }

  def createCacheData(csvFilesList: List[CsvFiles]): UpscanCsvFilesList = {
    val ids = for(fileData <- csvFilesList if fileData.isSelected.contains(ersUtil.OPTION_YES)) yield {
      UpscanIds(UploadId.generate, fileData.fileId, NotStarted)
    }
    UpscanCsvFilesList(ids)
  }

  def reloadWithError()(implicit messages: Messages): Future[Result] = {
    Future.successful(
      Redirect(routes.CheckCsvFilesController.checkCsvFilesPage())
				.flashing("csv-file-not-selected-error" -> messages(ersUtil.PAGE_CHECK_CSV_FILE + ".err.message"))
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
