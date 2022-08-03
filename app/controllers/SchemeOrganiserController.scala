/*
 * Copyright 2022 HM Revenue & Customs
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
class SchemeOrganiserController @Inject()(val mcc: MessagesControllerComponents,
																					val authConnector: DefaultAuthConnector,
																					implicit val countryCodes: CountryCodes,
																					implicit val ersUtil: ERSUtil,
																					implicit val appConfig: ApplicationConfig,
                                          globalErrorView: views.html.global_error,
                                          schemeOrganiserView: views.html.scheme_organiser,
                                          authAction: AuthAction
																				 ) extends FrontendController(mcc) with I18nSupport with WithUnsafeDefaultFormBinding with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def schemeOrganiserPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
          showSchemeOrganiserPage(requestObject)(request, hc)
        }
  }

  def showSchemeOrganiserPage(requestObject: RequestObject)
														 (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    logger.info(s"[SchemeOrganiserController][showSchemeOrganiserPage] schemeRef: ${requestObject.getSchemeReference}.")
		lazy val form = SchemeOrganiserDetails("", "", Some(""), Some(""), Some(""), Some(ersUtil.DEFAULT_COUNTRY), Some(""), Some(""), Some(""))

		ersUtil.fetch[ReportableEvents](ersUtil.reportableEvents, requestObject.getSchemeReference).flatMap { reportableEvent =>
      ersUtil.fetchOption[CheckFileType](ersUtil.FILE_TYPE_CACHE, requestObject.getSchemeReference).flatMap { fileType =>
        ersUtil.fetch[SchemeOrganiserDetails](ersUtil.SCHEME_ORGANISER_CACHE, requestObject.getSchemeReference).map { res =>
          val FileType = if (fileType.isDefined) {
            fileType.get.checkFileType.get
          } else {
            ""
          }
          Ok(schemeOrganiserView(requestObject, FileType, RsFormMappings.schemeOrganiserForm.fill(res), reportableEvent.isNilReturn.get))
        } recover {
          case _: NoSuchElementException =>
            Ok(schemeOrganiserView(
							requestObject,
							fileType.get.checkFileType.get,
							RsFormMappings.schemeOrganiserForm.fill(form),
							reportableEvent.isNilReturn.get
						))
        }
      } recover {
        case _: NoSuchElementException =>
          Ok(schemeOrganiserView(requestObject, "", RsFormMappings.schemeOrganiserForm.fill(form), reportableEvent.isNilReturn.get))
      }
    } recover {
      case e: Exception =>
				logger.error(s"[SchemeOrganiserController][showSchemeOrganiserPage] Get reportableEvent.isNilReturn failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
				getGlobalErrorPage
		}
  }

  def schemeOrganiserSubmit(): Action[AnyContent] = authAction.async {
      implicit request =>
        ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
          showSchemeOrganiserSubmit(requestObject)(request, hc)
        }
  }

  def showSchemeOrganiserSubmit(requestObject: RequestObject)
															 (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    RsFormMappings.schemeOrganiserForm.bindFromRequest.fold(
      errors => {
        val correctOrder = errors.errors.map(_.key).distinct
        val incorrectOrderGrouped = errors.errors.groupBy(_.key).map(_._2.head).toSeq
        val correctOrderGrouped = correctOrder.flatMap(x => incorrectOrderGrouped.find(_.key == x))
        val firstErrors: Form[models.SchemeOrganiserDetails] = new Form[SchemeOrganiserDetails](errors.mapping, errors.data, correctOrderGrouped, errors.value)
        Future.successful(Ok(schemeOrganiserView(requestObject, "", firstErrors)))
      },
      successful => {

        logger.warn(s"[SchemeOrganiserController][showSchemeOrganiserSubmit] schemeRef: ${requestObject.getSchemeReference}.")

        ersUtil.cache(ersUtil.SCHEME_ORGANISER_CACHE, successful, requestObject.getSchemeReference).map {
          _ => Redirect(routes.GroupSchemeController.groupSchemePage())
        } recover {
          case e: Exception =>
            logger.error(s"[SchemeOrganiserController][showSchemeOrganiserSubmit] Save scheme organiser details failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
            getGlobalErrorPage
        }
      }
    )
  }

	def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		Ok(globalErrorView(
			"ers.global_errors.title",
			"ers.global_errors.heading",
			"ers.global_errors.message"
		)(request, messages, appConfig))
	}

}
