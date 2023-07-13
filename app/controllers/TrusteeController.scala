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
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import models._
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrusteeController @Inject() (
  val mcc: MessagesControllerComponents,
  val authConnector: DefaultAuthConnector,
  val ersConnector: ErsConnector,
  implicit val countryCodes: CountryCodes,
  implicit val ersUtil: ERSUtil,
  implicit val appConfig: ApplicationConfig,
  globalErrorView: views.html.global_error,
  trusteeDetailsView: views.html.trustee_details,
  trusteeSummaryView: views.html.trustee_summary,
  authAction: AuthAction
) extends FrontendController(mcc)
    with I18nSupport
    with WithUnsafeDefaultFormBinding
    with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

/*
  def trusteeDetailsPage(index: Int): Action[AnyContent] = authAction.async {
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
          showTrusteeDetailsPage(requestObject, index)(request, hc)
        }
  }

  def showTrusteeDetailsPage(requestObject: RequestObject, index: Int)
														(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetch[GroupSchemeInfo](ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, requestObject.getSchemeReference).map { groupSchemeActivity =>
      Ok(trusteeDetailsView(requestObject, groupSchemeActivity.groupScheme.getOrElse(ersUtil.DEFAULT), index, RsFormMappings.trusteeDetailsForm))
    } recover {
      case e: Exception =>
				logger.error(s"[TrusteeController][showTrusteeDetailsPage] Get data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
				getGlobalErrorPage
		}
  }
  def trusteeDetailsSubmit(index: Int): Action[AnyContent] = authAction.async {
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
          showTrusteeDetailsSubmit(requestObject, index)(request, hc)
        }
  }

  def showTrusteeDetailsSubmit(requestObject: RequestObject, index: Int)(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[Result] =
    RsFormMappings
      .trusteeDetailsForm()
      .bindFromRequest()
      .fold(
        errors =>
          ersUtil.fetch[GroupSchemeInfo](ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, requestObject.getSchemeReference).map {
            groupSchemeActivity =>
              val correctOrder                             = errors.errors.map(_.key).distinct
              val incorrectOrderGrouped                    = errors.errors.groupBy(_.key).map(_._2.head).toSeq
              val correctOrderGrouped                      = correctOrder.flatMap(x => incorrectOrderGrouped.find(_.key == x))
              val firstErrors: Form[models.TrusteeDetails] =
                new Form[TrusteeDetails](errors.mapping, errors.data, correctOrderGrouped, errors.value)
              Ok(
                trusteeDetailsView(
                  requestObject,
                  groupSchemeActivity.groupScheme.getOrElse(ersUtil.DEFAULT),
                  index,
                  firstErrors
                )
              )
          } recover { case e: Exception =>
            logger.error(
              s"[TrusteeController][showTrusteeDetailsSubmit] Get data from cache failed with exception ${e.getMessage}, " +
                s"timestamp: ${System.currentTimeMillis()}."
            )
            getGlobalErrorPage
          },
        formData =>
          ersUtil.fetch[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, requestObject.getSchemeReference).flatMap {
            cachedTrusteeList =>
              val processedFormData = TrusteeDetailsList(replaceTrustee(cachedTrusteeList.trustees, index, formData))

              ersUtil.cache(ersUtil.TRUSTEES_CACHE, processedFormData, requestObject.getSchemeReference).map { _ =>
                Redirect(routes.TrusteeController.trusteeSummaryPage())
              }

          } recoverWith { case _: Exception =>
            val trusteeList = TrusteeDetailsList(List(formData))
            ersUtil.cache(ersUtil.TRUSTEES_CACHE, trusteeList, requestObject.getSchemeReference).map { _ =>
              Redirect(routes.TrusteeController.trusteeSummaryPage())
            }
          }
      )

 */

  def replaceTrustee(trustees: List[TrusteeDetails], index: Int, formData: TrusteeDetails): List[TrusteeDetails] =
    (if (index == 10000) {
       trustees :+ formData
     } else {
       trustees.zipWithIndex.map { case (a, b) =>
         if (b == index) formData else a
       }
     }).distinct

  def deleteTrustee(id: Int): Action[AnyContent] = authAction.async { implicit request =>
    showDeleteTrustee(id)(request, hc)
  }
  def showDeleteTrustee(id: Int)(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {

    (for {
      requestObject     <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      cachedTrusteeList <- ersUtil.fetch[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, requestObject.getSchemeReference)
      trusteeDetailsList = TrusteeDetailsList(filterDeletedTrustee(cachedTrusteeList, id))
      _                 <- ersUtil.cache(ersUtil.TRUSTEES_CACHE, trusteeDetailsList, requestObject.getSchemeReference)
    } yield Redirect(routes.TrusteeController.trusteeSummaryPage())) recover { case _: Exception =>
      getGlobalErrorPage
    }
  }

  private def filterDeletedTrustee(trusteeDetailsList: TrusteeDetailsList, id: Int): List[TrusteeDetails] =
    trusteeDetailsList.trustees.zipWithIndex.filterNot(_._2 == id).map(_._1)

/*
  def editTrustee(id: Int): Action[AnyContent] = authAction.async {
      implicit request =>
          showEditTrustee(id)(request, hc)
  }
  def showEditTrustee(id: Int)(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    (for {
      requestObject       <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      groupSchemeActivity <-
        ersUtil.fetch[GroupSchemeInfo](ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, requestObject.getSchemeReference)
      trusteeDetailsList  <- ersUtil.fetch[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, requestObject.getSchemeReference)
      formDetails          = trusteeDetailsList.trustees(id)
    } yield Ok(
      trusteeDetailsView(
        requestObject,
        groupSchemeActivity.groupScheme.get,
        id,
        RsFormMappings.trusteeDetailsForm().fill(formDetails)
      )
    )) recover { case e: Exception =>
      logger.error(
        s"[TrusteeController][showEditTrustee] Get data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}."
      )
      getGlobalErrorPage
    }

 */

  def trusteeSummaryPage(): Action[AnyContent] = authAction.async {
      implicit request =>
          showTrusteeSummaryPage()(request, hc)
  }

  def showTrusteeSummaryPage()(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[Result] =
    (for {
      requestObject      <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      trusteeDetailsList <- ersUtil.fetch[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, requestObject.getSchemeReference)
    } yield Ok(trusteeSummaryView(requestObject, trusteeDetailsList))) recover { case e: Exception =>
      logger.error(
        s"[TrusteeController][showTrusteeSummaryPage] Get data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}."
      )
      getGlobalErrorPage
    }

  def trusteeSummaryContinue(): Action[AnyContent] = authAction.async {
    continueFromTrusteeSummaryPage()
  }

  def continueFromTrusteeSummaryPage(): Future[Result] =
    Future(Redirect(routes.AltAmendsController.altActivityPage()))

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )

}
