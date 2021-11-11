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
import models.upscan._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json._
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Environment, i18n}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{ERSFakeApplicationConfig, ErsTestHelper, UpscanData}

import java.net.URL
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CsvFileUploadCallbackControllerSpec extends AnyWordSpecLike with Matchers with OptionValues
	with ERSFakeApplicationConfig with MockitoSugar with BeforeAndAfterEach with UpscanData with ErsTestHelper with GuiceOneAppPerSuite {

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

  implicit lazy val materializer: Materializer = app.materializer
  lazy val environment: Environment = app.injector.instanceOf[Environment]

  def request(body: JsValue): FakeRequest[JsValue] = FakeRequest().withBody(body)
  val scRef = "scRef"
  val url: URL = new URL("http://localhost:9000/myUrl")

  lazy val csvFileUploadCallbackController: CsvFileUploadCallbackController =
		new CsvFileUploadCallbackController(mockMCC, mockErsConnector, mockAuthConnector, mockErsUtil, mockAppConfig) {
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
          .cache(meq(s"check-csv-files-${uploadId.value}"), meq(callbackCaptor.getValue.asInstanceOf[UploadedSuccessfully]), meq(scRef))(any(), any(), any())
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
        assert(callbackCaptor.getValue equals Failed)
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

        val result = csvFileUploadCallbackController.callback(uploadId, scRef)(request(jsonBody))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        verify(mockErsUtil, times(1))
          .cache(meq(s"check-csv-files-${uploadId.value}"), any[UploadStatus], meq(scRef))(any(), any(), any())
      }

      "callback data is not in the correct format" in {
        val jsonBody = Json.parse("""{"key":"value"}""")
        val result = csvFileUploadCallbackController.callback(uploadId, scRef)(request(jsonBody))
        status(result) shouldBe BAD_REQUEST
        verify(mockErsUtil, never())
          .cache(any(), any(), meq(scRef))(any(), any(), any())
      }
    }
  }
}
