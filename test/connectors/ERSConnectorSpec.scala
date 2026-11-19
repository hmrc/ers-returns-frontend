/*
 * Copyright 2026 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import controllers.auth.RequestWithOptionalAuthContext
import models.upscan.UploadedSuccessfully
import models.{ERSAuthData, ErsMetaData, ErsSummary, SchemeInfo}
import org.mockito.Mockito.reset as mreset
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContent
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{ErsTestHelper, UpscanData, WireMockHelper}

import java.time.Instant

class ERSConnectorSpec
    extends AnyWordSpecLike
    with Matchers
    with ErsTestHelper
    with WireMockHelper
    with UpscanData
    with GuiceOneAppPerSuite {

  given authContext: ERSAuthData  = defaultErsAuthData
  lazy val testHttp: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  override val requestWithAuth: RequestWithOptionalAuthContext[AnyContent] =
    RequestWithOptionalAuthContext(testFakeRequest.withSession("sessionId" -> "someSessionId"), defaultErsAuthData)

  lazy val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", Instant.now, "1", "2016", "EMI", "EMI")

  lazy val ersConnector: ErsConnector = new ErsConnector(testHttp, mockAppConfig) {
    override lazy val ersUrl       = server.baseUrl()
    override lazy val validatorUrl = s"http://localhost:${server.port()}/process-file"
  }

  val mockSchemeInfo: SchemeInfo = SchemeInfo("schemeRef", Instant.now, "1", "2020", "schemeType", "schemeName")
  val ersMetaData: ErsMetaData   = ErsMetaData(mockSchemeInfo, "Test", None, "None", None, None)

  val mockErsSummary: ErsSummary =
    ErsSummary("", "", None, Instant.now, ersMetaData, None, None, None, None, None, None, None, None)

  lazy val data: JsObject = Json.obj(
    "schemeRef" -> "XA1100000000000",
    "confTime"  -> "2016-08-05T11:14:43"
  )

  "validateFileData" should {

    "call file validator using empref from auth context" in {
      val expectedPath = urlEqualTo("/process-file/ers/123%2FABCDE/process-file")

      server.stubFor(
        post(expectedPath).willReturn(aResponse().withStatus(OK))
      )

      val result = await(
        ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc)
      )

      server.verify(1, postRequestedFor(expectedPath))
      result.status shouldBe OK
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

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc))
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

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc))
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

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc))
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

        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "validator throw Exception" in {

        val expectedPath = urlEqualTo("/process-file/ers/123%2FABCDE/process-file")

        server.stubFor(
          post(expectedPath)
            .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )

        val result = await(
          ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc)
        )

        server.verify(postRequestedFor(expectedPath))
        result.status shouldBe BAD_REQUEST
        result.body   shouldBe ""

      }
    }
  }

  "validateCsvFileData" should {
    "call file validator using empref from auth context" in {
      val expectedPath = urlEqualTo("/process-file/ers/123%2FABCDE/process-file")

      server.stubFor(
        post(expectedPath).willReturn(aResponse().withStatus(OK))
      )

      val result = await(
        ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc)
      )

      server.verify(1, postRequestedFor(expectedPath))
      result.status shouldBe OK
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
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(using requestWithAuth, hc))
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
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(using requestWithAuth, hc))
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
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(using requestWithAuth, hc))
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
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(using requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "validator throw Exception" in {
        mreset(mockHttp)

        server.stubFor(
          post(urlPathMatching("/(.*)/process-file"))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
            )
        )
        val result =
          await(ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(using requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }
    }
  }

  "calling retrieveSubmissionData" should {
    val path = urlEqualTo("/ers/123%2FABCDE/retrieve-submission-data")
    "successful retrieving" in {
      server.stubFor(
        post(path)
          .withRequestBody(equalToJson(Json.stringify(data)))
          .willReturn(aResponse().withStatus(OK))
      )

      val result = await(ersConnector.retrieveSubmissionData(data)(using requestWithAuth, hc))

      server.verify(postRequestedFor(path))
      result.status shouldBe OK
    }

    "failed retrieving" in {
      server.stubFor(
        post(path)
          .withRequestBody(equalToJson(Json.stringify(data)))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = await(ersConnector.retrieveSubmissionData(data)(using requestWithAuth, hc))

      server.verify(postRequestedFor(path))
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "throws exception" in {
      val expectedBody = equalToJson(Json.stringify(data))

      server.stubFor(
        post(path)
          .withRequestBody(expectedBody)
          .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
      )

      intercept[Exception] {
        await(ersConnector.retrieveSubmissionData(data)(using requestWithAuth, hc))
      }

      server.verify(postRequestedFor(path).withRequestBody(expectedBody))

    }
  }

  "getCallbackRecord" should {
    "return an UploadStatus" when {
      "the response is successful and the status is OK" in {
        val path         = urlEqualTo("/process-file/ers/someSessionId/get-callback")
        val expectedName = "fileName"
        val expectedUrl  = "downloadUrl"

        val json = Json.obj(
          "_type"       -> "UploadedSuccessfully",
          "name"        -> expectedName,
          "downloadUrl" -> expectedUrl
        )

        server.stubFor(
          get(path).willReturn(okJson(Json.stringify(json)))
        )

        val result = await(ersConnector.getCallbackRecord(using requestWithAuth, hc))

        server.verify(1, getRequestedFor(path))
        result shouldBe Some(UploadedSuccessfully(expectedName, expectedUrl, mimeType = None))
      }
    }

    "return None" when {
      "the response status is not OK" in {
        val result = await(ersConnector.getCallbackRecord(using requestWithAuth, hc))
        result shouldBe None
      }
    }

    "return None" when {
      "an exception occurs during the GET request" in {

        val result = await(ersConnector.getCallbackRecord(using requestWithAuth, hc))
        result shouldBe None
      }
    }
  }

  "updateCallbackRecord" should {
    "return NO_CONTENT" when {
      "update is successful" in {
        val sessionId    = "sessionId"
        val path         = urlEqualTo(s"/process-file/ers/$sessionId/update-callback")
        val uploadStatus = UploadedSuccessfully("fileId", "downloadUrl", mimeType = None)

        server.stubFor(
          put(path).willReturn(aResponse().withStatus(NO_CONTENT))
        )

        val result = await(ersConnector.updateCallbackRecord(uploadStatus, sessionId)(using hc))

        server.verify(
          1,
          putRequestedFor(path).withRequestBody(
            equalToJson("""{"_type":"UploadedSuccessfully","name":"fileId","downloadUrl":"downloadUrl"}""")
          )
        )
        result shouldBe NO_CONTENT
      }
    }

    "throw an exception" when {
      "response status is not NO_CONTENT" in {
        an[Exception] should be thrownBy await(
          ersConnector.updateCallbackRecord(
            UploadedSuccessfully("fileId", "downloadUrl", mimeType = None),
            "sessionId"
          )(using hc)
        )
      }
    }

    "throw an exception" when {
      "an exception occurs during the PUT request" in {
        an[Exception] should be thrownBy await(
          ersConnector.updateCallbackRecord(
            UploadedSuccessfully("fileId", "downloadUrl", mimeType = None),
            "sessionId"
          )(using hc)
        )
      }
    }
  }

  "createCallbackRecord" should {
    "return CREATED" when {
      val path = urlEqualTo("/process-file/ers/someSessionId/create-callback")
      "return CREATED when the callback record is created successfully" in {

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(CREATED))
        )

        val result = await(ersConnector.createCallbackRecord(using requestWithAuth, hc))
        server.verify(1, postRequestedFor(path))

        result shouldBe CREATED
      }
    }

    "throw an exception" when {
      "response status is not CREATED" in {
        an[Exception] should be thrownBy await(ersConnector.createCallbackRecord(using requestWithAuth, hc))
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        an[Exception] should be thrownBy await(ersConnector.createCallbackRecord(using requestWithAuth, hc))
      }
    }

    "throw an exception" when {
      "no session ID in the request" in {
        val requestWithAuthNoSession: RequestWithOptionalAuthContext[AnyContent] =
          RequestWithOptionalAuthContext(testFakeRequest, defaultErsAuthData)

        an[Exception] should be thrownBy await(
          ersConnector.createCallbackRecord(using requestWithAuthNoSession, hc)
        )
      }
    }
  }

  "retrieveSubmissionData" should {
    "return an HttpResponse" when {

      "the POST request is successful" in {
        val mockData = Json.obj("key" -> "value")
        val path     = urlEqualTo("/ers/123%2FABCDE/retrieve-submission-data")

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(OK))
        )

        val result = await(ersConnector.retrieveSubmissionData(mockData)(using requestWithAuth, hc))

        server.verify(
          1,
          postRequestedFor(path).withRequestBody(equalToJson(Json.stringify(mockData)))
        )
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {

      "response status is not OK" in {
        val mockData = Json.obj("key" -> "value")
        val path     = urlEqualTo("/ers/123%2FABCDE/retrieve-submission-data")

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        val result = await(
          ersConnector.retrieveSubmissionData(mockData)(using requestWithAuth, hc)
        )

        server.verify(
          1,
          postRequestedFor(path).withRequestBody(equalToJson(Json.stringify(mockData)))
        )
        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val mockData     = Json.obj("key" -> "value")
        val path         = urlEqualTo("/ers/123%2FABCDE/retrieve-submission-data")
        val expectedBody = equalToJson(Json.stringify(mockData))

        server.stubFor(
          post(path)
            .withRequestBody(expectedBody)
            .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )

        val failure = await(
          ersConnector.retrieveSubmissionData(mockData)(using requestWithAuth, hc).failed
        )

        server.verify(postRequestedFor(path).withRequestBody(expectedBody))
        failure shouldBe an[Exception]
      }
    }
  }

  "removePresubmissionData" should {
    "return an HttpResponse" when {

      "the POST request is successful" in {
        val path         = urlEqualTo("/ers/123%2FABCDE/removePresubmissionData")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockSchemeInfo)))

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(OK))
        )

        val result = await(
          ersConnector.removePresubmissionData(mockSchemeInfo)(using requestWithAuth, hc)
        )

        server.verify(1, postRequestedFor(path).withRequestBody(expectedBody))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {

      "response status is not OK" in {
        val path         = urlEqualTo("/ers/123%2FABCDE/removePresubmissionData")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockSchemeInfo)))

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        val result = await(
          ersConnector.removePresubmissionData(mockSchemeInfo)(using requestWithAuth, hc)
        )

        server.verify(1, postRequestedFor(path).withRequestBody(expectedBody))
        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {
      import com.github.tomakehurst.wiremock.http.Fault

      "an exception occurs during the POST request" in {
        val path         = urlEqualTo("/ers/123%2FABCDE/removePresubmissionData")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockSchemeInfo)))

        server.stubFor(
          post(path)
            .withRequestBody(expectedBody)
            .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )

        val failure = await(
          ersConnector.removePresubmissionData(mockSchemeInfo)(using requestWithAuth, hc).failed
        )

        server.verify(postRequestedFor(path).withRequestBody(expectedBody))
        failure shouldBe an[Exception]
      }
    }
  }

  "checkForPresubmission" should {
    "return an HttpResponse" when {
      "the POST request is successful" in {
        val validatedSheets = "sheet1,sheet2"
        val path            = urlEqualTo(s"/ers/123%2FABCDE/check-for-presubmission/$validatedSheets")
        val expectedBody    = equalToJson(Json.stringify(Json.toJson(mockSchemeInfo)))

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(OK))
        )

        val result = await(
          ersConnector.checkForPresubmission(mockSchemeInfo, validatedSheets)(using requestWithAuth, hc)
        )

        server.verify(1, postRequestedFor(path).withRequestBody(expectedBody))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {
      "response status is not OK" in {
        val validatedSheets = "sheet1,sheet2"
        val path            = urlEqualTo(s"/ers/123%2FABCDE/check-for-presubmission/$validatedSheets")
        val expectedBody    = equalToJson(Json.stringify(Json.toJson(mockSchemeInfo)))

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        val result = await(
          ersConnector.checkForPresubmission(mockSchemeInfo, validatedSheets)(using requestWithAuth, hc)
        )
        server.verify(1, postRequestedFor(path).withRequestBody(expectedBody))

        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {

      "an exception occurs during the POST request" in {
        val validatedSheets = "sheet1,sheet2"
        val path            = urlEqualTo(s"/ers/123%2FABCDE/check-for-presubmission/$validatedSheets")
        val expectedBody    = equalToJson(Json.stringify(Json.toJson(mockSchemeInfo)))
        server.stubFor(
          post(path)
            .withRequestBody(expectedBody)
            .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )

        val failure = await(
          ersConnector.checkForPresubmission(mockSchemeInfo, validatedSheets)(using requestWithAuth, hc).failed
        )

        server.verify(postRequestedFor(path).withRequestBody(expectedBody))
        failure shouldBe an[Exception]
      }
    }
  }

  "saveMetadata" should {
    "return an HttpResponse" when {
      "the POST request is successful" in {
        val path         = urlEqualTo("/ers/123%2FABCDE/saveMetadata")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockErsSummary)))

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(OK))
        )

        val result = await(
          ersConnector.saveMetadata(mockErsSummary)(using requestWithAuth, hc)
        )

        server.verify(1, postRequestedFor(path).withRequestBody(expectedBody))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {
      "response status is not OK" in {

        val path         = urlEqualTo("/ers/123%2FABCDE/saveMetadata")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockErsSummary)))

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        val result = await(
          ersConnector.saveMetadata(mockErsSummary)(using requestWithAuth, hc)
        )

        server.verify(1, postRequestedFor(path).withRequestBody(expectedBody))
        result.status shouldBe BAD_REQUEST
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val path         = urlEqualTo("/ers/123%2FABCDE/saveMetadata")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockErsSummary)))

        server.stubFor(
          post(path)
            .withRequestBody(expectedBody)
            .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )

        val failure = await(
          ersConnector.saveMetadata(mockErsSummary)(using requestWithAuth, hc).failed
        )

        server.verify(postRequestedFor(path).withRequestBody(expectedBody))
        failure shouldBe an[Exception]
      }
    }
  }

  "submitReturnToBackend" should {
    "return an HttpResponse" when {
      "the POST request is successful" in {
        val path         = urlEqualTo("/ers/123%2FABCDE/saveReturnData")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockErsSummary)))

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(OK))
        )

        val result = await(
          ersConnector.submitReturnToBackend(mockErsSummary)(using requestWithAuth, hc)
        )

        server.verify(1, postRequestedFor(path).withRequestBody(expectedBody))
        result.status shouldBe OK
      }
    }

    "return an error HttpResponse" when {
      "response status is not OK" in {
        val path         = urlEqualTo("/ers/123%2FABCDE/saveReturnData")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockErsSummary)))

        server.stubFor(
          post(path).willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        val result = await(
          ersConnector.submitReturnToBackend(mockErsSummary)(using requestWithAuth, hc)
        )

        server.verify(1, postRequestedFor(path).withRequestBody(expectedBody))
        result.status shouldBe BAD_REQUEST

      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val path         = urlEqualTo("/ers/123%2FABCDE/saveReturnData")
        val expectedBody = equalToJson(Json.stringify(Json.toJson(mockErsSummary)))

        server.stubFor(
          post(path)
            .withRequestBody(expectedBody)
            .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )

        val failure = await(
          ersConnector.submitReturnToBackend(mockErsSummary)(using requestWithAuth, hc).failed
        )

        server.verify(postRequestedFor(path).withRequestBody(expectedBody))
        failure shouldBe an[Exception]
      }
    }
  }

  "connectToEtmpSummarySubmit" should {
    "return a bundle reference number" when {
      "the POST request is successful and status is OK" in {
        val sap          = "sap123"
        val payload      = Json.obj("key" -> "value")
        val bundleRef    = "bundleRef123"
        val path         = urlEqualTo(s"/ers/123%2FABCDE/summarySubmit/$sap")
        val responseBody = Json.obj("Form Bundle Number" -> bundleRef)

        server.stubFor(
          post(path).willReturn(okJson(Json.stringify(responseBody)))
        )

        val result = await(
          ersConnector.connectToEtmpSummarySubmit(sap, payload)(using requestWithAuth, hc)
        )

        server.verify(1, postRequestedFor(path).withRequestBody(equalToJson(Json.stringify(payload))))
        result shouldBe bundleRef
      }
    }

    "throw an exception" when {
      "response status is not OK" in {
        val sap     = "sap123"
        val payload = Json.obj("key" -> "value")

        an[Exception] should be thrownBy await(
          ersConnector.connectToEtmpSummarySubmit(sap, payload)(using requestWithAuth, hc)
        )
      }
    }

    "throw an exception" when {
      "an exception occurs during the POST request" in {
        val sap     = "sap123"
        val payload = Json.obj("key" -> "value")

        an[Exception] should be thrownBy await(
          ersConnector.connectToEtmpSummarySubmit(sap, payload)(using requestWithAuth, hc)
        )
      }
    }
  }

  "connectToEtmpSapRequest" should {
    "return a SAP number" when {
      "the GET request is successful and status is OK" in {
        val schemeRef    = "scheme123"
        val sapNumber    = "sapNumber123"
        val path         = urlEqualTo(s"/ers/123%2FABCDE/sapRequest/$schemeRef")
        val responseBody = Json.obj("SAP Number" -> sapNumber)

        server.stubFor(
          get(path).willReturn(okJson(Json.stringify(responseBody)))
        )

        val result = await(
          ersConnector.connectToEtmpSapRequest(schemeRef)(using requestWithAuth, hc)
        )

        server.verify(1, getRequestedFor(path))
        result shouldBe Right(sapNumber)
      }
    }

    "throw an exception" when {
      "response status is not OK" in {
        val schemeRef = "scheme123"

        an[Exception] should be thrownBy await(
          ersConnector.connectToEtmpSapRequest(schemeRef)(using requestWithAuth, hc)
        )
      }
    }

    "throw an exception" when {
      "an exception occurs during the GET request" in {
        val schemeRef = "scheme123"

        an[Exception] should be thrownBy await(
          ersConnector.connectToEtmpSapRequest(schemeRef)(using requestWithAuth, hc)
        )
      }
    }
  }

}
