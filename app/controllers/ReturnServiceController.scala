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
import config._
import controllers.auth.AuthActionGovGateway
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionKeys.{BUNDLE_REF, DATE_TIME_SUBMITTED}
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnServiceController @Inject() (val mcc: MessagesControllerComponents,
                                         val sessionService: FrontendSessionService,
                                         globalErrorView: views.html.global_error,
                                         unauthorisedView: views.html.unauthorised,
                                         startView: views.html.start,
                                         authActionGovGateway: AuthActionGovGateway)
                                        (implicit val ec: ExecutionContext,
                                         val ersUtil: ERSUtil,
                                         val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with HMACUtil with Logging {

  lazy val accessThreshold: Int = appConfig.accessThreshold
  val accessDeniedUrl = "/outage-ers-frontend/index.html"

  def cacheParams(ersRequestObject: RequestObject)(implicit request: Request[_]): Future[Result] = {
    implicit val formatRSParams: OFormat[RequestObject] = Json.format[RequestObject]
    logger.debug("Request Object created --> " + ersRequestObject)
    val futureResult = for {
      _ <- sessionService.remove(ersRequestObject.getSchemeReference)
      _ <- sessionService.cache(ersUtil.ERS_METADATA, ersRequestObject.toErsMetaData)
      _ <- sessionService.cache(ersUtil.ERS_REQUEST_OBJECT, ersRequestObject)
    } yield {
      logger.info(s"[ReturnServiceController][cacheParams] Meta Data Cached --> ${ersRequestObject.toErsMetaData}")
      logger.info(s"[ReturnServiceController][cacheParams] Request Object Cached -->  $ersRequestObject")
      showInitialStartPage(ersRequestObject)(request)
    }
    futureResult recover { case e: Exception =>
      logger.warn(s"[ReturnServiceController][cacheParams] Caught exception ${e.getMessage}", e)
      getGlobalErrorPage
    }
  }

  def getRequestParameters(request: Request[AnyContent]): RequestObject = {
    val aoRef: Option[String]        = request.getQueryString("aoRef")
    val taxYear: Option[String]      = request.getQueryString("taxYear")
    val ersSchemeRef: Option[String] = request.getQueryString("ersSchemeRef")
    val schemeType: Option[String]   = request.getQueryString("schemeType")
    val schemeName: Option[String]   = request.getQueryString("schemeName")
    val agentRef: Option[String]     = request.getQueryString("agentRef")
    val empRef: Option[String]       = request.getQueryString("empRef")
    val ts: Option[String]           = request.getQueryString("ts")
    val hmac: Option[String]         = request.getQueryString("hmac")
    val reqObj                       = RequestObject(aoRef, taxYear, ersSchemeRef, schemeName, schemeType, agentRef, empRef, ts, hmac)
    logger.info(s"Request Parameters:  ${reqObj.toString}")
    reqObj
  }

  def hmacCheck(): Action[AnyContent] = authActionGovGateway.async { implicit request =>
    logger.info("[ReturnServiceController][hmacCheck] HMAC Check Authenticated")
    if (request.getQueryString("ersSchemeRef").getOrElse("") == "") {
      logger.warn("[ReturnServiceController][hmacCheck] Missing SchemeRef in URL")
      Future(getGlobalErrorPage)
    } else {
      if (isHmacAndTimestampValid(getRequestParameters(request))) {
        logger.info("[ReturnServiceController][hmacCheck] HMAC Check Valid")
        try cacheParams(getRequestParameters(request))
        catch {
          case e: Throwable =>
            logger.warn(s"[ReturnServiceController][hmacCheck] Caught exception ${e.getMessage}", e)
            Future(getGlobalErrorPage)
        }
      } else {
        logger.warn("[ReturnServiceController][hmacCheck] HMAC Check Invalid")
        showUnauthorisedPage(request)
      }
    }
  }

  def showInitialStartPage(requestObject: RequestObject)(implicit request: Request[_]): Result = {
    val sessionData = s"${requestObject.getSchemeId} - ${requestObject.getPageTitle}"
    Ok(startView(requestObject)).withSession(
      request.session + ("screenSchemeInfo" -> sessionData) - BUNDLE_REF - DATE_TIME_SUBMITTED
    )
  }

  def startPage(): Action[AnyContent] = authActionGovGateway.async { implicit request =>
    sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT).map { result =>
      Ok(startView(result)).withSession(request.session - BUNDLE_REF - DATE_TIME_SUBMITTED)
    }
  }

  def showUnauthorisedPage(implicit request: Request[_]): Future[Result] =
    Future.successful(Unauthorized(unauthorisedView()))

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
    Ok(
      globalErrorView(
        "ers.global_errors.title",
        "ers.global_errors.heading",
        "ers.global_errors.message"
      )(request, messages, appConfig)
    )
}
