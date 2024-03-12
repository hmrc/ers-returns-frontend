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

package controllers.auth

import org.apache.pekko.http.scaladsl.model.Uri
import config.ApplicationConfig
import models.{ERSAuthData, ErsMetaData}
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.FrontendSessionService
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Constants

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class RequestWithOptionalAuthContext[A](request: Request[A], authData: ERSAuthData)
    extends WrappedRequest[A](request)
trait AuthIdentifierAction
    extends ActionBuilder[RequestWithOptionalAuthContext, AnyContent]
    with ActionFunction[Request, RequestWithOptionalAuthContext]

@Singleton
class AuthAction @Inject() (override val authConnector: DefaultAuthConnector,
                            appConfig: ApplicationConfig,
                            sessionService: FrontendSessionService,
                            val parser: BodyParsers.Default)
                           (implicit val executionContext: ExecutionContext)
    extends AuthorisedFunctions with AuthIdentifierAction with Constants with Logging {

  lazy val signInUrl: String = appConfig.ggSignInUrl
  lazy val origin: String    = appConfig.appName

  def loginParams(params: Map[String, String]): Map[String, Seq[String]] = Map(
    "continue" -> Seq(Uri(appConfig.loginCallback).withQuery(Uri.Query(params)).toString()),
    "origin" -> Seq(origin)
  )

  def delegationModelUser(metaData: ErsMetaData, authContext: ERSAuthData): ERSAuthData = {
    val twoPartKey = metaData.empRef.split('/')
    authContext.copy(empRef = EmpRef(twoPartKey(0), twoPartKey(1)))
  }

  override def invokeBlock[A](
    request: Request[A],
    block: RequestWithOptionalAuthContext[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val formatRSParams: OFormat[ErsMetaData] = Json.format[ErsMetaData]

    authorised((Enrolment("IR-PAYE") or Enrolment("HMRC-AGENT-AGENT") or Agent) and AuthProviders(GovernmentGateway))
      .retrieve(authorisedEnrolments and affinityGroup) { case authorisedEnrolments ~ affinityGroup =>
        val authContext = ERSAuthData(authorisedEnrolments.enrolments, affinityGroup)

        if (authContext.isAgent) {
          for {
            all <- sessionService.fetch[ErsMetaData](ERS_METADATA)(request, implicitly)
            result <- block(RequestWithOptionalAuthContext(request, delegationModelUser(all, authContext: ERSAuthData)))
          } yield result
        } else {
          val alteredAuthContext: ERSAuthData = authContext.getEnrolment("IR-PAYE") map { enrol =>
            authContext.copy(empRef = EmpRef(enrol.identifiers.head.value, enrol.identifiers(1).value))
          } getOrElse authContext

          block(RequestWithOptionalAuthContext(request, alteredAuthContext))
        }
      } recover {
      case _: NoActiveSession         =>
        Redirect(signInUrl, loginParams(request.queryString.map { case (k, v) => k -> v.headOption.getOrElse("") }))
      case er: AuthorisationException =>
        logger.error(s"[AuthFunctionality][handleException] Auth exception: $er")
        Redirect(controllers.routes.ApplicationController.unauthorised().url)
    }
  }
}

@Singleton
class AuthActionGovGateway @Inject() (
  override val authConnector: DefaultAuthConnector,
  appConfig: ApplicationConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthorisedFunctions
    with AuthIdentifierAction
    with Logging {

  lazy val signInUrl: String = appConfig.ggSignInUrl
  lazy val origin: String    = appConfig.appName

  def loginParams(params: Map[String, String]): Map[String, Seq[String]] = Map(
    "continue" -> Seq(Uri(appConfig.loginCallback).withQuery(Uri.Query(params)).toString()),
    "origin"   -> Seq(origin)
  )

  def delegationModelUser(metaData: ErsMetaData, authContext: ERSAuthData): ERSAuthData = {
    val twoPartKey = metaData.empRef.split('/')
    authContext.copy(empRef = EmpRef(twoPartKey(0), twoPartKey(1)))
  }

  override def invokeBlock[A](
    request: Request[A],
    block: RequestWithOptionalAuthContext[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(AuthProviders(GovernmentGateway)).retrieve(allEnrolments and affinityGroup) {
      case authorisedEnrolments ~ affinityGroup =>
        val authContext = ERSAuthData(authorisedEnrolments.enrolments, affinityGroup)
        block(RequestWithOptionalAuthContext(request, authContext))
    } recover {
      case _: NoActiveSession         =>
        Redirect(signInUrl, loginParams(request.queryString.map { case (k, v) => k -> v.headOption.getOrElse("") }))
      case er: AuthorisationException =>
        logger.error(s"[AuthFunctionality][handleException] Auth exception: $er")
        Redirect(controllers.routes.ApplicationController.unauthorised().url)
    }
  }
}
