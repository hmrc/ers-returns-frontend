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

package controllers

import models.{CheckFileType, ErsMetaData, RequestObject, RsFormMappings, SchemeInfo}
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Fixtures.{EMIMetaData, ersRequestObject}
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{check_file_type, global_error}

import java.time.ZonedDateTime
import scala.concurrent.{ExecutionContext, Future}

class CheckFileTypeControllerSpec
  extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ERSFakeApplicationConfig
    with ErsTestHelper
    with GuiceOneAppPerSuite
    with ScalaFutures {

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
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val checkFileTypeView: check_file_type = app.injector.instanceOf[check_file_type]

  "Check File Type Page GET" should {

    def buildFakeCheckingServiceController(
                                            fileType: Future[CheckFileType] = Future.successful(CheckFileType(Some("csv"))),
                                            requestObject: Future[RequestObject] = Future.successful(ersRequestObject),
                                            requestMeta: Future[ErsMetaData] = Future.successful(EMIMetaData)
                                          ): CheckFileTypeController = new CheckFileTypeController(
      mockMCC,
      mockSessionService,
      globalErrorView,
      checkFileTypeView,
      testAuthActionGov
    ) {
      when(mockSessionService.fetch[CheckFileType](refEq("check-file-type"))(any(), any())).thenReturn(fileType)
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(requestObject)
      when(mockSessionService.fetch[ErsMetaData](refEq(ERS_META_DATA))(any(), any())).thenReturn(requestMeta)
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
      val result = controllerUnderTest.showCheckFileTypePage()(
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      )
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=csv]").hasAttr("checked") shouldEqual true
      document.select("input[id=ods]").hasAttr("checked") shouldEqual false
    }

    "give a status OK if fetch fails then show check file type page with nothing selected" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileType = Future.failed(new Exception))

      val result = controllerUnderTest.showCheckFileTypePage()(
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      )

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=csv]").hasAttr("checked") shouldEqual false
      document.select("input[id=ods]").hasAttr("checked") shouldEqual false
    }

    "render error page if fetch on Request Object fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(requestObject = Future.failed(new Exception))
      val req = Fixtures.buildFakeRequestWithSessionId("GET")
      val result = controllerUnderTest.showCheckFileTypePage()(buildRequestWithAuth(req))

      contentAsString(result) should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(req, testMessages))
      )
    }

  }

  "Check File Type Page POST" should {
    def buildFakeCheckingServiceController(successfulCache: Boolean = true,
                                           requestObject: Future[RequestObject] = Future.successful(ersRequestObject)): CheckFileTypeController =
      new CheckFileTypeController(mockMCC, mockSessionService, globalErrorView, checkFileTypeView, testAuthActionGov) {
        when(mockSessionService.cache(matches("check-file-type"), any())(any(), any())).thenReturn(
          if (successfulCache) Future.successful(sessionPair) else Future.failed(new Exception("error")))
        when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(requestObject)
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
      val form = RsFormMappings.checkFileTypeForm().bind(fileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)

      val result = controllerUnderTest.showCheckFileTypeSelected()(buildRequestWithAuth(request))

      status(result) shouldBe Status.OK
    }

    "if no form errors with file type = csv and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val checkFileTypeData = Map("checkFileType" -> "csv")
      val form = RsFormMappings.schemeTypeForm().bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)

      val result = controllerUnderTest.showCheckFileTypeSelected()(buildRequestWithAuth(request))

      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }

    "if no form errors with file type = ods and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val checkFileTypeData = Map("checkFileType" -> "ods")
      val form = RsFormMappings.schemeTypeForm().bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)

      val result = controllerUnderTest.showCheckFileTypeSelected()(buildRequestWithAuth(request))

      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.FileUploadController.uploadFilePage().toString
    }

    "if no form errors with scheme type and save fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(successfulCache = false)
      val schemeTypeData = Map("checkFileType" -> "csv")
      val form = RsFormMappings.schemeTypeForm().bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)

      val result = controllerUnderTest.showCheckFileTypeSelected()(buildRequestWithAuth(request))

      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(request, testMessages))
      )
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }
}
