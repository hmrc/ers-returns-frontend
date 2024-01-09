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

import models.{ERSAuthData, ErsMetaData, RequestObject, SchemeInfo}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.test.Helpers.{redirectLocation, status, stubBodyParser}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ErsTestHelper

import scala.concurrent.{ExecutionContext, Future}

class AuthFunctionalitySpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ErsTestHelper
    with DefaultAwaitTimeout
    with GuiceFakeApplicationFactory {

  class Setup(
    enrolmentSet: Set[Enrolment],
    affGroup: Option[AffinityGroup] = None,
    testEmpRef: EmpRef = EmpRef("", "")
  ) {
    val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
      messagesActionBuilder,
      DefaultActionBuilder(stubBodyParser[AnyContent]()),
      cc.parsers,
      fakeApplication().injector.instanceOf[MessagesApi],
      cc.langs,
      cc.fileMimeTypes,
      ExecutionContext.global
    )

		class TestController(authAction: AuthAction, val mcc: MessagesControllerComponents) extends FrontendController(mcc){
			def onPageLoad(): Action[AnyContent] = authAction { _ => Ok }
		}
		class TestControllerGov(authAction: AuthActionGovGateway, val mcc: MessagesControllerComponents) extends FrontendController(mcc){
			def onPageLoad(): Action[AnyContent] = authAction { _ => Ok }
		}

    val controllerHarness = new TestController(testAuthAction, mockMCC)
    val controllerHarnessGov = new TestControllerGov(testAuthActionGov, mockMCC)

    val ersAuthData: ERSAuthData = ERSAuthData(
      enrolments = enrolmentSet,
      affinityGroup = affGroup,
      empRef = testEmpRef
    )

    lazy val schemeInfo: SchemeInfo   = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "EMI", "EMI")
    val validErsMetaData: ErsMetaData =
      ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "1234/GA4567", Some("agentRef"), Some("sapNumber"))
    val reqObj: RequestObject = RequestObject(None, None, None, None, None, None, Some("1234/GA4567"), None, None)

    when(
      mockSessionService.fetch[RequestObject](ArgumentMatchers.any())(
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )
    )
      .thenReturn(Future.successful(reqObj))
    when(
      mockSessionService.fetch[ErsMetaData](ArgumentMatchers.any())(
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )
    )
      .thenReturn(Future.successful(validErsMetaData))
  }

  "authoriseFor" should {
    "authorise a user" when {
      "they have a valid enrolment" in new Setup(ersEnrolmentSet, Some(Agent)) {
        when(
          mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )
        )
          .thenReturn(Future.successful(buildRetrieval(ersAuthData)))

        val res: Future[Result] = controllerHarness.onPageLoad()(requestWithAuth)
        status(res) shouldBe 200
      }
    }

    "redirect and fail to authorise" when {
      "it receives a NoActiveSessionException" in new Setup(invalidEnrolmentSet) {
        setUnauthorisedMocks()

        val res: Future[Result] = controllerHarness.onPageLoad()(requestWithAuth)

        status(res)           shouldBe 303
        redirectLocation(res) shouldBe Some(
          "http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9290%2Fsubmit-your-ers-annual-return&origin=ers-returns-frontend"
        )
      }

      "it receives a NoActiveSessionException and preserves query parameters" in new Setup(invalidEnrolmentSet) {
        setUnauthorisedMocks()

        implicit val testFakeRequest: FakeRequest[AnyContent]           = FakeRequest("GET", "/my-resources?a=1&b=2&c=3")
        val requestWithAuth: RequestWithOptionalAuthContext[AnyContent] =
          RequestWithOptionalAuthContext(testFakeRequest, defaultErsAuthData)

        val res: Future[Result] = controllerHarness.onPageLoad()(requestWithAuth)

        status(res)           shouldBe 303
        redirectLocation(res) shouldBe Some(
          "http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9290%2Fsubmit-your-ers-annual-return%3Fa%3D1%26b%3D2%26c%3D3&origin=ers-returns-frontend"
        )
      }

      "it receives an AuthorisationException" in new Setup(invalidEnrolmentSet) {
        when(
          mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )
        )
          .thenReturn(Future.failed(UnsupportedAuthProvider("Not GGW")))

        val res: Future[Result] = controllerHarness.onPageLoad()(requestWithAuth)

				status(res) shouldBe 303
				redirectLocation(res) shouldBe Some("/submit-your-ers-annual-return/unauthorised")
			}
		}
	}

  "authoriseFor govGateway" should {
    "authorise a user" when {
      "they have a valid enrolment" in new Setup(ersEnrolmentSet, Some(Agent)) {
        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(buildRetrieval(ersAuthData)))

        val res: Future[Result] = controllerHarnessGov.onPageLoad()(requestWithAuth)
        status(res) shouldBe 200
      }
    }

		"redirect and fail to authorise" when {
			"it receives a NoActiveSessionException" in new Setup(invalidEnrolmentSet) {
				setUnauthorisedMocks()

				val res: Future[Result] = controllerHarnessGov.onPageLoad()(requestWithAuth)

				status(res) shouldBe 303
				redirectLocation(res) shouldBe Some("http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9290%2Fsubmit-your-ers-annual-return&origin=ers-returns-frontend")
			}

			"it receives a NoActiveSessionException and preserves query parameters" in new Setup(invalidEnrolmentSet) {
				setUnauthorisedMocks()

				implicit val testFakeRequest: FakeRequest[AnyContent] = FakeRequest("GET", "/my-resources?a=1&b=2&c=3")
				val requestWithAuth: RequestWithOptionalAuthContext[AnyContent] = RequestWithOptionalAuthContext(testFakeRequest, defaultErsAuthData)

				val res: Future[Result] = controllerHarnessGov.onPageLoad()(requestWithAuth)

				status(res) shouldBe 303
				redirectLocation(res) shouldBe Some("http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9290%2Fsubmit-your-ers-annual-return%3Fa%3D1%26b%3D2%26c%3D3&origin=ers-returns-frontend")
			}

			"it receives an AuthorisationException" in new Setup(invalidEnrolmentSet) {
				when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
					.thenReturn(Future.failed(UnsupportedAuthProvider("Not GGW")))

				val res: Future[Result] = controllerHarnessGov.onPageLoad()(requestWithAuth)

				status(res) shouldBe 303
				redirectLocation(res) shouldBe Some("/submit-your-ers-annual-return/unauthorised")
			}
		}
	}

	"delegationUserModel method" should {
		"return an ERSAuthData with the empRef updated" in new Setup(ersEnrolmentSet, Some(Agent)) {
			val metadata = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "1234/5678", Some("agentRef"), Some("sapNumber"))
			val authdata = defaultErsAuthData
			val result = testAuthActionGov.delegationModelUser(metadata, authdata)

			result.empRef shouldBe EmpRef("1234", "5678")
		}
	}
}
