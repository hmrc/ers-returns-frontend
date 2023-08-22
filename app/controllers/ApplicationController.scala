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
import controllers.auth.AuthActionGovGateway
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject() (
  val mcc: MessagesControllerComponents,
  val authConnector: DefaultAuthConnector,
  implicit val ersUtil: ERSUtil,
  implicit val appConfig: ApplicationConfig,
  unauthorisedView: views.html.unauthorised,
  signedOutView: views.html.signedOut,
  notAuthorisedView: views.html.not_authorised,
  authAction: AuthActionGovGateway
) extends FrontendController(mcc)
    with I18nSupport {

  implicit val ec: ExecutionContext = mcc.executionContext

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
}
