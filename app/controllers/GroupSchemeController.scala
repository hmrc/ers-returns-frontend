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
import forms.YesNoFormProvider
import models._
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{CompanyService, FrontendSessionService}
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupSchemeController @Inject() (val mcc: MessagesControllerComponents,
                                       val sessionService: FrontendSessionService,
                                       val companyService: CompanyService,
                                       globalErrorView: views.html.global_error,
                                       groupView: views.html.group,
                                       manualCompanyDetailsView: views.html.manual_company_details,
                                       groupPlanSummaryView: views.html.group_plan_summary,
                                       confirmDeleteCompanyView: views.html.confirm_delete_company,
                                       yesNoFormProvider: YesNoFormProvider,
                                       authAction: AuthAction)
                                      (implicit val ec: ExecutionContext,
                                       val ersUtil: ERSUtil,
                                       val appConfig: ApplicationConfig,
                                       val countryCodes: CountryCodes)
  extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging with Constants with CacheHelper {

  private val form: Form[Boolean] = yesNoFormProvider.withPrefix("delete-company")

  def manualCompanyDetailsPage(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    showManualCompanyDetailsPage(index)(request)
  }

  def showManualCompanyDetailsPage(index: Int)
                                  (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).map { requestObject =>
      Ok(manualCompanyDetailsView(requestObject, index, RsFormMappings.companyDetailsForm()))
    }

  def manualCompanyDetailsSubmit(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
      showManualCompanyDetailsSubmit(requestObject, index)(request)
    }
  }

  def showManualCompanyDetailsSubmit(requestObject: RequestObject, index: Int)
                                    (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    RsFormMappings
      .companyDetailsForm()
      .bindFromRequest()
      .fold(
        errors => Future(Ok(manualCompanyDetailsView(requestObject, index, errors))),
        successful =>
          sessionService.fetch[CompanyDetailsList](ersUtil.GROUP_SCHEME_COMPANIES).flatMap {
            cachedCompaniesList =>
              val processedFormData =
                CompanyDetailsList(companyService.replaceCompany(cachedCompaniesList.companies, index, successful))

              sessionService.cache(ersUtil.GROUP_SCHEME_COMPANIES, processedFormData).map {
                _ =>
                  Redirect(routes.GroupSchemeController.groupPlanSummaryPage())
              }
          } recoverWith { case _: NoSuchElementException =>
            val companiesList = CompanyDetailsList(List(successful))
            sessionService.cache(ersUtil.GROUP_SCHEME_COMPANIES, companiesList).map { _ =>
              Redirect(routes.GroupSchemeController.groupPlanSummaryPage())
            }
          }
      )

  def confirmDeleteCompanyPage(id: Int): Action[AnyContent] = authAction.async { implicit request =>
    showConfirmDeleteCompany(id)
  }

  def showConfirmDeleteCompany(id: Int)(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    (for {
      all             <- sessionService.fetchAll()
      requestObject   <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      companies       = getEntry[CompanyDetailsList](all, DataKey(ersUtil.GROUP_SCHEME_COMPANIES)).getOrElse(CompanyDetailsList(Nil))
      companyLength   = companies.companies.length
    } yield {
      Ok(confirmDeleteCompanyView(requestObject, id, form, companyLength == 1, companies.companies(id).companyName))
    }) recover { case e: Exception =>
      logger.error(
        s"[GroupSchemeController][showConfirmDeleteCompany] Fetch all data failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}."
      )
      getGlobalErrorPage()
    }

  def confirmDeleteCompanySubmit(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    val requestObjectWithCompanyList = for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      companyDetailsList <- sessionService.fetchCompaniesOptionally()
      companySize = companyDetailsList.companies.size
    } yield (requestObject, companySize, companyDetailsList)

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
          case true if companySize == 1 =>
            companyService.deleteCompany(index).map {
              case true => Redirect(routes.GroupSchemeController.groupSchemePage())
              case _    => getGlobalErrorPage()
            }
          case true =>
            companyService.deleteCompany(index).map {
              case true => Redirect(routes.GroupSchemeController.groupPlanSummaryPage())
              case _    => getGlobalErrorPage()
            }
          case _ => Future.successful(Redirect(routes.GroupSchemeController.groupPlanSummaryPage()))
        }
      )
    }
  }

  def editCompany(id: Int): Action[AnyContent] = authAction.async { implicit request =>
    showEditCompany(id)(request)
  }

  def showEditCompany(id: Int)(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    (for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      all <- sessionService.fetchAll()
      companies = getEntry[CompanyDetailsList](all, DataKey(ersUtil.GROUP_SCHEME_COMPANIES)).getOrElse(CompanyDetailsList(Nil))
      companyDetails = companies.companies(id)
    } yield Ok(
      manualCompanyDetailsView(requestObject, id, RsFormMappings.companyDetailsForm().fill(companyDetails))
    )) recover { case e: Exception =>
      logger.error(s"[GroupSchemeController][showEditCompany] Fetch group scheme companies for edit failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
      getGlobalErrorPage()
    }

  def groupSchemePage(): Action[AnyContent] = authAction.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).flatMap { requestObject =>
      showGroupSchemePage(requestObject)(request)
    }
  }

  def showGroupSchemePage(requestObject: RequestObject)(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
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
          val correctOrder                             = errors.errors.map(_.key).distinct
          val incorrectOrderGrouped                    = errors.errors.groupBy(_.key).map(_._2.head).toSeq
          val correctOrderGrouped                      = correctOrder.flatMap(x => incorrectOrderGrouped.find(_.key == x))
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
              case (_, Some(ersUtil.OPTION_YES)) => Redirect(routes.GroupSchemeController.manualCompanyDetailsPage())
              case (ersUtil.SCHEME_CSOP | ersUtil.SCHEME_SAYE, _) =>
                sessionService.remove(ersUtil.GROUP_SCHEME_COMPANIES)
                Redirect(routes.AltAmendsController.altActivityPage())
              case (ersUtil.SCHEME_EMI | ersUtil.SCHEME_OTHER, _) =>
                sessionService.remove(ersUtil.GROUP_SCHEME_COMPANIES)
                Redirect(routes.SummaryDeclarationController.summaryDeclarationPage())
              case (ersUtil.SCHEME_SIP, _) =>
                sessionService.remove(ersUtil.GROUP_SCHEME_COMPANIES)
                Redirect(controllers.trustees.routes.TrusteeSummaryController.trusteeSummaryPage())
              case (_, _) => getGlobalErrorPage()
            }
          }
        }
      )

  def groupPlanSummaryPage(): Action[AnyContent] = authAction.async { implicit request =>
    showGroupPlanSummaryPage()(request)
  }

  def showGroupPlanSummaryPage()(implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[Result] =
    (for {
      requestObject <- sessionService.fetch[RequestObject](ERS_REQUEST_OBJECT)
      compDetails   <- sessionService.fetch[CompanyDetailsList](GROUP_SCHEME_COMPANIES)
    } yield {
      compDetails.companies match {
        case Nil  =>
          sessionService.cache(ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, GroupSchemeInfo(None, None))
          logger.error("[GroupSchemeController][showGroupPlanSummaryPage] Attempted to route to group summary with no companies.")
          getGlobalErrorPage()
        case _    => Ok(groupPlanSummaryView(requestObject, ersUtil.OPTION_MANUAL, compDetails))
      }
    }) recover { case e: Exception =>
      logger.error(s"[GroupSchemeController][showGroupPlanSummaryPage] Get data from cache failed with exception", e)
      getGlobalErrorPage()
    }

  def groupPlanSummaryContinue(scheme: String): Action[AnyContent] = authAction.async {
    continueFromGroupPlanSummaryPage(scheme)
  }

  def continueFromGroupPlanSummaryPage(scheme: String): Future[Result] =
    scheme match {
      case ersUtil.SCHEME_CSOP | ersUtil.SCHEME_SAYE =>
        Future(Redirect(routes.AltAmendsController.altActivityPage()))

      case ersUtil.SCHEME_EMI | ersUtil.SCHEME_OTHER =>
        Future(Redirect(routes.SummaryDeclarationController.summaryDeclarationPage()))

      case ersUtil.SCHEME_SIP =>
        Future(Redirect(trustees.routes.TrusteeSummaryController.trusteeSummaryPage()))

    }

  def getGlobalErrorPage(status: Status = InternalServerError)(implicit request: Request[_], messages: Messages): Result =
    status(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
