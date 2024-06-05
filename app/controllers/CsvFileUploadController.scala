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
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models._
import models.upscan._
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{FrontendSessionService, UpscanService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class CsvFileUploadController @Inject() (val mcc: MessagesControllerComponents,
                                         val ersConnector: ErsConnector,
                                         val upscanService: UpscanService,
                                         val sessionService: FrontendSessionService,
                                         globalErrorView: views.html.global_error,
                                         upscanCsvFileUploadView: views.html.upscan_csv_file_upload,
                                         fileUploadErrorsView: views.html.file_upload_errors,
                                         fileUploadProblemView: views.html.file_upload_problem,
                                         authAction: AuthAction)
                                        (implicit val ec: ExecutionContext,
                                         val ersUtil: ERSUtil,
                                         val appConfig: ApplicationConfig,
                                         val actorSystem: ActorSystem)
  extends FrontendController(mcc) with Retryable with I18nSupport with Logging with CacheHelper {

  lazy val allCsvFilesCacheRetryAmount: Int = appConfig.allCsvFilesCacheRetryAmount

  def uploadFilePage(): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      requestObject  <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      csvFilesList   <- sessionService.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD)
      currentCsvFile  = csvFilesList.ids.find(ids => ids.uploadStatus == NotStarted)
      if currentCsvFile.isDefined
      upscanFormData <- upscanService.getUpscanFormDataCsv(currentCsvFile.get.uploadId, requestObject.getSchemeReference)
    } yield {
      Ok(upscanCsvFileUploadView(requestObject, upscanFormData, currentCsvFile.get.fileId, useCsopV5Templates(requestObject.taxYear)))
    }).recover {
      case _: NoSuchElementException =>
        logger.warn(s"[CsvFileUploadController][uploadFilePage] Attempting to load upload page when no files are ready to upload")
        getGlobalErrorPage
      case e: Throwable =>
        logger.error(s"[CsvFileUploadController][uploadFilePage] Failed to display csv upload page with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
        getGlobalErrorPage
    }
  }

  private def useCsopV5Templates(taxYear: Option[String]): Boolean = taxYear match {
    case Some(year) => appConfig.csopV5Enabled && year.split("/")(0).toInt >= 2023
    case None if appConfig.csopV5Enabled => true
    case _ => false
  }


  def success(uploadId: UploadId): Action[AnyContent] = authAction.async { implicit request =>
    logger.info(s"[CsvFileUploadController][success] Upload form submitted for ID: $uploadId")
    (for {
      csvFileList <- sessionService.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD)
      updatedCacheFileList = {
        logger.info(s"[CsvFileUploadController][success] Updating uploadId: ${uploadId.value} to InProgress")
        csvFileList.updateToInProgress(uploadId)
      }
      _ <- sessionService.cache(ersUtil.CSV_FILES_UPLOAD, updatedCacheFileList)
    } yield
      if (updatedCacheFileList.noOfFilesToUpload == updatedCacheFileList.noOfUploads) {
        Redirect(controllers.routes.CsvFileUploadController.validationResults())
      } else {
        Redirect(controllers.routes.CsvFileUploadController.uploadFilePage())
      }) recover { case NonFatal(e) =>
      logger.error(s"[CsvFileUploadController][success] failed to fetch callback data with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
      getGlobalErrorPage
    }
  }

  def validationResults(): Action[AnyContent] = authAction.async { implicit request =>
    processValidationResults()
  }

  def processValidationResults()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] =
    (for {
      all <- sessionService.fetch[ErsMetaData](ersUtil.ERS_METADATA)
      result <- removePresubmissionData(all.schemeInfo)
    } yield result) recover { case e: Exception =>
      logger.error(s"[CsvFileUploadController][processValidationResults] Failed to fetch metadata data with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
      getGlobalErrorPage
    }

  def removePresubmissionData(schemeInfo: SchemeInfo)(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] =
    ersConnector.removePresubmissionData(schemeInfo).flatMap { result =>
      result.status match {
        case OK => extractCsvCallbackData(schemeInfo)
        case _  =>
          logger.error(s"[CsvFileUploadController][removePresubmissionData] failed with status ${result.status}, timestamp: ${System.currentTimeMillis()}.")
          Future.successful(getGlobalErrorPage)
      }
    } recover { case e: Exception =>
      logger.error(s"[CsvFileUploadController][removePresubmissionData] Failed to remove presubmission data with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
      getGlobalErrorPage
    }

  def extractCsvCallbackData(schemeInfo: SchemeInfo)(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] =
    sessionService.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD).flatMap { data =>
      sessionService
        .fetchAll()
        .map { cacheItem =>
          data.ids.foldLeft(Option(List.empty[UpscanCsvFilesCallback])) {
            case (Some(upscanCallbackList), UpscanIds(uploadId, fileId, _)) =>
              getEntry[UploadStatus](cacheItem, DataKey(s"${ersUtil.CHECK_CSV_FILES}-${uploadId.value}")).map { status =>
                UpscanCsvFilesCallback(uploadId, fileId, status) :: upscanCallbackList
              }
            case (_, _) => None
          }
        }
        .withRetry(allCsvFilesCacheRetryAmount)(_.isDefined)
        .flatMap { files =>
          val csvFilesCallbackList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(files.get)
          sessionService.cache(ersUtil.CHECK_CSV_FILES, csvFilesCallbackList).flatMap { _ =>
            if (csvFilesCallbackList.files.nonEmpty && csvFilesCallbackList.areAllFilesComplete()) {
              if (csvFilesCallbackList.areAllFilesSuccessful()) {
                val callbackDataList: List[UploadedSuccessfully] =
                  csvFilesCallbackList.files.map(_.uploadStatus.asInstanceOf[UploadedSuccessfully])
                checkFileNames(callbackDataList, schemeInfo)
              } else {
                val failedFiles: String =
                  csvFilesCallbackList.files.filter(_.uploadStatus == Failed).map(_.uploadId.value).mkString(", ")
                logger.error(s"[CsvFileUploadController][extractCsvCallbackData] Validation failed as one or more csv files failed to upload via Upscan. Failure IDs: $failedFiles")
                Future.successful(getGlobalErrorPage)
              }
            } else {
              logger.error(s"[CsvFileUploadController][extractCsvCallbackData] Failed to validate as not all csv files have completed upload to upscan. Data: $csvFilesCallbackList")
              Future.successful(getGlobalErrorPage)
            }
          }
        } recover { case e: LoopException[_] =>
        logger.error(s"[CsvFileUploadController][extractCsvCallbackData] Could not fetch all files from cache map. UploadIds: ${data.ids.map(_.uploadId).mkString}", e)
        getGlobalErrorPage
      }
    } recover { case e: Exception =>
      logger.error(s"[CsvFileUploadController][extractCsvCallbackData] Failed to fetch CsvFilesCallbackList with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
      getGlobalErrorPage
    }

  def checkFileNames(csvCallbackData: List[UploadedSuccessfully], schemeInfo: SchemeInfo)
                    (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] =
    sessionService.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD).flatMap { list =>
      val uploadedWithCorrectName: Boolean = list.ids
        .map(expectedFile => expectedFile.fileId)
        .zip(
          csvCallbackData.reverse
            .map(uploadedFile => uploadedFile.name)
        )
        .map { names =>
          val expectedName =
            ersUtil.getPageElement(schemeInfo.schemeId, ersUtil.PAGE_CHECK_CSV_FILE, names._1 + ".file_name")
          val expectedNameCsopV5 =
            ersUtil.getPageElement(schemeInfo.schemeId, ersUtil.PAGE_CHECK_CSV_FILE, names._1 + ".file_name.v5")
          val uploadedName = names._2
          if (schemeInfo.schemeType == "CSOP" && uploadedName.contains("V5")) {
            (expectedNameCsopV5, uploadedName)
          } else {
            (expectedName, uploadedName)
          }
        }
        .forall(names => names._1 == names._2)
      if (uploadedWithCorrectName) {
        validateCsv(csvCallbackData, schemeInfo) //HERE IS CALL TO FILE-VALIDATOR
      } else {
        logger.info(s"[CsvFileUploadController][checkFileNames] User uploaded the wrong file")
        Future(getFileUploadProblemPage())
      }
    } recover { case e: Exception =>
      logger.error(s"[CsvFileUploadController][checkFileNames] Failed to fetch CsvFilesCallbackList with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
      getGlobalErrorPage
    }

  def validateCsv(csvCallbackData: List[UploadedSuccessfully], schemeInfo: SchemeInfo)
                 (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] =
    ersConnector.validateCsvFileData(csvCallbackData, schemeInfo).flatMap { res =>
      res.status match {
        case OK       =>
          logger.info(
            s"[CsvFileUploadController][validateCsv] Validation is successful for schemeRef: ${schemeInfo.schemeRef}, " +
              s"timestamp: ${System.currentTimeMillis()}."
          )
          sessionService.cache(ersUtil.VALIDATED_SHEETS, res.body)
          Future.successful(Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserBasedInUkController.questionPage()))
        case ACCEPTED =>
          logger.warn(s"[CsvFileUploadController][validateCsv] Validation is not successful for schemeRef: ${schemeInfo.schemeRef}, " +
              s"timestamp: ${System.currentTimeMillis()}.")
          Future.successful(Redirect(controllers.routes.CsvFileUploadController.validationFailure()))
        case _ =>
          logger.error(s"[CsvFileUploadController][validateCsv] Validate file data failed with Status ${res.status}, timestamp: ${System.currentTimeMillis()}.")
          Future.successful(getGlobalErrorPage)
      }
    } recover { case e: Exception =>
      logger.error(s"[CsvFileUploadController][validateCsv] Failed to fetch CsvFilesCallbackList with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
      getGlobalErrorPage
    }

  def validationFailure(): Action[AnyContent] = authAction.async { implicit request =>
    processValidationFailure()(request)
  }

  def processValidationFailure()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] = {
    logger.info("[CsvFileUploadController][processValidationFailure] Validation Failure: " + (System.currentTimeMillis() / 1000))
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      fileType      <- sessionService.fetch[CheckFileType](ersUtil.FILE_TYPE_CACHE)
    } yield {
      Ok(fileUploadErrorsView(requestObject, fileType.checkFileType.getOrElse("")))
    }).recover {
      case e: Exception =>
        logger.error(s"[CsvFileUploadController][processValidationFailure] failed to save callback data list with exception ${e.getMessage}, " +
            s"timestamp: ${System.currentTimeMillis()}.")
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
