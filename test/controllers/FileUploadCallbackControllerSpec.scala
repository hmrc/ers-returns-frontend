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

import akka.stream.Materializer
import models.upscan._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ERSFakeApplicationConfig, UpscanData}

import scala.concurrent.Future

class FileUploadCallbackControllerSpec extends PlaySpec with MockitoSugar with ERSFakeApplicationConfig with OneAppPerSuite with UpscanData {

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer
  val mockSessionService: SessionService = mock[SessionService]

  object TestFileUploadCallbackController extends FileUploadCallbackController(mockSessionService)

  "callback" must {
    val sessionId = "sessionId"

    "update callback" when {
      "Upload status is UpscanReadyCallback" in {
        val uploadStatusCaptor: ArgumentCaptor[UploadStatus] = ArgumentCaptor.forClass(classOf[UploadStatus])
        val request = FakeRequest(controllers.routes.FileUploadCallbackController.callback(sessionId))
          .withBody(Json.toJson(readyCallback))

        when(mockSessionService.updateCallbackRecord(meq(sessionId), uploadStatusCaptor.capture())(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val result = TestFileUploadCallbackController.callback(sessionId)(request)

        status(result) mustBe OK
        uploadStatusCaptor.getValue mustBe UploadedSuccessfully(uploadDetails.fileName, readyCallback.downloadUrl.toExternalForm)
        verify(mockSessionService).updateCallbackRecord(meq(sessionId), any[UploadedSuccessfully])(any[Request[_]], any[HeaderCarrier])
      }

      "Upload status is failed" in {
        val uploadStatusCaptor: ArgumentCaptor[UploadStatus] = ArgumentCaptor.forClass(classOf[UploadStatus])
        val request = FakeRequest(controllers.routes.FileUploadCallbackController.callback(sessionId))
          .withBody(Json.toJson(failedCallback))

        when(mockSessionService.updateCallbackRecord(meq(sessionId), uploadStatusCaptor.capture())(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val result = TestFileUploadCallbackController.callback(sessionId)(request)
        status(result) mustBe OK
        uploadStatusCaptor.getValue mustBe Failed
        verify(mockSessionService).updateCallbackRecord(meq(sessionId), meq(Failed))(any[Request[_]], any[HeaderCarrier])
      }
    }

    "return Internal Server Error" when {
      "an exception occurs updating callback record" in {
        val request = FakeRequest(controllers.routes.FileUploadCallbackController.callback(sessionId))
          .withBody(Json.toJson(failedCallback))

        when(mockSessionService.updateCallbackRecord(meq(sessionId), any[UploadStatus])(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Mock Session Service Exception")))
        val result = TestFileUploadCallbackController.callback(sessionId)(request)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "throw an exception" when {
      "callback data cannot be parsed" in {
        val request = FakeRequest(controllers.routes.FileUploadCallbackController.callback(sessionId))
          .withBody(Json.parse("""{"unexpectedKey": "unexpectedValue"}"""))

        status(TestFileUploadCallbackController.callback(sessionId)(request)) mustBe BAD_REQUEST
      }
    }
  }
}
