/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.auth.AuthFunctionality
import models.{ERSAuthData, ErsMetaData, RequestObject}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import utils.ERSUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Authenticator extends AuthFunctionality {
	val ersUtil: ERSUtil
	private type AsyncUserRequest = ERSAuthData => Request[AnyContent] => Future[Result]
	private type UserRequest = ERSAuthData => Request[AnyContent] => Result

	def authorisedForAsync()(body: AsyncUserRequest): Action[AnyContent] = Action.async {
		implicit request =>
			implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
			authoriseFor { implicit user =>
				filterAgentsWrapperAsync(user, body)
			}
	}

	def authorisedByGG(body: AsyncUserRequest): Action[AnyContent] = Action.async {
		implicit request =>
			implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
			authorisedByGovGateway { implicit user =>
				filterAgentsWrapperAsync(user, body)
			}
	}

	def filterAgentsWrapperAsync(authContext: ERSAuthData, body: AsyncUserRequest)
															(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
		implicit val formatRSParams: OFormat[ErsMetaData] = Json.format[ErsMetaData]
		if (authContext.isAgent) {
			for {
				requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
				all <- ersUtil.fetch[ErsMetaData](ersUtil.ersMetaData, requestObject.getSchemeReference)
				result <- body(delegationModelUser(all, authContext: ERSAuthData))(request)
			} yield {
				result
			}
		} else {
			val alteredAuthContext: ERSAuthData = authContext.getEnrolment("IR-PAYE") map { enrol =>
				authContext.copy(empRef = EmpRef(enrol.identifiers.head.value, enrol.identifiers(1).value))
			} getOrElse authContext

			body(alteredAuthContext)(request)
		}
	}

	def delegationModelUser(metaData: ErsMetaData, authContext: ERSAuthData): ERSAuthData = {
		val twoPartKey = metaData.empRef.split('/')
		authContext.copy(empRef = EmpRef(twoPartKey(0), twoPartKey(1)))
	}
}

