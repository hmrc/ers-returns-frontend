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

import akka.actor.ActorSystem
import config.ApplicationConfig
import connectors.ErsConnector
import javax.inject.{Inject, Singleton}
import models._
import models.upscan._
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.UpscanService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class CsvFileUploadController @Inject()(val mcc: MessagesControllerComponents,
																				val ersConnector: ErsConnector,
																				val authConnector: DefaultAuthConnector,
																				val upscanService: UpscanService,
																				implicit val ersUtil: ERSUtil,
																				implicit val appConfig: ApplicationConfig,
                                        implicit val actorSystem: ActorSystem,
                                        globalErrorView: views.html.global_error,
                                        upscanCsvFileUploadView: views.html.upscan_csv_file_upload,
                                        fileUploadErrorsView: views.html.file_upload_errors
																			 ) extends FrontendController(mcc) with Authenticator with Retryable with I18nSupport{

  implicit val ec: ExecutionContext = mcc.executionContext

	lazy val allCsvFilesCacheRetryAmount: Int = appConfig.allCsvFilesCacheRetryAmount

  def uploadFilePage(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        (for {
          requestObject   <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
          csvFilesList    <- ersUtil.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD, requestObject.getSchemeReference)
          currentCsvFile  = csvFilesList.ids.find(ids => ids.uploadStatus == NotStarted)
          if currentCsvFile.isDefined
          upscanFormData  <- upscanService.getUpscanFormDataCsv(currentCsvFile.get.uploadId, requestObject.getSchemeReference)
        } yield {
          Ok(upscanCsvFileUploadView(requestObject, upscanFormData, currentCsvFile.get.fileId))
        }) recover {
          case _: NoSuchElementException =>
            Logger.warn(s"[CsvFileUploadController][uploadFilePage] Attempting to load upload page when no files are ready to upload")
            getGlobalErrorPage
          case e: Throwable =>
            Logger.error(s"[CsvFileUploadController][uploadFilePage] Failed to display csv upload page with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
            getGlobalErrorPage
        }
  }

  def success(uploadId: UploadId): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        Logger.info(s"[CsvFileUploadController][success] Upload form submitted for ID: $uploadId")
        (for {
          requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
          csvFileList   <- ersUtil.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD, requestObject.getSchemeReference)
          updatedCacheFileList = {
            Logger.info(s"[CsvFileUploadController][success] Updating uploadId: ${uploadId.value} to InProgress")
            csvFileList.updateToInProgress(uploadId)
          }
          _ <- ersUtil.cache(ersUtil.CSV_FILES_UPLOAD, updatedCacheFileList, requestObject.getSchemeReference)
        } yield {
          if(updatedCacheFileList.noOfFilesToUpload == updatedCacheFileList.noOfUploads) {
            Redirect(routes.CsvFileUploadController.validationResults())
					} else {
            Redirect(routes.CsvFileUploadController.uploadFilePage())
					}
				}) recover {
          case NonFatal(e) =>
            Logger.error(s"[CsvFileUploadController][success] failed to fetch callback data with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
            getGlobalErrorPage
        }
  }

  def validationResults(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        processValidationResults()
  }

  def processValidationResults()(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {

    (for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      all           <- ersUtil.fetch[ErsMetaData](ersUtil.ersMetaData, requestObject.getSchemeReference)
      result        <- removePresubmissionData(all.schemeInfo)
    } yield {
      result
    }) recover {
      case e: Exception =>
        Logger.error(s"[CsvFileUploadController][processValidationResults] Failed to fetch metadata data with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        getGlobalErrorPage
    }
  }

  def removePresubmissionData(schemeInfo: SchemeInfo)(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    ersConnector.removePresubmissionData(schemeInfo).flatMap { result =>
      result.status match {
        case OK => extractCsvCallbackData(schemeInfo)
        case _ =>
          Logger.error(s"[CsvFileUploadController][removePresubmissionData] failed with status ${result.status}, timestamp: ${System.currentTimeMillis()}.")
          Future.successful(getGlobalErrorPage)
      }
    } recover {
      case e: Exception =>
				Logger.error(s"[CsvFileUploadController][removePresubmissionData] Failed to remove presubmission data with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
				getGlobalErrorPage
		}
  }

  def extractCsvCallbackData(schemeInfo: SchemeInfo)(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD, schemeInfo.schemeRef).flatMap {
      data =>
        ersUtil.fetchAll(schemeInfo.schemeRef).map {
          cacheMap =>
            data.ids.foldLeft(Option(List.empty[UpscanCsvFilesCallback])) {
              case(Some(upscanCallbackList), UpscanIds(uploadId, fileId, _)) =>
                cacheMap.getEntry[UploadStatus](s"${ersUtil.CHECK_CSV_FILES}-${uploadId.value}").map { status =>
									UpscanCsvFilesCallback(uploadId, fileId, status):: upscanCallbackList
                }
              case(_, _) => None
            }
        }.withRetry(allCsvFilesCacheRetryAmount)(_.isDefined).flatMap {
          files =>
            val csvFilesCallbackList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(files.get)
            ersUtil.cache(ersUtil.CHECK_CSV_FILES, csvFilesCallbackList, schemeInfo.schemeRef).flatMap { _ =>
              if (csvFilesCallbackList.files.nonEmpty && csvFilesCallbackList.areAllFilesComplete()) {
                if(csvFilesCallbackList.areAllFilesSuccessful()) {
                  val callbackDataList: List[UploadedSuccessfully] =
                    csvFilesCallbackList.files.map(_.uploadStatus.asInstanceOf[UploadedSuccessfully])
                  validateCsv(callbackDataList, schemeInfo)
                } else {
                  val failedFiles: String = csvFilesCallbackList.files.filter(_.uploadStatus == Failed).map(_.uploadId.value).mkString(", ")
                  Logger.error(s"[CsvFileUploadController][extractCsvCallbackData] Validation failed as one or more csv files failed to upload via Upscan. Failure IDs: $failedFiles")
                  Future.successful(getGlobalErrorPage)
                }
              } else {
                Logger.error(s"[CsvFileUploadController][extractCsvCallbackData] Failed to validate as not all csv files have completed upload to upscan. Data: $csvFilesCallbackList")
                Future.successful(getGlobalErrorPage)
              }
            }
        } recover {
          case e: LoopException[_] =>
            Logger.error(s"[CsvFileUploadController][extractCsvCallbackData] Could not fetch all files from cache map. UploadIds: ${data.ids.map(_.uploadId).mkString}", e)
            getGlobalErrorPage
        }
    } recover {
      case e: Exception =>
        Logger.error(s"[CsvFileUploadController][extractCsvCallbackData] Failed to fetch CsvFilesCallbackList with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
        getGlobalErrorPage
    }
  }


  def validateCsv(csvCallbackData: List[UploadedSuccessfully], schemeInfo: SchemeInfo)(implicit authContext: ERSAuthData,
                                                                                       request: Request[AnyRef],
                                                                                       hc: HeaderCarrier): Future[Result] = {
    ersConnector.validateCsvFileData(csvCallbackData, schemeInfo).map { res =>
      res.status match {
        case OK =>
          Logger.warn(s"[CsvFileUploadController][validateCsv] Validation is successful for schemeRef: ${schemeInfo.schemeRef}, " +
						s"callback: ${csvCallbackData.toString}, timestamp: ${System.currentTimeMillis()}.")
          ersUtil.cache(ersUtil.VALIDATED_SHEEETS, res.body, schemeInfo.schemeRef)
          Redirect(routes.SchemeOrganiserController.schemeOrganiserPage())
        case ACCEPTED =>
          Logger.warn(s"[CsvFileUploadController][validateCsv] Validation is not successful for schemeRef: ${schemeInfo.schemeRef}, " +
						s"callback: ${csvCallbackData.toString}, timestamp: ${System.currentTimeMillis()}.")
          Redirect(routes.CsvFileUploadController.validationFailure())
        case _ => Logger.error(s"[CsvFileUploadController][validateCsv] Validate file data failed with Status ${res.status}, timestamp: ${System.currentTimeMillis()}.")
          getGlobalErrorPage
      }
    } recover {
      case e: Exception =>
        Logger.error(s"[CsvFileUploadController][validateCsv] Failed to fetch CsvFilesCallbackList with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        getGlobalErrorPage
    }
  }

  def validationFailure(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
          processValidationFailure()(user, request, hc)
  }

  def processValidationFailure()(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    Logger.info("[CsvFileUploadController][processValidationFailure] Validation Failure: " + (System.currentTimeMillis() / 1000))
    (for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      fileType      <- ersUtil.fetch[CheckFileType](ersUtil.FILE_TYPE_CACHE, requestObject.getSchemeReference)
    } yield {
      Ok(fileUploadErrorsView(requestObject, fileType.checkFileType.get))
    }).recover {
      case e: Exception =>
				Logger.error(s"[CsvFileUploadController][processValidationFailure] failed to save callback data list with exception ${e.getMessage}, " +
					s"timestamp: ${System.currentTimeMillis()}.")
				getGlobalErrorPage
		}
  }

  def failure(): Action[AnyContent] = authorisedForAsync() {
    implicit user =>
      implicit request =>
        val errorCode = request.getQueryString("errorCode").getOrElse("Unknown")
        val errorMessage = request.getQueryString("errorMessage").getOrElse("Unknown")
        val errorRequestId = request.getQueryString("errorRequestId").getOrElse("Unknown")
        Logger.error(s"Upscan Failure. errorCode: $errorCode, errorMessage: $errorMessage, errorRequestId: $errorRequestId")
        Future.successful(getGlobalErrorPage)
  }

	def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		Ok(globalErrorView(
			"ers.global_errors.title",
			"ers.global_errors.heading",
			"ers.global_errors.message"
		)(request, messages, appConfig))
	}
}
