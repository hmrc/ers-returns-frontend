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
import helpers.ErsTestHelper
import models.{CheckFileType, RequestObject, RsFormMappings}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, Fixtures}

import scala.concurrent.Future

class CheckFileTypeControllerTest extends UnitSpec with GuiceOneAppPerSuite with ERSFakeApplicationConfig with ErsTestHelper {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))
  implicit val requests: Request[_] = FakeRequest()
  implicit lazy val mat: Materializer = app.materializer

  "Check File Type Page GET" should {

    def buildFakeCheckingServiceController(
                                            fileType: Future[CheckFileType] = Future.successful(CheckFileType(Some("csv"))),
                                            requestObject: Future[RequestObject] = Future.successful(ersRequestObject)
                                          ): CheckFileTypeController = new CheckFileTypeController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig) {
      when(mockErsUtil.fetch[CheckFileType](refEq("check-file-type"), any())(any(), any(), any())).thenReturn(fileType)
      when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any(), any())).thenReturn(requestObject)
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkFileTypePage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "gives a call to showCheckFileTypePage if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkFileTypePage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK if fetch successful and shows check file type page with file type selected" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.showCheckFileTypePage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=csv]").hasAttr("checked") shouldEqual true
      document.select("input[id=ods]").hasAttr("checked") shouldEqual false
    }

    "give a status OK if fetch fails then show check file type page with nothing selected" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileType = Future.failed(new Exception))
      val result = controllerUnderTest.showCheckFileTypePage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=csv]").hasAttr("checked") shouldEqual false
      document.select("input[id=ods]").hasAttr("checked") shouldEqual false
    }

    "render error page if fetch on Request Object fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(requestObject = Future.failed(new Exception))
			val req = Fixtures.buildFakeRequestWithSessionId("GET")
      val result = controllerUnderTest.showCheckFileTypePage()(Fixtures.buildFakeUser, req, hc)

      contentAsString(result) should include(messages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage(req, messages))
    }

  }

  "Check File Type Page POST" should {

    def buildFakeCheckingServiceController(
                                            cache: Future[CacheMap] = Future.successful(mock[CacheMap]),
                                           requestObject: Future[RequestObject] = Future.successful(ersRequestObject)): CheckFileTypeController =
			new CheckFileTypeController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig){
      when(mockErsUtil.cache(matches("check-file-type"), any(), any())(any(), any(), any())).thenReturn(cache)
      when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any(), any())).thenReturn(requestObject)
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkFileTypeSelected().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "gives a call to showCheckFileTypeSelected if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkFileTypeSelected().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a bad request status and stay on the same page if form errors" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val fileTypeData = Map("" -> "")
      val form = RsFormMappings.checkFileTypeForm.bind(fileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected()(request, hc)
      status(result) shouldBe Status.OK
    }

    "if no form errors with file type = csv and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val checkFileTypeData = Map("checkFileType" -> "csv")
      val form = RsFormMappings.schemeTypeForm.bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected()(request, hc)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }

    "if no form errors with file type = ods and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val checkFileTypeData = Map("checkFileType" -> "ods")
      val form = RsFormMappings.schemeTypeForm.bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected()(request, hc)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.FileUploadController.uploadFilePage().toString
    }

    "if no form errors with scheme type and save fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(cache = Future.failed(new Exception))
      val schemeTypeData = Map("checkFileType" -> "csv")
      val form = RsFormMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = await(controllerUnderTest.showCheckFileTypeSelected()(request, hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

  }

}
