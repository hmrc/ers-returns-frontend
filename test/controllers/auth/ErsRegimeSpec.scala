/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

class ErsRegimeSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  private val orgAccount = Some(OrgAccount("org/1234", Org("1234")))
  private val saAccount = Some(SaAccount("", SaUtr("1234567890")))
  private val vatAccount = Some(VatAccount("", Vrn("123456789")))
  private val epayeAccount = Some(EpayeAccount("", EmpRef("000", "AA00000")))
  private val authority = Authority("", Accounts(), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
  private val randomToken = "RANDOMTOKEN"

  "ERSRegime" must {
    "define isAuthorised" must {
      val accounts = mock[Accounts](RETURNS_DEEP_STUBS)
      "return true when epaye is defined" in {
        when(accounts.epaye.isDefined).thenReturn(true)
        ERSRegime.isAuthorised(accounts) must be(true)
      }

      "return false when epaye is not defined" in {
        when(accounts.epaye.isDefined).thenReturn(false)
        ERSRegime.isAuthorised(accounts) must be(false)
      }
    }
    "define the authentication type as the epaye GG" in {
      ERSRegime.authenticationType must be(ErsFileValidatorGovernmentGateway)
    }

    "define the unauthorised landing page as /unauthorised" in {
      ERSRegime.unauthorisedLandingPage.get must be("/submit-your-ers-annual-return/unauthorised")
    }
  }


  "when applying the ERSRegime" must {
    val mockAuthConnector = mock[AuthConnector]
    object TestController extends FrontendController with Actions {
      val authConnector: AuthConnector = mockAuthConnector
      def testRoute: Action[AnyContent] = AuthorisedFor(ERSRegime, pageVisibility = GGConfidence) {
        implicit user =>
          implicit request =>
            Ok
      }
    }

    "logged-in users" must {
      val userId = s"user-${UUID.randomUUID}"
      val sessionId = s"session-${UUID.randomUUID}"
      val request = FakeRequest().withSession(SessionKeys.sessionId -> sessionId, SessionKeys.token -> randomToken, SessionKeys.userId -> userId)
      "with a psa account" must {
        "allow the user access to the page" in {
          val newAuthority = authority.copy(uri = userId, accounts = Accounts(sa = saAccount, vat = vatAccount, epaye = epayeAccount))
          when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn Future.successful(Some(newAuthority))
          val result = TestController.testRoute.apply(request)
          status(result) must be(OK)
        }
      }

      "without a psa account" must {
        "redirect the user to the unauthorised page" in {
          val newAuthority = authority.copy(uri = userId, accounts = Accounts(org = orgAccount))
          when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn Future.successful(Some(newAuthority))
          val result = TestController.testRoute.apply(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/submit-your-ers-annual-return/unauthorised"))
        }
      }
    }

    "not logged-in users" must {
      "redirect to the gg sign-in page" in {
        val result = TestController.testRoute.apply(FakeRequest())
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }
  }
}
