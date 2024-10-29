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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import controllers.auth.RequestWithOptionalAuthContext
import models.upscan.UploadedSuccessfully
import models.{ERSAuthData, ErsSummary, SchemeInfo}
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset => mreset, _}
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
import uk.gov.hmrc.play.bootstrap.http.HttpClientV2Provider
import utils.{ERSFakeApplicationConfig, ErsTestHelper, UpscanData, WireMockHelper}

import java.time.ZonedDateTime
import scala.concurrent.{ExecutionContext, Future}

class ERSConnectorSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with MockitoSugar
    with ERSFakeApplicationConfig
    with ErsTestHelper
    with WireMockHelper
    with UpscanData
    with GuiceOneAppPerSuite {

  override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  lazy val newConfig: Map[String, Any] = config + ("microservice.services.ers-file-validator.port" -> server.port())
  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)
  implicit lazy val mat: Materializer = app.materializer
  implicit lazy val authContext: ERSAuthData = defaultErsAuthData
  lazy val testHttp: HttpClientV2Provider = app.injector.instanceOf[HttpClientV2Provider]
  override val requestWithAuth: RequestWithOptionalAuthContext[AnyContent] =
    RequestWithOptionalAuthContext(testFakeRequest.withSession("sessionId" -> "someSessionId"), defaultErsAuthData)
  lazy val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", ZonedDateTime.now, "1", "2016", "EMI", "EMI")

  lazy val ersConnector: ErsConnector = new ErsConnector(testHttp, mockAppConfig) {
    override lazy val ersUrl = "ers-returns"
    override lazy val validatorUrl = s"http://localhost:${server.port()}/process-file"
  }

  lazy val ersConnectorMockHttp: ErsConnector = new ErsConnector(mockHttp, mockAppConfig) {
    override lazy val ersUrl = "ers-returns"
    override lazy val validatorUrl = "ers-file-validator"
  }

  lazy val data: JsObject = Json.obj(
    "schemeRef" -> "XA1100000000000",
    "confTime"  -> "2016-08-05T11:14:43"
  )

  "validateFileData" should {
    "call file validator using empref from auth context" in {

      mreset(mockHttp)
      val stringCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val result = await(ersConnectorMockHttp.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
      result.status shouldBe OK
      stringCaptor.getValue should include("123%2FABCDE")
    }

    "return the response from file-validator" when {
      "response code is 200" in {
        server.stubFor(
          post(urlPathMatching("/(.*)/process-file"))
            .willReturn(
              aResponse()
                .withStatus(OK)
            )
        )

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
        result.status shouldBe OK
      }

      "response code is 202" in {
        server.stubFor(
          post(urlPathMatching("/(.*)/process-file"))
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
        server.stubFor(
          post(urlPathMatching("/(.*)/process-file"))
            .willReturn(
              aResponse()
                .withStatus(UNAUTHORIZED)
            )
        )

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "file-validator returns 5xx" in {
        server.stubFor(
          post(urlPathMatching("/(.*)/process-file"))
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
        when(mockHttp.get().post(any()) (any()).execute)
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
      when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(OK, "")))
      val result                               = await(ersConnectorMockHttp.validateFileData(uploadedSuccessfully, schemeInfo)(requestWithAuth, hc))
      result.status       shouldBe OK
      stringCaptor.getValue should include("123%2FABCDE")
    }

    "return the response from file-validator" when {
      "response code is 200" in {
        server.stubFor(
          post(urlPathMatching("/(.*)/process-csv-file"))
            .willReturn(
              aResponse()
                .withStatus(OK)
            )
        )

        val result =
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe OK
      }

      "response code is 202" in {
        server.stubFor(
          post(urlPathMatching("/(.*)/process-csv-file"))
            .willReturn(
              aResponse()
                .withStatus(ACCEPTED)
            )
        )

        val result =
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe ACCEPTED
      }
    }

    "return blank Bad Request" when {
      "file-validator returns 4xx" in {
        server.stubFor(
          post(urlPathMatching("/(.*)/process-csv-file"))
            .willReturn(
              aResponse()
                .withStatus(UNAUTHORIZED)
            )
        )

        val result =
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "file-validator returns 5xx" in {
        server.stubFor(
          post(urlPathMatching("/(.*)/process-csv-file"))
            .willReturn(
              aResponse()
                .withStatus(NOT_IMPLEMENTED)
            )
        )

        val result =
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "validator throw Exception" in {
        mreset(mockHttp)
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))
        val result =
          await(ersConnectorMockHttp.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }
  }

  "calling retrieveSubmissionData" should {
    "successful retrieving" in {
      mreset(mockHttp)
      when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val result = await(ersConnectorMockHttp.retrieveSubmissionData(data)(requestWithAuth, hc))
      result.status shouldBe OK
    }

    "failed retrieving" in {
      mreset(mockHttp)
      when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

      val result = await(ersConnectorMockHttp.retrieveSubmissionData(data)(requestWithAuth, hc))
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "throws exception" in {
      mreset(mockHttp)
      when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
        .thenReturn(Future.failed(new RuntimeException))

      intercept[Exception] {
        await(ersConnector.retrieveSubmissionData(data)(requestWithAuth, hc))
      }
    }
  }

  "getCallbackRecord" should {
    "return an UploadStatus" when {
      "the response is successful and the status is OK" in {
        val expectedName = "fileName"
        val expectedUrl = "downloadUrl"
        val json = s"""{"_type": "UploadedSuccessfully", "name": "$expectedName", "downloadUrl": "$expectedUrl"}"""
        val successfulResponse = HttpResponse(OK, json, Map.empty)

        when(mockHttp.get().get(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(successfulResponse))

        val result = await(ersConnectorMockHttp.getCallbackRecord(requestWithAuth, hc))
        result shouldBe Some(UploadedSuccessfully("fileName", "downloadUrl"))
      }
    }

    "return None" when {
      "the response status is not OK" in {
        when(mockHttp.get().get(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

        val result = await(ersConnectorMockHttp.getCallbackRecord(requestWithAuth, hc))
        result shouldBe None
      }
    }

    "return None" when {
      "an exception occurs during the GET request" in {
        when(mockHttp.get().get(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        val result = await(ersConnectorMockHttp.getCallbackRecord(requestWithAuth, hc))
        result shouldBe None
      }
    }
  }

  "updateCallbackRecord" should {
    "return NO_CONTENT" when {
      "update is successful" in {
        when(mockHttp.get().put(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

        val result = await(ersConnectorMockHttp.updateCallbackRecord(UploadedSuccessfully("fileId", "downloadUrl"), "sessionId")(hc))
        result shouldBe NO_CONTENT
      }
    }

    "throw an exception" when {
      "response status is not NO_CONTENT" in {
        when(mockHttp.get().put(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.updateCallbackRecord(UploadedSuccessfully("fileId", "downloadUrl"), "sessionId")(hc))
      }
    }

    "throw an exception" when {
      "an exception occurs during the PUT request" in {
        when(mockHttp.get().put(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.updateCallbackRecord(UploadedSuccessfully("fileId", "downloadUrl"), "sessionId")(hc))
      }
    }
  }

  "createCallbackRecord" should {
    "return CREATED" when {
      "callback record is created successfully" in {
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(CREATED, "")))

        val result = await(ersConnectorMockHttp.createCallbackRecord(requestWithAuth, hc))
        result shouldBe CREATED
      }
    }

    "throw an exception" when {
      "response status is not CREATED" in {
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.createCallbackRecord(requestWithAuth, hc))
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.createCallbackRecord(requestWithAuth, hc))
      }
    }

    "throw an exception" when {
      "no session ID in the request" in {
        val requestWithAuthNoSession: RequestWithOptionalAuthContext[AnyContent] =
          RequestWithOptionalAuthContext(testFakeRequest, defaultErsAuthData)
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.createCallbackRecord(requestWithAuthNoSession, hc))
      }
    }
  }

  "retrieveSubmissionData" should {
    "return an HttpResponse" when {
      "the POST request is successful" in {
        val mockData: JsObject = Json.obj("key" -> "value")
        val successfulResponse = HttpResponse(OK, "")
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(successfulResponse))

        val result = await(ersConnectorMockHttp.retrieveSubmissionData(mockData)(requestWithAuth, hc))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {
      "response status is not OK" in {
        val mockData: JsObject = Json.obj("key" -> "value")
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val result = await(ersConnectorMockHttp.retrieveSubmissionData(mockData)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val mockData: JsObject = Json.obj("key" -> "value")
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.retrieveSubmissionData(mockData)(requestWithAuth, hc))
      }
    }
  }

  "removePresubmissionData" should {
    "return an HttpResponse" when {
      "the POST request is successful" in {
        val mockSchemeInfo: SchemeInfo = SchemeInfo("schemeRef", ZonedDateTime.now, "1", "2020", "schemeType", "schemeName")
        val successfulResponse = HttpResponse(OK, "")
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(successfulResponse))

        val result = await(ersConnectorMockHttp.removePresubmissionData(mockSchemeInfo)(requestWithAuth, hc))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {
      "response status is not OK" in {
        val mockSchemeInfo: SchemeInfo = SchemeInfo("schemeRef", ZonedDateTime.now, "1", "2020", "schemeType", "schemeName")
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val result = await(ersConnectorMockHttp.removePresubmissionData(mockSchemeInfo)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val mockSchemeInfo: SchemeInfo = SchemeInfo("schemeRef", ZonedDateTime.now, "1", "2020", "schemeType", "schemeName")
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.removePresubmissionData(mockSchemeInfo)(requestWithAuth, hc))
      }
    }
  }

  "checkForPresubmission" should {
    "return an HttpResponse" when {
      "the POST request is successful" in {
        val mockSchemeInfo: SchemeInfo = SchemeInfo("schemeRef", ZonedDateTime.now, "1", "2020", "schemeType", "schemeName")
        val validatedSheets = "sheet1,sheet2"
        val successfulResponse = HttpResponse(OK, "")

        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(successfulResponse))

        val result = await(ersConnectorMockHttp.checkForPresubmission(mockSchemeInfo, validatedSheets)(requestWithAuth, hc))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {
      "response status is not OK" in {
        val mockSchemeInfo: SchemeInfo = SchemeInfo("schemeRef", ZonedDateTime.now, "1", "2020", "schemeType", "schemeName")
        val validatedSheets = "sheet1,sheet2"
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val result = await(ersConnectorMockHttp.checkForPresubmission(mockSchemeInfo, validatedSheets)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val mockSchemeInfo: SchemeInfo = SchemeInfo("schemeRef", ZonedDateTime.now, "1", "2020", "schemeType", "schemeName")
        val validatedSheets = "sheet1,sheet2"
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.checkForPresubmission(mockSchemeInfo, validatedSheets)(requestWithAuth, hc))
      }
    }
  }

  "saveMetadata" should {
    "return an HttpResponse" when {
      "the POST request is successful" in {
        val mockErsSummary: ErsSummary = mock[ErsSummary]
        val successfulResponse = HttpResponse(OK, "")
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(successfulResponse))

        val result = await(ersConnectorMockHttp.saveMetadata(mockErsSummary)(requestWithAuth, hc))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {
      "response status is not OK" in {
        val mockErsSummary: ErsSummary = mock[ErsSummary]
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
            .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val result = await(ersConnectorMockHttp.saveMetadata(mockErsSummary)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val mockErsSummary: ErsSummary = mock[ErsSummary]
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
            .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.saveMetadata(mockErsSummary)(requestWithAuth, hc))
      }
    }
  }

  "submitReturnToBackend" should {
    "return an HttpResponse" when {
      "the POST request is successful" in {
        val mockErsSummary: ErsSummary = mock[ErsSummary]
        val successfulResponse = HttpResponse(OK, "")
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(successfulResponse))

        val result = await(ersConnectorMockHttp.submitReturnToBackend(mockErsSummary)(requestWithAuth, hc))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {
      "response status is not OK" in {
        val mockErsSummary: ErsSummary = mock[ErsSummary]
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
            .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val result = await(ersConnectorMockHttp.submitReturnToBackend(mockErsSummary)(requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val mockErsSummary: ErsSummary = mock[ErsSummary]
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
            .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.submitReturnToBackend(mockErsSummary)(requestWithAuth, hc))
      }
    }
  }

  "connectToEtmpSummarySubmit" should {
    "return a bundle reference number" when {
      "the POST request is successful and status is OK" in {
        val sap = "sap123"
        val payload = Json.obj("key" -> "value")
        val bundleRef = "bundleRef123"
        val successfulResponse = HttpResponse(OK, Json.obj("Form Bundle Number" -> bundleRef).toString())
        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(successfulResponse))

        val result = await(ersConnectorMockHttp.connectToEtmpSummarySubmit(sap, payload)(requestWithAuth, hc))
        result shouldBe bundleRef
      }
    }

    "throw an exception" when {
      "response status is not OK" in {
        val sap = "sap123"
        val payload = Json.obj("key" -> "value")

        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.connectToEtmpSummarySubmit(sap, payload)(requestWithAuth, hc))
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val sap = "sap123"
        val payload = Json.obj("key" -> "value")

        when(mockHttp.get().post(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.connectToEtmpSummarySubmit(sap, payload)(requestWithAuth, hc))
      }
    }
  }

  "connectToEtmpSapRequest" should {
    "return a SAP number" when {
      "the GET request is successful and status is OK" in {
        val schemeRef = "scheme123"
        val sapNumber = "sapNumber123"
        val successfulResponse = HttpResponse(OK, Json.obj("SAP Number" -> sapNumber).toString())

        when(mockHttp.get().get(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(successfulResponse))

        val result = await(ersConnectorMockHttp.connectToEtmpSapRequest(schemeRef)(requestWithAuth, hc))
        result shouldBe sapNumber
      }
    }

    "throw an exception" when {
      "response status is not OK" in {
        val schemeRef = "scheme123"
        when(mockHttp.get().get(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.connectToEtmpSapRequest(schemeRef)(requestWithAuth, hc))
      }
    }

    "throw an exception" when {
      "an exception occurs during the GET request" in {
        val schemeRef = "scheme123"
        when(mockHttp.get().get(any()) (any()).execute[HttpResponse])
          .thenReturn(Future.failed(new Exception("Test exception")))

        an[Exception] should be thrownBy await(ersConnectorMockHttp.connectToEtmpSapRequest(schemeRef)(requestWithAuth, hc))
      }
    }
  }
}
