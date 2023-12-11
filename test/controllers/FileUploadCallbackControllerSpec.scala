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

package controllers

import akka.stream.Materializer
import models.upscan._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ERSFileValidatorSessionCacheServices
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ERSFakeApplicationConfig, ErsTestHelper, UpscanData}

import controllers.internal.FileUploadCallbackController

import scala.concurrent.{ExecutionContext, Future}

class FileUploadCallbackControllerSpec
    extends PlaySpec
    with MockitoSugar
    with ERSFakeApplicationConfig
    with UpscanData
    with ErsTestHelper
    with GuiceOneAppPerSuite {

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

  implicit lazy val mat: Materializer    = app.materializer
  val mockSessionService: ERSFileValidatorSessionCacheServices = mock[ERSFileValidatorSessionCacheServices]

  object TestFileUploadCallbackController extends FileUploadCallbackController(mockMCC, mockSessionService)

  "callback" must {
    val sessionId = "sessionId"

    "update callback" when {
      "Upload status is UpscanReadyCallback" in {
        val uploadStatusCaptor: ArgumentCaptor[UploadStatus] = ArgumentCaptor.forClass(classOf[UploadStatus])
        val request                                          = FakeRequest(controllers.internal.routes.FileUploadCallbackController.callback(sessionId))
          .withBody(Json.toJson(readyCallback))

        when(mockSessionService.updateCallbackRecord(meq(sessionId), uploadStatusCaptor.capture())(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val result = TestFileUploadCallbackController.callback(sessionId)(request)

        status(result) mustBe OK
        uploadStatusCaptor.getValue mustBe UploadedSuccessfully(
          uploadDetails.fileName,
          readyCallback.downloadUrl.toExternalForm
        )
        verify(mockSessionService).updateCallbackRecord(meq(sessionId), any[UploadedSuccessfully])(any[HeaderCarrier])
      }

      "Upload status is failed" in {
        val uploadStatusCaptor: ArgumentCaptor[UploadStatus] = ArgumentCaptor.forClass(classOf[UploadStatus])
        val request                                          = FakeRequest(controllers.internal.routes.FileUploadCallbackController.callback(sessionId))
          .withBody(Json.toJson(failedCallback))

        when(mockSessionService.updateCallbackRecord(meq(sessionId), uploadStatusCaptor.capture())(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val result = TestFileUploadCallbackController.callback(sessionId)(request)
        status(result) mustBe OK
        uploadStatusCaptor.getValue mustBe Failed
        verify(mockSessionService).updateCallbackRecord(meq(sessionId), meq(Failed))(any[HeaderCarrier])
      }
    }

    "return Internal Server Error" when {
      "an exception occurs updating callback record" in {
        val request = FakeRequest(controllers.internal.routes.FileUploadCallbackController.callback(sessionId))
          .withBody(Json.toJson(failedCallback))

        when(mockSessionService.updateCallbackRecord(meq(sessionId), any[UploadStatus])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Mock Session Service Exception")))
        val result = TestFileUploadCallbackController.callback(sessionId)(request)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "throw an exception" when {
      "callback data cannot be parsed" in {
        val request = FakeRequest(controllers.internal.routes.FileUploadCallbackController.callback(sessionId))
          .withBody(Json.parse("""{"unexpectedKey": "unexpectedValue"}"""))

        status(TestFileUploadCallbackController.callback(sessionId)(request)) mustBe BAD_REQUEST
      }
    }
  }
}
