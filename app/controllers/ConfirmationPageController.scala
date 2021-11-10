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

import _root_.models._
import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionKeys.{BUNDLE_REF, DATE_TIME_SUBMITTED}
import utils._

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmationPageController @Inject()(val mcc: MessagesControllerComponents,
																					 val ersConnector: ErsConnector,
																					 val authConnector: DefaultAuthConnector,
																					 val auditEvents: AuditEvents,
																					 implicit val ersUtil: ERSUtil,
																					 implicit val appConfig: ApplicationConfig,
                                           globalErrorView: views.html.global_error,
                                           confirmationView: views.html.confirmation,
                                           authAction: AuthAction
                                          ) extends FrontendController(mcc) with I18nSupport with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def confirmationPage(): Action[AnyContent] = authAction.async {
			implicit request =>
          showConfirmationPage()(request, hc)
  }

  def showConfirmationPage()(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
      val schemeRef: String = requestObject.getSchemeReference
      val sessionBundleRef: String = request.session.get(BUNDLE_REF).getOrElse("")
      val sessionDateTimeSubmitted: String = request.session.get(DATE_TIME_SUBMITTED).getOrElse("")
      if (sessionBundleRef == "") {
        ersUtil.fetch[ErsMetaData](ersUtil.ersMetaData, schemeRef).flatMap { all =>
          if (all.sapNumber.isEmpty) logger.error(s"[ConfirmationPageController][showConfirmationPage] Did cache util fail for scheme $schemeRef all.sapNumber is empty: $all")
					val submissionJson = ersUtil.getSubmissionJson(all.schemeInfo.schemeRef, all.schemeInfo.schemeType, all.schemeInfo.taxYear, "EOY-RETURN")
          ersConnector.connectToEtmpSummarySubmit(all.sapNumber.get, submissionJson).flatMap { bundle =>
            ersUtil.getAllData(bundle, all).flatMap { alldata =>
              if (alldata.isNilReturn == ersUtil.OPTION_NIL_RETURN) {
                saveAndSubmit(alldata, all, bundle)
              } else {
                ersUtil.fetch[String](ersUtil.VALIDATED_SHEEETS, schemeRef).flatMap { validatedSheets =>
                  ersConnector.checkForPresubmission(all.schemeInfo, validatedSheets).flatMap { checkResult =>
                    checkResult.status match {
                      case OK =>
                        logger.info(s"[ConfirmationPageController][showConfirmationPage] Check for presubmission success with status ${checkResult.status}.")
                        saveAndSubmit(alldata, all, bundle)
                      case _ =>
                        logger.error(s"[ConfirmationPageController][showConfirmationPage] File data not found: ${checkResult.status}")
                        Future(getGlobalErrorPage)
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        val url: String = appConfig.portalDomain
        ersUtil.fetch[ErsMetaData](ersUtil.ersMetaData, schemeRef).flatMap { all =>
          logger.info(s"[ConfirmationPageController][showConfirmationPage] Preventing resubmission of confirmation page, timestamp: ${System.currentTimeMillis()}.")

          Future(Ok(confirmationView(requestObject, sessionDateTimeSubmitted, sessionBundleRef, all.schemeInfo.taxYear, url)))
        }
      }
    } recoverWith {
      case e: Throwable =>
        logger.error(s"[ConfirmationPageController][showConfirmationPage] Failed to render Confirmation page: ${e.getMessage}")
        Future.successful(getGlobalErrorPage)
    }
  }

  def saveAndSubmit(alldata: ErsSummary, all: ErsMetaData, bundle: String)
									 (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {

    val jsonDateTimeFormat = new SimpleDateFormat("d MMMM yyyy, h:mma")
    val dateTimeSubmitted = jsonDateTimeFormat.format(alldata.confirmationDateTime.toDate).replace("AM", "am").replace("PM", "pm")

    ersConnector.saveMetadata(alldata).flatMap { res =>
      res.status match {
        case OK =>
          val startTime = System.currentTimeMillis()
          logger.info("[ConfirmationPageController][saveAndSubmit] alldata.transferStatus is " + alldata.transferStatus)
          if (alldata.transferStatus.contains(ersUtil.largeFileStatus)) {
            None
          } else {
            ersConnector.submitReturnToBackend(alldata).map { response =>
              response.status match {
                case OK =>
                  auditEvents.ersSubmissionAuditEvent(all, bundle)
                  ersUtil.submitReturnToBackend(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
                  logger.info(s"[ConfirmationPageController][saveAndSubmit] Submitting return to backend success with status ${response.status}.")
                case _ =>
                  ersUtil.submitReturnToBackend(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
                  logger.info(s"[ConfirmationPageController][saveAndSubmit] Submitting return to backend failed with status ${response.status}.")
              }
              logger.info(s"Process data ends: ${System.currentTimeMillis()}")
            } recover {
              case e: Throwable =>
                logger.error(s"[ConfirmationPageController][saveAndSubmit] Submitting return to backend failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
                auditEvents.auditRunTimeError(e.getCause, e.getMessage, all, bundle)
            }
          }

          logger.warn(s"[ConfirmationPageController][saveAndSubmit] Submission completed for schemeInfo: ${all.schemeInfo.toString}, bundle: $bundle ")
          val url: String = appConfig.portalDomain

          ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).map { requestObject =>
            Ok(confirmationView(requestObject, dateTimeSubmitted, bundle, all.schemeInfo.taxYear, url))
							.withSession(request.session + (BUNDLE_REF -> bundle) + (DATE_TIME_SUBMITTED -> dateTimeSubmitted))
          }
        case _ =>
          logger.info(s"[ConfirmationPageController][saveAndSubmit] Save meta data to backend returned status ${res.status}, timestamp: ${System.currentTimeMillis()}.")
          Future.successful(getGlobalErrorPage)
      }
    } recover { case e: Throwable =>
      logger.error(s"[ConfirmationPageController][saveAndSubmit] Save meta data to backend failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
      getGlobalErrorPage
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
