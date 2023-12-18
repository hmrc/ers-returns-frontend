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

import _root_.models._
import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SummaryDeclarationController @Inject() (val mcc: MessagesControllerComponents,
                                              val ersConnector: ErsConnector,
                                              val sessionService: FrontendSessionService,
                                              globalErrorView: views.html.global_error,
                                              summaryView: views.html.summary,
                                              authAction: AuthAction)
                                             (implicit val ec: ExecutionContext,
                                              val ersUtil: ERSUtil,
                                              val appConfig: ApplicationConfig,
                                              val countryCodes: CountryCodes)
  extends FrontendController(mcc) with I18nSupport with CacheHelper {

  def summaryDeclarationPage(): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
      showSummaryDeclarationPage(requestObject)(request)
    }
  }

  def showSummaryDeclarationPage(requestObject: RequestObject)(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    sessionService.fetchAll().flatMap { all =>
      val schemeOrganiser: SchemeOrganiserDetails =
        getEntry[SchemeOrganiserDetails](all, DataKey(ersUtil.SCHEME_ORGANISER_CACHE)).get
      val groupSchemeInfo: GroupSchemeInfo =
        getEntry[GroupSchemeInfo](all, DataKey(ersUtil.GROUP_SCHEME_CACHE_CONTROLLER)).getOrElse(new GroupSchemeInfo(None, None))
      val groupScheme: String = groupSchemeInfo.groupScheme.getOrElse("")
      val reportableEvents: String = getEntry[ReportableEvents](all, DataKey(ersUtil.REPORTABLE_EVENTS)).get.isNilReturn.get
      var fileType: String = ""
      var fileNames: String = ""
      var fileCount: Int = 0

      if (reportableEvents == ersUtil.OPTION_YES) {
        fileType = getEntry[CheckFileType](all, DataKey(ersUtil.FILE_TYPE_CACHE)).get.checkFileType.get
        if (fileType == ersUtil.OPTION_CSV) {
          val csvCallback = getEntry[UpscanCsvFilesCallbackList](all, DataKey(ersUtil.CHECK_CSV_FILES))
            .getOrElse(
              throw new Exception(s"Cache data missing for key: ${ersUtil.CHECK_CSV_FILES} in CacheMap")
            )
          val csvFilesCallback: List[UpscanCsvFilesCallback] = if (csvCallback.areAllFilesSuccessful()) {
            csvCallback.files.collect { case successfulFile @ UpscanCsvFilesCallback(_, _, _: UploadedSuccessfully) =>
              successfulFile
            }
          } else {
            throw new Exception("Not all files have been complete")
          }

          for (file <- csvFilesCallback) {
            fileNames = fileNames + Messages(
              ersUtil.getPageElement(requestObject.getSchemeId, ersUtil.PAGE_CHECK_CSV_FILE, file.fileId + ".file_name")
            ) + "\n"
            fileCount += 1
          }
        } else {
          fileNames = getEntry[String](all, DataKey(ersUtil.FILE_NAME_CACHE)).get
          fileCount += 1
        }
      }

      val altAmendsActivity =
        getEntry[AltAmendsActivity](all, DataKey(ersUtil.ALT_AMENDS_ACTIVITY)).getOrElse(AltAmendsActivity(""))
      val altActivity       = requestObject.getSchemeId match {
        case ersUtil.SCHEME_CSOP | ersUtil.SCHEME_SIP | ersUtil.SCHEME_SAYE => altAmendsActivity.altActivity
        case _                                                              => ""
      }
      Future(
        Ok(
          summaryView(
            requestObject,
            reportableEvents,
            fileType,
            fileNames,
            fileCount,
            groupScheme,
            schemeOrganiser,
            getCompDetails(all),
            altActivity,
            getAltAmends(all),
            getTrustees(all)
          )
        )
      )
    } recover { case e: Throwable =>
      logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] failed to load page with exception ${e.getMessage}.", e)
      getGlobalErrorPage
    }

  def getTrustees(cacheItem: CacheItem): TrusteeDetailsList =
    getEntry[TrusteeDetailsList](cacheItem, DataKey(ersUtil.TRUSTEES_CACHE))
      .getOrElse(TrusteeDetailsList(List[TrusteeDetails]()))

  def getAltAmends(cacheItem: CacheItem): AlterationAmends =
    getEntry[AlterationAmends](cacheItem, DataKey(ersUtil.ALT_AMENDS_CACHE_CONTROLLER))
      .getOrElse(AlterationAmends(None, None, None, None, None))

  def getCompDetails(cacheItem: CacheItem): CompanyDetailsList =
    getEntry[CompanyDetailsList](cacheItem, DataKey(ersUtil.GROUP_SCHEME_COMPANIES))
      .getOrElse(CompanyDetailsList(List[CompanyDetails]()))

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
