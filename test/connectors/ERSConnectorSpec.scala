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

package connectors

import akka.stream.Materializer
import com.github.tomakehurst.wiremock.client.WireMock._
import metrics.Metrics
import models.{ERSAuthData, SchemeInfo, ValidatorData}
import org.joda.time.DateTime
import org.mockito.Mockito.{reset => mreset, _}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.{ERSFakeApplicationConfig, ErsTestHelper, UpscanData, WireMockHelper}

import scala.concurrent.{ExecutionContext, Future}

class ERSConnectorSpec extends AnyWordSpecLike with Matchers with OptionValues
												with MockitoSugar
												with ERSFakeApplicationConfig
												with ErsTestHelper
												with WireMockHelper
												with UpscanData
                        with GuiceOneAppPerSuite {

  lazy val newConfig: Map[String, Any] = config + ("microservice.services.ers-file-validator.port" -> server.port())
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
  override implicit val ec: ExecutionContext = mockMCC.executionContext
  implicit lazy val authContext: ERSAuthData = defaultErsAuthData
	lazy val testHttp: DefaultHttpClient = app.injector.instanceOf[DefaultHttpClient]

  lazy val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "EMI", "EMI")

  lazy val ersConnector: ErsConnector = new ErsConnector(testHttp, mockErsUtil, mockAppConfig) {
    override lazy val metrics: Metrics = mockMetrics
    override lazy val ersUrl = "ers-returns"
    override lazy val validatorUrl = s"http://localhost:${server.port()}/process-file"
  }

	lazy val ersConnectorMockHttp: ErsConnector = new ErsConnector(mockHttp, mockErsUtil, mockAppConfig) {
		override lazy val metrics: Metrics = mockMetrics
		override lazy val ersUrl = "ers-returns"
		override lazy val validatorUrl = "ers-file-validator"
	}

  lazy val data: JsObject = Json.obj(
    "schemeRef" -> "XA1100000000000",
    "confTime" -> "2016-08-05T11:14:43"
  )

  "validateFileData" should {
    "call file validator using empref from auth context" in {

      mreset(mockHttp)
      val stringCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      when(mockHttp.POST[ValidatorData, HttpResponse](stringCaptor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any())
				(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val result = await(ersConnectorMockHttp.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
      result.status shouldBe OK
      stringCaptor.getValue should include("123%2FABCDE")
    }

    "return the response from file-validator" when {
      "response code is 200" in {
				server.stubFor(post(urlPathMatching("/(.*)/process-file"))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
        )

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
        result.status shouldBe OK
      }

      "response code is 202" in {
        server.stubFor(post(urlPathMatching("/(.*)/process-file"))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
        )

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
        result.status shouldBe ACCEPTED
      }
    }

    "return blank Bad Request" when {
      "file-validator returns 4xx" in {
        server.stubFor(post(urlPathMatching("/(.*)/process-file"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
        )

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "file-validator returns 5xx" in {
        server.stubFor(post(urlPathMatching("/(.*)/process-file"))
          .willReturn(
            aResponse()
              .withStatus(NOT_IMPLEMENTED)
          )
        )

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "validator throw Exception" in {
        mreset(mockHttp)
        when(mockHttp.POST[ValidatorData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
					(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Exception("Test exception")))
        val result = await(ersConnectorMockHttp.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }
  }

  "validateCsvFileData" should {
    "call file validator using empref from auth context" in {
      mreset(mockHttp)
      val stringCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      when(mockHttp.POST[ValidatorData, HttpResponse](stringCaptor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any())
				(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))
      val result = await(ersConnectorMockHttp.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
      result.status shouldBe OK
      stringCaptor.getValue should include("123%2FABCDE")
    }

    "return the response from file-validator" when {
      "response code is 200" in {
        server.stubFor(post(urlPathMatching("/(.*)/process-csv-file"))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
        )

        val result = await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe OK
      }

      "response code is 202" in {
        server.stubFor(post(urlPathMatching("/(.*)/process-csv-file"))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
        )

        val result = await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe ACCEPTED
      }
    }

    "return blank Bad Request" when {
      "file-validator returns 4xx" in {
        server.stubFor(post(urlPathMatching("/(.*)/process-csv-file"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
        )

        val result = await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "file-validator returns 5xx" in {
        server.stubFor(post(urlPathMatching("/(.*)/process-csv-file"))
          .willReturn(
            aResponse()
              .withStatus(NOT_IMPLEMENTED)
          )
        )

        val result = await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "validator throw Exception" in {
        mreset(mockHttp)
        when(mockHttp.POST[ValidatorData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
					(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Exception("Test exception")))
        val result = await(ersConnectorMockHttp.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }
  }


  "calling retrieveSubmissionData" should {

    "successful retrieving" in {
      mreset(mockHttp)
      when(
        mockHttp.POST[SchemeInfo, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
					(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      ).thenReturn(
        Future.successful(HttpResponse(OK, ""))
      )

      val result = await(ersConnectorMockHttp.retrieveSubmissionData(data)(requestWithAuth, hc))
      result.status shouldBe OK
    }

    "failed retrieving" in {
      mreset(mockHttp)
      when(
        mockHttp.POST[SchemeInfo, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
					(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      ).thenReturn(
        Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, ""))
      )

      val result = await(ersConnectorMockHttp.retrieveSubmissionData(data)(requestWithAuth, hc))
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "throws exception" in {
      mreset(mockHttp)
      when(
        mockHttp.POST[SchemeInfo, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
					(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )

      intercept[Exception] {
        await(ersConnector.retrieveSubmissionData(data)(requestWithAuth, hc))
      }
    }

  }
}
