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

package controllers.subsidiaries

import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import controllers.{routes, trustees}
import forms.YesNoFormProvider
import models._
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._
import services.CompanyDetailsService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class GroupSchemeController @Inject()(val mcc: MessagesControllerComponents,
                                      val authConnector: DefaultAuthConnector,
                                      implicit val countryCodes: CountryCodes,
                                      implicit val ersUtil: ERSUtil,
                                      implicit val sessionService: FrontendSessionService,
                                      implicit val companyService: CompanyDetailsService,
                                      implicit val appConfig: ApplicationConfig,
                                      globalErrorView: views.html.global_error,
                                      groupView: views.html.group,
                                      groupPlanSummaryView: views.html.group_plan_summary,
                                      confirmDeleteCompanyView: views.html.confirm_delete_company,
                                      yesNoFormProvider: YesNoFormProvider,
                                      authAction: AuthAction
                                     ) extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging with Constants with CacheHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  private val form: Form[Boolean] = yesNoFormProvider.withPrefix("delete-company")

  private def getRequestObjAndCompanyDetails()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[(RequestObject, Int, CompanyDetailsList)] =
    for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      companyDetailsList <- sessionService.fetchCompaniesOptionally()
      companySize = companyDetailsList.companies.size
    } yield (requestObject, companySize, companyDetailsList)


  def confirmDeleteCompanyPage(id: Int): Action[AnyContent] = authAction.async { implicit request =>
    showConfirmDeleteCompanyPage(id)
  }

  def showConfirmDeleteCompanyPage(id: Int)(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] = {
    getRequestObjAndCompanyDetails() transformWith {
      case _ @ Success((requestObject: RequestObject, companySize: Int, companyDetailsList: CompanyDetailsList)) =>
        Future.successful(Ok(confirmDeleteCompanyView(requestObject, id, form, companySize == 1, companyDetailsList.companies(id).companyName)))
      case Failure(cause) =>
        logger.error(
          s"[GroupSchemeController][showConfirmDeleteCompany] getRequestObjAndCompanyDetails failed, timestamp: ${System.currentTimeMillis()}, error: $cause"
        )
        Future.successful(getGlobalErrorPage)
    } recover { case _: Exception =>
      getGlobalErrorPage
    }
  }

  def confirmDeleteCompanySubmit(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    val requestObjectWithCompanyList = getRequestObjAndCompanyDetails()

    requestObjectWithCompanyList.flatMap { case (requestObject, companySize, companyDetailsList) =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(
          confirmDeleteCompanyView(
            requestObject,
            index,
            formWithErrors,
            companySize == 1,
            companyDetailsList.companies(index).companyName
          )
        )),
        {
          (formSubmissionRadio: Boolean) => {
            if (formSubmissionRadio) {
              val pageToRedirectTo = if (companySize == 1) {
                controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage()
              } else {
                controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage()
              }
              companyService.deleteCompany(companyDetailsList, index).map {
                case true => Redirect(pageToRedirectTo)
                case _    => getGlobalErrorPage
              }
            } else {
              Future.successful(Redirect(controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage()))
            }
          }
        }
      )
    } recover { case _: Exception =>
      logger.error(
        s"[GroupSchemeController][showConfirmDeleteCompany] Fetching companies failed, timestamp: ${System.currentTimeMillis()}."
      )
      getGlobalErrorPage
    }
  }

  def groupSchemePage(): Action[AnyContent] = authAction.async {
    implicit request =>
      sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
        showGroupSchemePage(requestObject)(request)
      }
  }

  def showGroupSchemePage(requestObject: RequestObject)
                         (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    sessionService.fetch[GroupSchemeInfo](ersUtil.GROUP_SCHEME_CACHE_CONTROLLER).map {
      groupSchemeInfo =>
        Ok(
          groupView(
            requestObject,
            groupSchemeInfo.groupScheme,
            RsFormMappings.groupForm().fill(RS_groupScheme(groupSchemeInfo.groupScheme))
          )
        )
    } recover { case _: Exception =>
      val form = RS_groupScheme(Some(ersUtil.DEFAULT))
      Ok(groupView(requestObject, Some(ersUtil.DEFAULT), RsFormMappings.groupForm().fill(form)))
    }

  def groupSchemeSelected(scheme: String): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
      showGroupSchemeSelected(requestObject, scheme)(request)
    }
  }

  def showGroupSchemeSelected(requestObject: RequestObject, scheme: String)
                             (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    RsFormMappings
      .groupForm()
      .bindFromRequest()
      .fold(
        errors => {
          val correctOrder = errors.errors.map(_.key).distinct
          val incorrectOrderGrouped = errors.errors.groupBy(_.key).map(_._2.head).toSeq
          val correctOrderGrouped = correctOrder.flatMap(x => incorrectOrderGrouped.find(_.key == x))
          val firstErrors: Form[models.RS_groupScheme] =
            new Form[RS_groupScheme](errors.mapping, errors.data, correctOrderGrouped, errors.value)
          Future.successful(Ok(groupView(requestObject, Some(""), firstErrors)))
        },
        formData => {
          val gsc: GroupSchemeInfo =
            GroupSchemeInfo(
              Some(formData.groupScheme.getOrElse("")),
              if (formData.groupScheme.contains(ersUtil.OPTION_YES)) Some(ersUtil.OPTION_MANUAL) else None
            )

          sessionService.cache(ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, gsc).map { _ =>
            (requestObject.getSchemeId, formData.groupScheme) match {

              case (_, Some(ersUtil.OPTION_YES)) => Redirect(controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage())

              case (ersUtil.SCHEME_CSOP | ersUtil.SCHEME_SAYE, _) =>
                sessionService.remove(ersUtil.SUBSIDIARY_COMPANIES_CACHE)
                Redirect(routes.AltAmendsController.altActivityPage())

              case (ersUtil.SCHEME_EMI | ersUtil.SCHEME_OTHER, _) =>
                sessionService.remove(ersUtil.SUBSIDIARY_COMPANIES_CACHE)
                Redirect(routes.SummaryDeclarationController.summaryDeclarationPage())

              case (ersUtil.SCHEME_SIP, _) =>
                sessionService.remove(ersUtil.SUBSIDIARY_COMPANIES_CACHE)
                Redirect(controllers.trustees.routes.TrusteeSummaryController.trusteeSummaryPage())

              case (_, _) => getGlobalErrorPage
            }
          }
        }
      )

  def groupPlanSummaryPage(): Action[AnyContent] = authAction.async { implicit request =>
    showGroupPlanSummaryPage()(request)
  }

  def showGroupPlanSummaryPage()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      companyDetailsList <- sessionService.fetchCompaniesOptionally()
    } yield {
      if (companyDetailsList.companies.isEmpty) {
        Redirect(controllers.subsidiaries.routes.SubsidiaryBasedInUkController.questionPage())
      } else {
        Ok(groupPlanSummaryView(requestObject, ersUtil.OPTION_MANUAL, companyDetailsList))
      }

    }) recover { case e: Exception =>
      logger.error(
        s"[GroupSchemeController][showGroupPlanSummaryPage] Fetch group scheme companies before call" +
          s" to group plan summary page failed with exception ${e.getMessage}, " +
          s"timestamp: ${System.currentTimeMillis()}."
      )
      getGlobalErrorPage
    }

  def groupPlanSummaryContinue(scheme: String): Action[AnyContent] = authAction.async { implicit request =>
    continueFromGroupPlanSummaryPage(scheme)
  }

  def continueFromGroupPlanSummaryPage(scheme: String)(implicit request: Request[_]): Future[Result] = {
    RsFormMappings.addSubsidiaryForm().bindFromRequest().fold(
      _ => {
        for {
          requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
          companyDetailsList <- sessionService.fetchCompaniesOptionally()
        } yield {
          BadRequest(groupPlanSummaryView(requestObject, ersUtil.OPTION_MANUAL, companyDetailsList, formHasError = true))
        }
      },
      addCompany => {
        if (addCompany.addCompany) {
          Future.successful(Redirect(controllers.subsidiaries.routes.SubsidiaryBasedInUkController.questionPage()))
        } else {
          scheme match {
            case ersUtil.SCHEME_CSOP | ersUtil.SCHEME_SAYE =>
              Future(Redirect(routes.AltAmendsController.altActivityPage()))

            case ersUtil.SCHEME_EMI | ersUtil.SCHEME_OTHER =>
              Future(Redirect(routes.SummaryDeclarationController.summaryDeclarationPage()))

            case ersUtil.SCHEME_SIP =>
              Future(Redirect(trustees.routes.TrusteeSummaryController.trusteeSummaryPage()))
          }
        }
      }
    )
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
