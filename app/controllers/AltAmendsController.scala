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
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding

import scala.concurrent._

@Singleton
class AltAmendsController @Inject()(val mcc: MessagesControllerComponents,
																		val authConnector: DefaultAuthConnector,
																		implicit val ersUtil: ERSUtil,
																		implicit val appConfig: ApplicationConfig,
                                    alterationsActivityView: views.html.alterations_activity,
                                    alterationsAmendsView: views.html.alterations_amends,
                                    globalErrorView: views.html.global_error,
                                    authAction: AuthAction
																	  ) extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def altActivityPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showAltActivityPage()
  }

  def showAltActivityPage()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ( for {
      requestObject     <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      groupSchemeInfo   <- ersUtil.fetch[GroupSchemeInfo](ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, requestObject.getSchemeReference)
      altAmendsActivity <- ersUtil.fetch[AltAmendsActivity](ersUtil.altAmendsActivity, requestObject.getSchemeReference).recover {
        case _: NoSuchElementException => AltAmendsActivity("")
      }
    } yield {
      Ok(alterationsActivityView(
        requestObject,
        altAmendsActivity.altActivity,
        groupSchemeInfo.groupScheme.getOrElse(ersUtil.DEFAULT),
        RsFormMappings.altActivityForm.fill(altAmendsActivity)
      ))
      }).recover {
        case e: Exception =>
          logger.error(s"[AltAmendsController][showAltActivityPage] Rendering AltAmends view failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
          getGlobalErrorPage
      }
  }


  def altActivitySelected(): Action[AnyContent] = authAction.async {
      implicit request =>
          showAltActivitySelected()(request, hc)

  }

  def showAltActivitySelected()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {

    ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
      RsFormMappings.altActivityForm.bindFromRequest.fold(
        errors => {
          Future.successful(Ok(alterationsActivityView(requestObject, "", "", errors)))
        },
        formData => {
          ersUtil.cache(ersUtil.altAmendsActivity, formData, requestObject.getSchemeReference).map { _ =>
            formData.altActivity match {
              case ersUtil.OPTION_NO => Redirect(routes.SummaryDeclarationController.summaryDeclarationPage())
              case ersUtil.OPTION_YES => Redirect(routes.AltAmendsController.altAmendsPage())
            }
          }.recover {
            case e: Throwable =>
              logger.error(s"showAltActivitySelected: Save data to cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
              getGlobalErrorPage
          }
        }
      )
    }
  }

  def altAmendsPage(): Action[AnyContent] = authAction.async {
      implicit request =>
          showAltAmendsPage()(request, hc)
  }

  def showAltAmendsPage()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {

    for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      altAmends     <- ersUtil.fetchOption[AltAmends](ersUtil.ALT_AMENDS_CACHE_CONTROLLER, requestObject.getSchemeReference).recover {
        case _: Throwable => None
      }
    } yield {
      Ok(alterationsAmendsView(requestObject, altAmends.getOrElse(AltAmends(None, None, None, None, None))))
    }
  }

  def altAmendsSelected(): Action[AnyContent] = authAction.async {
      implicit request =>
        showAltAmendsSelected()(request, hc)
  }

  def showAltAmendsSelected()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {

    ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
      RsFormMappings.altAmendsForm.bindFromRequest.fold(
        _ => {
          Future.successful(Redirect(routes.AltAmendsController.altAmendsPage())
						.flashing("alt-amends-not-selected-error" -> ersUtil.getPageElement(requestObject.getSchemeId, ersUtil.PAGE_ALT_AMENDS, "err.message")))
        },
        formData => {
          ersUtil.cache(ersUtil.ALT_AMENDS_CACHE_CONTROLLER, formData, requestObject.getSchemeReference).flatMap { _ =>
            if (formData.altAmendsTerms.isEmpty
              && formData.altAmendsEligibility.isEmpty
              && formData.altAmendsExchange.isEmpty
              && formData.altAmendsVariations.isEmpty
              && formData.altAmendsOther.isEmpty) {
              Future.successful(Redirect(routes.AltAmendsController.altAmendsPage())
								.flashing("alt-amends-not-selected-error" -> ersUtil.getPageElement(requestObject.getSchemeId, ersUtil.PAGE_ALT_AMENDS, "err.message")))
            } else {
              Future.successful(Redirect(routes.SummaryDeclarationController.summaryDeclarationPage()))
            }
          } recover {
            case e: Throwable =>
							logger.error(s"[AltAmendsController][showAltAmendsSelected] Save data to cache failed with exception ${e.getMessage}, " +
								s"timestamp: ${System.currentTimeMillis()}.")
							getGlobalErrorPage
					}
        }
      )
    }
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		Ok(globalErrorView(
			"ers.global_errors.title",
			"ers.global_errors.heading",
			"ers.global_errors.message"
		)(request, messages, appConfig))
	}
}
