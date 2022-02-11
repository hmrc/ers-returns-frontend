/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import connectors.UpscanConnector
import models.upscan.{Reference, UploadId, UpscanInitiateRequest, UpscanInitiateResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Future


class UpscanServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures {

  override def fakeApplication: Application = new GuiceApplicationBuilder()
    .overrides(bind[UpscanConnector].toInstance(mockUpscanConnector))
    .build()

  def upscanService: UpscanService = app.injector.instanceOf[UpscanService]

  "getUpscanFormDataOds" must {
    "get form data from Upscan Connector with an initiate request" in {
      implicit val request: Request[AnyRef] = FakeRequest("GET", "http://localhost:9290/")
      val hc = HeaderCarrier(sessionId = Some(SessionId("sessionid")))
      val callback = controllers.internal.routes.FileUploadCallbackController.callback(hc.sessionId.get.value).absoluteURL()
      val success = controllers.routes.FileUploadController.success().absoluteURL()
      val failure = controllers.routes.FileUploadController.failure().absoluteURL()
      val expectedInitiateRequest = UpscanInitiateRequest(callback, success, failure, 1, 10485760)

      val upscanInitiateResponse = UpscanInitiateResponse(Reference("reference"), "postTarget", formFields = Map.empty[String, String])
      val initiateRequestCaptor = ArgumentCaptor.forClass(classOf[UpscanInitiateRequest])

      when(mockUpscanConnector.getUpscanFormData(initiateRequestCaptor.capture())(any[HeaderCarrier]))
        .thenReturn(Future.successful(upscanInitiateResponse))

      upscanService.getUpscanFormDataOds()(hc, request).futureValue

      initiateRequestCaptor.getAllValues contains expectedInitiateRequest
    }
  }

  "getUpscanFormDataCsv" must {
    "get form data from Upscan Connector with an initiate and uploadId" in {
      implicit val request: Request[AnyRef] = FakeRequest("GET", "http://localhost:9290/")
      val uploadId = UploadId("TestUploadId")
      val scRef = "ScRef"
      val hc = HeaderCarrier(sessionId = Some(SessionId("sessionid")))
      val callback = controllers.internal.routes.CsvFileUploadCallbackController.callback(uploadId, scRef).absoluteURL()
      val success = controllers.routes.CsvFileUploadController.success(uploadId).absoluteURL()
      val failure = controllers.routes.CsvFileUploadController.failure().absoluteURL()
      val expectedInitiateRequest = UpscanInitiateRequest(callback, success, failure, 1, 104857600)

      val upscanInitiateResponse = UpscanInitiateResponse(Reference("reference"), "postTarget", formFields = Map.empty[String, String])
      val initiateRequestCaptor = ArgumentCaptor.forClass(classOf[UpscanInitiateRequest])

      when(mockUpscanConnector.getUpscanFormData(initiateRequestCaptor.capture())(any[HeaderCarrier]))
        .thenReturn(Future.successful(upscanInitiateResponse))

      upscanService.getUpscanFormDataCsv(uploadId, scRef)(hc, request).futureValue

      initiateRequestCaptor.getAllValues contains expectedInitiateRequest
    }
  }

  val mockUpscanConnector: UpscanConnector = mock[UpscanConnector]

}
