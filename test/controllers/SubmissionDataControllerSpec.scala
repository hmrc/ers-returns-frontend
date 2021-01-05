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

import akka.stream.Materializer
import connectors.ErsConnector
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpResponse
import views.html.global_error

class SubmissionDataControllerSpec extends UnitSpec with ERSFakeApplicationConfig with ErsTestHelper with GuiceOneAppPerSuite {

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)

  implicit lazy val mat: Materializer = app.materializer
  val globalErrorView: global_error = app.injector.instanceOf[global_error]

  "calling createSchemeInfoFromURL" should {

    lazy val submissionDataController: SubmissionDataController =
			new SubmissionDataController(mockMCC, mockAuthConnector, mockErsConnector, mockErsUtil, mockAppConfig, globalErrorView)

    "return correct json if all parameters are given in request" in {
      val request = FakeRequest("GET", "/get-submission-data?schemeRef=AA0000000000000&confTime=2016-08-05T11:14:30")
      val result = submissionDataController.createSchemeInfoFromURL(request)
      result shouldBe Some(
        Json.obj(
          "schemeRef" -> "AA0000000000000",
          "confTime" -> "2016-08-05T11:14:30"
        )
      )
    }

    "return None if not all parameters are given in request" in {
      val request = FakeRequest("GET", "").withBody(
        Json.obj(
          "confTime" -> "2016-08-05T11:14:30"
        )
      )
      val result = submissionDataController.createSchemeInfoFromURL(request)
      result shouldBe None
    }

  }

  "calling retrieveSubmissionData" should {
		lazy val submissionDataController: SubmissionDataController =
			new SubmissionDataController(mockMCC, mockAuthConnector, mockErsConnector, mockErsUtil, mockAppConfig, globalErrorView)

    "redirect to login page if user is not authenticated" in {
			setUnauthorisedMocks()
      val result = submissionDataController.retrieveSubmissionData()(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe SEE_OTHER
    }
  }

  "calling getRetrieveSubmissionData" should {

    val mockErsConnector: ErsConnector = mock[ErsConnector]

		class Setup(obj: Option[JsObject] = None)
			extends SubmissionDataController(mockMCC, mockAuthConnector, mockErsConnector, mockErsUtil, mockAppConfig, globalErrorView) {
			when(mockAppConfig.enableRetrieveSubmissionData).thenReturn(true)

			override def createSchemeInfoFromURL(request: Request[Any]): Option[JsObject] = obj
    }

    "returns NOT_FOUND if not all parameters are given" in {
      lazy val submissionDataController: SubmissionDataController = new Setup()
      val result = submissionDataController.getRetrieveSubmissionData()(Fixtures.buildFakeUser, FakeRequest(), hc)
      status(result) shouldBe NOT_FOUND
    }

    "returns OK if all parameters are given and retrieveSubmissionData is successful" in {
			reset(mockErsConnector)
			lazy val submissionDataController: SubmissionDataController = new Setup(Some(mock[JsObject]))

      when(mockErsConnector.retrieveSubmissionData(any[JsObject]())(any(), any()))
				.thenReturn(Future.successful(HttpResponse(OK, Some(Json.obj()))))

      val result = submissionDataController.getRetrieveSubmissionData()(Fixtures.buildFakeUser, FakeRequest(), hc)
      status(result) shouldBe OK
      bodyOf(result).contains("Retrieve Failure") shouldBe false
    }

    "shows error page if all parameters are given but retrieveSubmissionData fails" in {
			reset(mockErsConnector)
			lazy val submissionDataController: SubmissionDataController = new Setup(Some(mock[JsObject]))

			when(mockErsConnector.retrieveSubmissionData(any[JsObject]())(any(), any()))
				.thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

      val result = submissionDataController.getRetrieveSubmissionData()(Fixtures.buildFakeUser, FakeRequest(), hc)
      status(result) shouldBe OK
      bodyOf(result).contains(testMessages("ers.global_errors.message")) shouldBe true
    }

    "shows error page if all parameters are given but retrieveSubmissionData throws exception" in {
			reset(mockErsConnector)
			lazy val submissionDataController: SubmissionDataController = new Setup(Some(mock[JsObject]))

			when(mockErsConnector.retrieveSubmissionData(any[JsObject]())(any(), any()))
				.thenReturn(Future.failed(new RuntimeException))

      val result = submissionDataController.getRetrieveSubmissionData()(Fixtures.buildFakeUser, FakeRequest(), hc)
      status(result) shouldBe OK
      bodyOf(result).contains(testMessages("ers.global_errors.message")) shouldBe true
    }
  }
}
