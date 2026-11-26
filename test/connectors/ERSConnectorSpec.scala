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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContent
import play.api.test.Helpers.*
import utils.{ErsTestHelper, UpscanData, WireMockHelper}

import java.time.Instant

class ERSConnectorSpec
    extends AnyWordSpecLike
    with Matchers
    with ErsTestHelper
    with UpscanData
    with GuiceOneAppPerSuite
    with WireMockHelper {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.ers-returns.port"        -> server.port(),
        "microservice.services.ers-file-validator.port" -> server.port()
      )
      .build()

  lazy val ersConnector: ErsConnector = app.injector.instanceOf[ErsConnector]

  given authContext: ERSAuthData = defaultErsAuthData

  override val requestWithAuth: RequestWithOptionalAuthContext[AnyContent] =
    RequestWithOptionalAuthContext(testFakeRequest.withSession("sessionId" -> "someSessionId"), defaultErsAuthData)

  lazy val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", Instant.now, "1", "2016", "EMI", "EMI")

  val mockSchemeInfo: SchemeInfo = SchemeInfo("schemeRef", Instant.now, "1", "2020", "schemeType", "schemeName")
  val ersMetaData: ErsMetaData   = ErsMetaData(mockSchemeInfo, "Test", None, "None", None, None)

  val mockErsSummary: ErsSummary =
    ErsSummary("", "", None, Instant.now, ersMetaData, None, None, None, None, None, None, None, None)

  lazy val data: JsObject = Json.obj(
    "schemeRef" -> "XA1100000000000",
    "confTime"  -> "2016-08-05T11:14:43"
  )

  "validateFileData" should {
    val expectedPath = urlEqualTo("/ers/123%2FABCDE/process-file")
    "call file validator using empref from auth context" in {
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
          post(expectedPath)
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
          post(expectedPath)
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
          post(expectedPath)
            .willReturn(
              aResponse()
                .withStatus(UNAUTHORIZED)
            )
        )
        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
        result.body   shouldBe ""
      }

      "file-validator returns 5xx" in {
        server.stubFor(
          post(expectedPath)
            .willReturn(
              aResponse()
                .withStatus(NOT_IMPLEMENTED)
            )
        )
        val result = await(ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc))
        result.status shouldBe BAD_REQUEST
      }

      "validator throw Exception" in {
        server.stubFor(
          post(expectedPath)
            .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )
        val result = await(
          ersConnector.validateFileData(uploadedSuccessfully, schemeInfo)(using requestWithAuth, hc)
        )
        server.verify(postRequestedFor(expectedPath))
        result.status shouldBe BAD_REQUEST
      }
    }
  }

  "validateCsvFileData" should {
    val process_CSV_URL = urlEqualTo("/ers/v2/123%2FABCDE/process-csv-file")
    "call file validator using empref from auth context" in {
      server.stubFor(
        post(process_CSV_URL).willReturn(aResponse().withStatus(OK))
      )
      val result = await(
        ersConnector.validateCsvFileData(
          List(uploadedSuccessfully),
          schemeInfo
        )(using requestWithAuth, hc)
      )
      server.verify(1, postRequestedFor(process_CSV_URL))
      result.status shouldBe OK
    }

    "return the response from file-validator" when {
      "response code is 200" in {
        server.stubFor(
          post(process_CSV_URL)
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
          post(process_CSV_URL)
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
          post(process_CSV_URL)
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
          post(process_CSV_URL)
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
        server.stubFor(
          post(process_CSV_URL)
            .willReturn(
              aResponse()
                .withFault(Fault.EMPTY_RESPONSE)
            )
        )
        val result = await(
          ersConnector.validateCsvFileData(List(uploadedSuccessfully), schemeInfo)(using requestWithAuth, hc)
        )
        server.verify(postRequestedFor(process_CSV_URL))
        result.status shouldBe BAD_REQUEST
      }
    }
  }

  "getCallbackRecord" should {
    val path = urlEqualTo("/ers/someSessionId/get-callback")
    "return an UploadStatus" when {
      "the response is successful and the status is OK" in {
        val expectedName = "fileName"
        val expectedUrl  = "downloadUrl"
        val json         = Json.obj(
          "_type"       -> "UploadedSuccessfully",
          "name"        -> expectedName,
          "downloadUrl" -> expectedUrl
        )
        server.stubFor(
          get(path).willReturn(okJson(Json.stringify(json)))
        )
        val result       = await(ersConnector.getCallbackRecord(using requestWithAuth, hc))
        server.verify(1, getRequestedFor(path))
        result shouldBe Some(UploadedSuccessfully(expectedName, expectedUrl, mimeType = None))
      }
    }

    "return None" when {
      "the response status is not OK" in {
        server.stubFor(
          get(path)
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )
        val result = await(
          ersConnector.getCallbackRecord(using requestWithAuth, hc)
        )
        server.verify(getRequestedFor(path))
        result shouldBe None
      }

      "an exception occurs during the GET request" in {
        server.stubFor(
          get(path)
            .willReturn(
              aResponse()
                .withFault(Fault.EMPTY_RESPONSE)
            )
        )
        val result = await(
          ersConnector.getCallbackRecord(using requestWithAuth, hc)
        )
        server.verify(getRequestedFor(path))
        result shouldBe None
      }
    }
  }

  "updateCallbackRecord" should {
    val sessionId = "sessionId"
    val path      = urlEqualTo(s"/ers/$sessionId/update-callback")

    "return NO_CONTENT" when {
      "update is successful" in {
        val uploadStatus = UploadedSuccessfully("fileId", "downloadUrl", mimeType = None)
        server.stubFor(
          put(path).willReturn(aResponse().withStatus(NO_CONTENT))
        )
        val result       = await(ersConnector.updateCallbackRecord(uploadStatus, sessionId)(using hc))
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
        server.stubFor(
          put(path).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )
        an[Exception] should be thrownBy await(
          ersConnector.updateCallbackRecord(
            UploadedSuccessfully("fileId", "downloadUrl", mimeType = None),
            sessionId
          )(using hc)
        )
        server.verify(putRequestedFor(path))
      }

      "an exception occurs during the PUT request" in {
        server.stubFor(
          put(path).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )
        an[Exception] should be thrownBy await(
          ersConnector.updateCallbackRecord(
            UploadedSuccessfully("fileId", "downloadUrl", mimeType = None),
            sessionId
          )(using hc)
        )
        server.verify(putRequestedFor(path))
      }
    }
  }

  "createCallbackRecord" should {
    val path = urlEqualTo("/ers/someSessionId/create-callback")
    "return CREATED" when {
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
        server.stubFor(
          post(path).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )
        an[Exception] should be thrownBy await(
          ersConnector.createCallbackRecord(using requestWithAuth, hc)
        )
        server.verify(postRequestedFor(path))
      }

      "an exception occurs during the POST request" in {
        server.stubFor(
          post(path).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )
        an[Exception] should be thrownBy await(
          ersConnector.createCallbackRecord(using requestWithAuth, hc)
        )
        server.verify(postRequestedFor(path))
      }

      "no session ID in the request" in {
        val requestWithAuthNoSession = RequestWithOptionalAuthContext(testFakeRequest, defaultErsAuthData)
        an[Exception] should be thrownBy await(
          ersConnector.createCallbackRecord(using requestWithAuthNoSession, hc)
        )
        server.verify(0, postRequestedFor(path))
      }
    }

  }

  "retrieveSubmissionData" should {
    val path         = urlEqualTo("/ers/123%2FABCDE/retrieve-submission-data")
    val expectedBody = equalToJson(Json.stringify(data))
    "return OK when the POST request is successful" in {
      server.stubFor(
        post(path)
          .withRequestBody(expectedBody)
          .willReturn(aResponse().withStatus(OK))
      )
      val result = await(
        ersConnector.retrieveSubmissionData(data)(using requestWithAuth, hc)
      )
      server.verify(postRequestedFor(path).withRequestBody(expectedBody))
      result.status shouldBe OK
    }

    "return BAD_REQUEST when the POST request returns 400" in {
      server.stubFor(
        post(path)
          .withRequestBody(expectedBody)
          .willReturn(aResponse().withStatus(BAD_REQUEST))
      )
      val result = await(
        ersConnector.retrieveSubmissionData(data)(using requestWithAuth, hc)
      )
      server.verify(postRequestedFor(path).withRequestBody(expectedBody))
      result.status shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR when the POST request returns 500" in {
      server.stubFor(
        post(path)
          .withRequestBody(expectedBody)
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )
      val result = await(
        ersConnector.retrieveSubmissionData(data)(using requestWithAuth, hc)
      )
      server.verify(postRequestedFor(path).withRequestBody(expectedBody))
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "throw an exception when an exception occurs during the POST request" in {
      server.stubFor(
        post(path)
          .withRequestBody(expectedBody)
          .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
      )
      val failure = await(
        ersConnector.retrieveSubmissionData(data)(using requestWithAuth, hc).failed
      )
      server.verify(postRequestedFor(path).withRequestBody(expectedBody))
      failure shouldBe an[Exception]
    }
  }

  "removePresubmissionData" should {
    val path         = urlEqualTo("/ers/123%2FABCDE/removePresubmissionData")
    val expectedBody = equalToJson(Json.stringify(Json.toJson(mockSchemeInfo)))
    "return an HttpResponse" when {
      "the POST request is successful" in {
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
      "an exception occurs during the POST request" in {
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
    val validatedSheets = "sheet1,sheet2"
    val path            = urlEqualTo(s"/ers/123%2FABCDE/check-for-presubmission/$validatedSheets")
    val expectedBody    = equalToJson(Json.stringify(Json.toJson(mockSchemeInfo)))

    "return an HttpResponse" when {
      "the POST request is successful" in {
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
    val path         = urlEqualTo("/ers/123%2FABCDE/saveMetadata")
    val expectedBody = equalToJson(Json.stringify(Json.toJson(mockErsSummary)))
    "return an HttpResponse" when {
      "the POST request is successful" in {
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
    val path         = urlEqualTo("/ers/123%2FABCDE/saveReturnData")
    val expectedBody = equalToJson(Json.stringify(Json.toJson(mockErsSummary)))
    "return an HttpResponse" when {
      "the POST request is successful" in {
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
    val sap       = "sap123"
    val payload   = Json.obj("key" -> "value")
    val bundleRef = "bundleRef123"
    val path      = urlEqualTo(s"/ers/123%2FABCDE/summarySubmit/$sap")
    "return a bundle reference number" when {
      "the POST request is successful and status is OK" in {
        val responseBody = Json.obj("Form Bundle Number" -> bundleRef)
        server.stubFor(
          post(path).willReturn(okJson(Json.stringify(responseBody)))
        )
        val result       = await(
          ersConnector.connectToEtmpSummarySubmit(sap, payload)(using requestWithAuth, hc)
        )
        server.verify(1, postRequestedFor(path).withRequestBody(equalToJson(Json.stringify(payload))))
        result shouldBe bundleRef
      }
    }

    "throw an exception" when {
      "response status is not OK" in {
        server.stubFor(
          post(path).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )
        an[Exception] should be thrownBy await(
          ersConnector.connectToEtmpSummarySubmit(
            sap,
            payload
          )(using requestWithAuth, hc)
        )
        server.verify(
          postRequestedFor(path)
            .withRequestBody(equalToJson(Json.stringify(payload)))
        )
      }

      "an exception occurs during the POST request" in {
        server.stubFor(
          post(path).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )
        an[Exception] should be thrownBy await(
          ersConnector.connectToEtmpSummarySubmit(
            sap,
            payload
          )(using requestWithAuth, hc)
        )
        server.verify(
          postRequestedFor(path)
            .withRequestBody(equalToJson(Json.stringify(payload)))
        )
      }
    }
  }

  "connectToEtmpSapRequest" should {
    val schemeRef    = "scheme123"
    val sapNumber    = "sapNumber123"
    val path         = urlEqualTo(s"/ers/123%2FABCDE/sapRequest/$schemeRef")
    val responseBody = Json.obj("SAP Number" -> sapNumber)
    "return a SAP number" when {
      "the GET request is successful and status is OK" in {
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
        server.stubFor(
          get(path).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )
        an[Exception] should be thrownBy await(
          ersConnector.connectToEtmpSapRequest(
            schemeRef
          )(using requestWithAuth, hc)
        )
        server.verify(getRequestedFor(path))
      }

      "an exception occurs during the GET request" in {
        server.stubFor(
          get(path).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        )
        an[Exception] should be thrownBy await(
          ersConnector.connectToEtmpSapRequest(
            schemeRef
          )(using requestWithAuth, hc)
        )
        server.verify(getRequestedFor(path))
      }
    }
  }

}
