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

package controllers.schemeOrganiser

import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models._
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SchemeOrganiserController @Inject()(
                                           val mcc: MessagesControllerComponents,
                                           val authConnector: DefaultAuthConnector,
                                           implicit val countryCodes: CountryCodes,
                                           implicit val ersUtil: ERSUtil,
                                           implicit val appConfig: ApplicationConfig,
                                           globalErrorView: views.html.global_error,
                                           schemeOrganiserView: views.html.scheme_organiser,
                                           schemeOrganiserSummaryView: views.html.scheme_organiser_summary,
                                           authAction: AuthAction
                                         ) extends FrontendController(mcc)
  with I18nSupport
  with WithUnsafeDefaultFormBinding
  with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def schemeOrganiserPage(): Action[AnyContent] = authAction.async { implicit request =>
    ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
      showSchemeOrganiserPage(requestObject)(request, hc)
    }
  }

  def showSchemeOrganiserPage(requestObject: RequestObject)
                             (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    logger.info(s"[SchemeOrganiserController][showSchemeOrganiserPage] schemeRef: ${requestObject.getSchemeReference}.")

    (for {
      reportableEvent <- ersUtil.fetch[ReportableEvents](ersUtil.reportableEvents, requestObject.getSchemeReference)
      fileType <- ersUtil.actuallyFetchOption[CheckFileType](ersUtil.FILE_TYPE_CACHE, requestObject.getSchemeReference)
      res <- ersUtil.actuallyFetchOption[SchemeOrganiserDetails](ersUtil.SCHEME_ORGANISER_CACHE, requestObject.getSchemeReference)
    } yield {
      val form = res.fold(RsFormMappings.schemeOrganiserForm())(RsFormMappings.schemeOrganiserForm().fill(_))
      Ok(
        schemeOrganiserView(
          requestObject,
          fileType.fold("")(_.checkFileType.get),
          form,
          reportableEvent.isNilReturn.get
        )
      )
    }) recover {
      case e: Exception =>
        logger.error(
          s"[SchemeOrganiserController][showSchemeOrganiserPage] Get reportableEvent.isNilReturn failed with exception ${e.getMessage}," +
            s" timestamp: ${System.currentTimeMillis()}."
        )
        getGlobalErrorPage
    }
  }

  def schemeOrganiserSubmit(): Action[AnyContent] = authAction.async { implicit request =>
    println("surely we arrive here pausechamp")
    ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
      showSchemeOrganiserSubmit(requestObject)(request, hc)
    }
  }

  def showSchemeOrganiserSubmit(requestObject: RequestObject)
                               (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    println("weee wooo weee wooo")
    Future.successful(Redirect(controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage()))
  }

  def schemeOrganiserSummaryPage: Action[AnyContent] = authAction.async {
    implicit request =>
      showschemeOrganiserSummaryPage(request, hc)
  }

  def showschemeOrganiserSummaryPage(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    (for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      companyDetails <- ersUtil.fetch[CompanyDetails](ersUtil.SCHEME_ORGANISER_CACHE, requestObject.getSchemeReference)
    } yield {
      println(companyDetails)
      Ok(schemeOrganiserSummaryView(requestObject, companyDetails))
    }) recover {
      case e: Exception =>
        logger.error(s"[SchemeOrganiserController][showManualCompanyDetailsPage] Get data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        getGlobalErrorPage
    }
  }

  def companySummaryContinue(): Action[AnyContent] = authAction.async {
    implicit request =>
      continueFromCompanySummaryPage()(request, hc)
  }

  def continueFromCompanySummaryPage()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    Future(Redirect(controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage()))
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
