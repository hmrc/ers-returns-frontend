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

package controllers.auth

import config.ApplicationConfig
import helpers.ErsTestHelper
import models.ERSAuthData
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.Helpers.redirectLocation
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthFunctionalitySpec extends UnitSpec with GuiceOneAppPerSuite with ErsTestHelper with DefaultAwaitTimeout {

  class Setup(enrolmentSet: Set[Enrolment], affGroup: Option[AffinityGroup] = None, testEmpRef: EmpRef = EmpRef("", "")) {
    val controllerHarness: AuthFunctionality = new AuthFunctionality {
      override val authConnector: AuthConnector = mockAuthConnector
			override val appConfig: ApplicationConfig = mockAppConfig
    }

		val ersAuthData: ERSAuthData = ERSAuthData(
			enrolments = enrolmentSet,
			affinityGroup = affGroup,
			empRef = testEmpRef
		)
  }

  "authoriseFor" should {
    "authorise a user" when {
      "they have a valid enrolment" in new Setup(ersEnrolmentSet, Some(Agent)) {
        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(buildRetrieval(ersAuthData)))

        val func: ERSAuthData => Future[Result] = (_: ERSAuthData) => Future.successful(Results.Ok("test"))

        val res: Future[Result] = controllerHarness.authoriseFor(func)
        status(res) shouldBe 200
      }
    }

		"redirect and fail to authorise" when {
			"it receives a NoActiveSessionException" in new Setup(invalidEnrolmentSet) {
				setUnauthorisedMocks()

				val func: ERSAuthData => Future[Result] = (_: ERSAuthData) => Future.successful(Results.Ok("test"))

				val res: Future[Result] = controllerHarness.authoriseFor(func)
				status(res) shouldBe 303
				redirectLocation(res) shouldBe Some("http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9290%2Fsubmit-your-ers-annual-return&origin=ers-returns-frontend")
			}

			"it receives an AuthorisationException" in new Setup(invalidEnrolmentSet) {
				when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
					.thenReturn(Future.failed(UnsupportedAuthProvider("Not GGW")))

				val func: ERSAuthData => Future[Result] = (_: ERSAuthData) => Future.successful(Results.Ok("test"))

				val res: Future[Result] = controllerHarness.authoriseFor(func)
				status(res) shouldBe 303
				redirectLocation(res) shouldBe Some("/submit-your-ers-annual-return/unauthorised")
			}
		}
	}
}
