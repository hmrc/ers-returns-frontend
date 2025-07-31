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
import controllers.auth.AuthActionGovGateway
import models.ErsMetaData
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.FrontendSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject() (val mcc: MessagesControllerComponents,
                                       unauthorisedView: views.html.unauthorised,
                                       signedOutView: views.html.signedOut,
                                       notAuthorisedView: views.html.not_authorised,
                                       val sessionService: FrontendSessionService,
                                       authAction: AuthActionGovGateway)
                                      (implicit val ec: ExecutionContext,
                                       val ersUtil: ERSUtil,
                                       val appConfig: ApplicationConfig) extends FrontendController(mcc) with I18nSupport {

  def unauthorised(): Action[AnyContent] = Action { implicit request =>
    Unauthorized(unauthorisedView())
  }

  //TODO investigate why both of these are needed
  def notAuthorised(): Action[AnyContent] = authAction.async { implicit request =>
    //TODO the content of this page references ERS Checking - needs investigation
    Future.successful(Unauthorized(notAuthorisedView.render(request, request2Messages, appConfig)))
  }

  def timedOut(): Action[AnyContent] = Action { implicit request =>
    val loginScreenUrl = appConfig.portalDomain
    Ok(signedOutView(loginScreenUrl))
  }

  def keepAlive: Action[AnyContent] = Action.async { implicit request =>
    sessionService.fetch[ErsMetaData](ersUtil.ERS_METADATA).flatMap {
      case data: ErsMetaData =>
        sessionService.cache(ersUtil.ERS_METADATA, data).map { _ =>
          Ok("OK")
        }
      case _ =>
        logger.warn("[ApplicationController][keepAlive] No session data found for ERS_METADATA in keepAlive")
        Future.failed(new Exception("no Session"))
    }.recover {
      case ex =>
        logger.error("[ApplicationController][keepAlive] Unexpected error in keepAlive", ex)
        InternalServerError("Unexpected error (test)")
    }
  }
}
