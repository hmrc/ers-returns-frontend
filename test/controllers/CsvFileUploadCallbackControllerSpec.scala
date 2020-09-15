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

import java.net.URL
import java.time.Instant

import akka.stream.Materializer
import helpers.ErsTestHelper
import models.upscan._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Environment}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec
import utils.{ERSFakeApplicationConfig, UpscanData}

import scala.concurrent.Future

class CsvFileUploadCallbackControllerSpec extends UnitSpec
	with ERSFakeApplicationConfig with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach with UpscanData with ErsTestHelper {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val materializer: Materializer = app.materializer
  lazy val environment: Environment = app.injector.instanceOf[Environment]
	lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  def request(body: JsValue): FakeRequest[JsValue] = FakeRequest().withBody(body)
  val scRef = "scRef"
  val url: URL = new URL("http://localhost:9000/myUrl")

  lazy val csvFileUploadCallbackController: CsvFileUploadCallbackController =
		new CsvFileUploadCallbackController(messagesApi, mockErsConnector, mockAuthConnector, mockErsUtil, mockAppConfig) {
		import scala.concurrent.duration._
    when(mockAppConfig.retryDelay).thenReturn(1 millisecond)
  }

  override def beforeEach(): Unit = {
    reset(mockErsUtil)
		when(mockErsUtil.CHECK_CSV_FILES).thenReturn("check-csv-files")
		super.beforeEach()
  }

  "callback" should {
    val uploadId: UploadId = UploadId("ID")
    "update upload status to Uploaded Successfully" when {
      "callback is UpscanReadyCallback" in {
        val callbackCaptor = ArgumentCaptor.forClass(classOf[UploadStatus])
        val body = UpscanReadyCallback(Reference("Reference"), url, UploadDetails(Instant.now(), "checkSum", "fileMimeType", "fileName"))
        val jsonBody = Json.toJson(body)
        when(mockErsUtil.cache(meq(s"check-csv-files-${uploadId.value}"), callbackCaptor.capture, meq(scRef))(any(), any(), any()))
          .thenReturn(Future.successful(mock[CacheMap]))
        val result = csvFileUploadCallbackController.callback(uploadId, scRef)(request(jsonBody))
        status(result) shouldBe OK
        callbackCaptor.getValue.isInstanceOf[UploadedSuccessfully] shouldBe true
        verify(mockErsUtil, times(1))
          .cache(meq(s"check-csv-files-${uploadId.value}"), meq(callbackCaptor.getValue), meq(scRef))(any(), any(), any())
      }
    }

    "update upload status to Failed" when {
      "callback is UpscanFailedCallback and upload is InProgress" in {
        val callbackCaptor = ArgumentCaptor.forClass(classOf[UploadStatus])
        val body = UpscanFailedCallback(Reference("ref"), ErrorDetails("failed", "message"))
        val jsonBody = Json.toJson(body)
        when(mockErsUtil.cache(meq(s"check-csv-files-${uploadId.value}"), callbackCaptor.capture, meq(scRef))(any(), any(), any()))
          .thenReturn(Future.successful(mock[CacheMap]))
        val result = csvFileUploadCallbackController.callback(uploadId, scRef)(request(jsonBody))
        status(result) shouldBe OK
        callbackCaptor.getValue shouldBe Failed
        verify(mockErsUtil, times(1))
          .cache(meq(s"check-csv-files-${uploadId.value}"), meq(Failed), meq(scRef))(any(), any(), any())
      }
    }

    "return InternalServerError" when {
      "updating the cache fails" in {
        val body = UpscanFailedCallback(Reference("ref"), ErrorDetails("failed", "message"))
        val jsonBody = Json.toJson(body)
        when(mockErsUtil.cache(meq(s"check-csv-files-${uploadId.value}"), any[UploadStatus], meq(scRef))(any(), any(), any()))
          .thenReturn(Future.failed(new Exception("Test exception")))

        val result = await(csvFileUploadCallbackController.callback(uploadId, scRef)(request(jsonBody)))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        verify(mockErsUtil, times(1))
          .cache(meq(s"check-csv-files-${uploadId.value}"), any[UploadStatus], meq(scRef))(any(), any(), any())
      }

      "callback data is not in the correct format" in {
        val jsonBody = Json.parse("""{"key":"value"}""")
        val result = await(csvFileUploadCallbackController.callback(uploadId, scRef)(request(jsonBody)))
        status(result) shouldBe BAD_REQUEST
        verify(mockErsUtil, never())
          .cache(any(), any(), meq(scRef))(any(), any(), any())
      }
    }
  }
}
