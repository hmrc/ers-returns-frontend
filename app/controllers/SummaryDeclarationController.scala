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

      val (fileNames: String, fileCount: Int) = if (reportableEvents == ersUtil.OPTION_YES) {
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

          val successfulUploadNames: List[String] = csvFilesCallback.collect {
            case UpscanCsvFilesCallback(_, _, status: UploadedSuccessfully) => status.name
          }

          (successfulUploadNames.mkString("\n"), successfulUploadNames.length)
        } else {
          (getEntry[String](all, DataKey(ersUtil.FILE_NAME_CACHE)).get, 1)
        }
      } else ("", 0)

      val schemeID = requestObject.getSchemeId
      val altAmendsActivity =
        getEntry[AltAmendsActivity](all, DataKey(ersUtil.ALT_AMENDS_ACTIVITY)).getOrElse(AltAmendsActivity(""))
      val altActivity       = schemeID match {
        case ersUtil.SCHEME_CSOP | ersUtil.SCHEME_SIP | ersUtil.SCHEME_SAYE => altAmendsActivity.altActivity
        case _ => ""
      }

      if (validateCompanies(all, groupScheme) && validateAltAmends(all, altActivity, schemeID)) {
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
      } else {
        throw new Exception("Validation of companies or alt activities failed")
      }
    } recover { case e: Throwable =>
      logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] failed to load page with exception ${e.getMessage}.", e)
      getGlobalErrorPage
    }

  def validateCompanies(all: CacheItem, groupScheme: String): Boolean = {
    if ((groupScheme == ersUtil.OPTION_YES) || (groupScheme == ersUtil.OPTION_NO)) {
      getCompDetails(all).companies match {
        case Nil if groupScheme == ersUtil.OPTION_YES =>
          logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] attempted to route to summary page with a group scheme + no companies submitted.")
          false
        case List(_, _*) if groupScheme == ersUtil.OPTION_NO =>
          logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] attempted to route to summary page with no group scheme + a filled company list.")
          false
        case _ => true
      }
    } else {
      logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] attempted to route to summary page with empty group scheme.")
      false
    }
  }

  def validateAltAmends(all: CacheItem, altActivity: String, schemeID: String): Boolean = {
    val altActivityCheck = schemeID match {
      case ersUtil.SCHEME_CSOP | ersUtil.SCHEME_SIP | ersUtil.SCHEME_SAYE => true
      case _ => false
    }

    if (altActivityCheck) {
      emptyAltAmends(getAltAmends(all)) match {
        case true if altActivity == ersUtil.OPTION_YES =>
          logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] attempted to route to summary page with empty alt amends + alt activity selected.")
          false
        case false if altActivity == ersUtil.OPTION_NO =>
          logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] attempted to route to summary page with alt amends + 'No' alt activity selected.")
          false
        case false if altActivity == "" =>
          logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] attempted to route to summary page with alt activity page skipped + alt amends filled.")
          false
        case _ => true
      }
    } else if (!emptyAltAmends(getAltAmends(all)) || altActivity == ersUtil.OPTION_YES) {
      logger.error(s"[SummaryDeclarationController][showSummaryDeclarationPage] attempted to route to summary page with alt amends/activity for non compliant scheme.")
      false
    } else {
      true
    }
  }

  def getTrustees(cacheItem: CacheItem): TrusteeDetailsList =
    getEntry[TrusteeDetailsList](cacheItem, DataKey(ersUtil.TRUSTEES_CACHE))
      .getOrElse(TrusteeDetailsList(List[TrusteeDetails]()))

  def getAltAmends(cacheItem: CacheItem): AlterationAmends =
    getEntry[AlterationAmends](cacheItem, DataKey(ersUtil.ALT_AMENDS_CACHE_CONTROLLER))
      .getOrElse(AlterationAmends(None, None, None, None, None))

  def getCompDetails(cacheItem: CacheItem): CompanyDetailsList =
    getEntry[CompanyDetailsList](cacheItem, DataKey(ersUtil.SUBSIDIARY_COMPANIES_CACHE))
      .getOrElse(CompanyDetailsList(List[CompanyDetails]()))

  def emptyAltAmends(alterationAmends: AlterationAmends): Boolean =
    alterationAmends match {
      case AlterationAmends(None, None, None, None, None) => true
      case _                                              => false
    }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    InternalServerError(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
