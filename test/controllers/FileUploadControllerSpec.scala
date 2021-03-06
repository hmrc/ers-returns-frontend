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

import akka.actor.ActorSystem
import akka.stream.Materializer
import models._
import models.upscan.Failed
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{SessionService, UpscanService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{ERSFakeApplicationConfig, ErsTestHelper, UpscanData}
import views.html.{file_upload_errors, file_upload_problem, global_error, upscan_ods_file_upload}

import scala.concurrent.{ExecutionContext, Future}

class FileUploadControllerSpec extends PlaySpec
  with MockitoSugar with LegacyI18nSupport
  with ERSFakeApplicationConfig with UpscanData with ErsTestHelper with GuiceOneAppPerSuite {

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

	val testOptString: Option[String] = Some("test")
	val schemeInfo: SchemeInfo = SchemeInfo(testOptString.get, DateTime.now, testOptString.get, testOptString.get, testOptString.get, testOptString.get)
	val validErsMetaData: ErsMetaData = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
	val ersRequestObject: RequestObject = RequestObject(testOptString, testOptString, testOptString, Some("CSOP"), Some("CSOP"),
		testOptString, testOptString, testOptString, testOptString)

	lazy val mockSessionService: SessionService = mock[SessionService]
	lazy val mockUpscanService: UpscanService = mock[UpscanService]
  val mockActorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val fileUploadErrorsView: file_upload_errors = app.injector.instanceOf[file_upload_errors]
  val upscanOdsFileUploadView: upscan_ods_file_upload = app.injector.instanceOf[upscan_ods_file_upload]
  val fileUploadProblemView: file_upload_problem = app.injector.instanceOf[file_upload_problem]


  implicit lazy val materializer: Materializer = app.materializer

	object TestFileUploadController extends FileUploadController(
		mockMCC, mockErsConnector, mockAuthConnector, mockSessionService, mockUpscanService, mockErsUtil, mockAppConfig, mockActorSystem, globalErrorView, fileUploadErrorsView, upscanOdsFileUploadView, fileUploadProblemView
	)

	when(mockErsUtil.fetch[CheckFileType](refEq("check-file-type"), any[String]())(any(),
		any(), any())).thenReturn(Future.successful(CheckFileType(Some("csv"))))


	def failure(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
		handler(TestFileUploadController.failure().apply(request))
	}

	def validationFailure(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
		handler(TestFileUploadController.validationFailure().apply(request))
	}

	def checkGlobalErrorPage(result: Future[Result]): Assertion = {
		status(result) mustBe INTERNAL_SERVER_ERROR
		contentAsString(result) must include (testMessages("ers.global_errors.title"))
	}

  def checkFileUploadProblemPage(result: Future[Result]): Assertion = {
    status(result) mustBe BAD_REQUEST
    contentAsString(result) must include (testMessages("ers.file_problem.heading"))
  }

  "uploadFilePage" must {
    when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(ersRequestObject))
    "return OK" when {
      "Upscan form data is successfully returned and callback record is created in session cache" in {
        when(mockUpscanService.getUpscanFormDataOds()(any[HeaderCarrier], any[Request[_]]))
          .thenReturn(Future.successful(upscanInitiateResponse))
        when(mockSessionService.createCallbackRecord(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        setAuthMocks()
        val result = TestFileUploadController.uploadFilePage()(request)
        status(result) mustBe OK
      }
    }

    "return global error page" when {
      "Upscan service returns an exception retrieving form data" in {
        reset(mockSessionService)
        when(mockUpscanService.getUpscanFormDataOds()(any[HeaderCarrier], any[Request[_]]))
          .thenReturn(Future.failed(new Exception("Expected exception")))
        setAuthMocks()
        val result = TestFileUploadController.uploadFilePage()(request)
        checkGlobalErrorPage(result)

        verify(mockSessionService, never()).createCallbackRecord(any(), any())
      }


      "Session service returns an exception creating callback record" in {
        when(mockUpscanService.getUpscanFormDataOds()(any[HeaderCarrier], any[Request[_]]))
          .thenReturn(Future.successful(upscanInitiateResponse))
        when(mockSessionService.createCallbackRecord(any[Request[_]], any[HeaderCarrier]))
            .thenReturn(Future.failed(new Exception("Expected exception")))

        setAuthMocks()
        val result = TestFileUploadController.uploadFilePage()(request)
        checkGlobalErrorPage(result)
      }
    }
  }

  "success" must {
    "return OK" when {
      "Callback record is returned with a successful upload and file name is cached" in {
        when(mockErsUtil.fetch[RequestObject](anyString())(any(), any(), any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockSessionService.getCallbackRecord(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockErsUtil.cache(meq("file-name"), meq(uploadedSuccessfully.name), any())(any[HeaderCarrier], any(), any[Request[AnyRef]]))
          .thenReturn(Future.successful(mock[CacheMap]))

        setAuthMocks()
        val result = TestFileUploadController.success()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileUploadController.validationResults().url)

      }
    }

    "return global error page" when {
      "caching file name fails" in {
        when(mockSessionService.getCallbackRecord(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockErsUtil.cache(meq("file-name"), meq(uploadedSuccessfully.name), any())(any[HeaderCarrier], any(), any[Request[AnyRef]]))
          .thenReturn(Future.failed(new Exception))

        setAuthMocks()
        val result = TestFileUploadController.success()(request)
        checkGlobalErrorPage(result)
      }
    }

    "return file upload problem page" when {
      "file name includes .csv" in {
        when(mockErsUtil.fetch[RequestObject](anyString())(any(), any(), any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockSessionService.getCallbackRecord(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(uploadedSuccessfullyCsv)))
        when(mockErsUtil.cache(meq("file-name"), meq(uploadedSuccessfullyCsv.name), any())(any[HeaderCarrier], any(), any[Request[AnyRef]]))
          .thenReturn(Future.successful(mock[CacheMap]))

        setAuthMocks()
        val result = TestFileUploadController.success()(request)
        checkFileUploadProblemPage(result)
      }
    }
  }

  "validationResults" must {
    "redirect the user" when {
      "Ers Meta Data is returned, callback record is uploaded successfully, remove presubmission data returns OK and validate file data returns OK" in {
        when(mockErsUtil.fetch[RequestObject](anyString())(any(), any(), any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockSessionService.getCallbackRecord(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockErsConnector.removePresubmissionData(any())(any[ERSAuthData], any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        when(mockErsUtil.fetch[ErsMetaData](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(validErsMetaData))
        when(mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(any[ERSAuthData], any[Request[AnyRef]], any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SchemeOrganiserController.schemeOrganiserPage().url)
      }

      "Ers Meta Data is returned, callback record is uploaded successfully, remove presubmission data returns OK and validate file data returns Accepted" in {
        when(mockErsUtil.fetch[RequestObject](anyString())(any(), any(), any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockSessionService.getCallbackRecord(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockErsConnector.removePresubmissionData(any())(any[ERSAuthData], any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        when(mockErsUtil.fetch[ErsMetaData](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(validErsMetaData))
        when(mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(any[ERSAuthData], any[Request[AnyRef]], any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(ACCEPTED, "")))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileUploadController.validationFailure().url)
      }
    }
    "return global error page" when {
      "validate file returns status code other than 200 OR 202" in {
        when(mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(any[ERSAuthData], any[Request[AnyRef]], any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(request)
        checkGlobalErrorPage(result)
      }

      "remove presubmission data returns status code other than 200" in {
        when(mockErsConnector.removePresubmissionData(meq(validErsMetaData.schemeInfo))(any[ERSAuthData], any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "")))
        setAuthMocks()
        val result = TestFileUploadController.validationResults()(request)
          checkGlobalErrorPage(result)
      }

      "session service throws an exception retrieving callback data" in {
        when(mockSessionService.getCallbackRecord(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception))
        setAuthMocks()
        val result = TestFileUploadController.validationResults()(request)
        checkGlobalErrorPage(result)
      }

      "session service returns a file which has not been successfully uploaded" in {
        when(mockSessionService.getCallbackRecord(any[Request[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(Failed)))
        setAuthMocks()
        val result = TestFileUploadController.validationResults()(request)
          checkGlobalErrorPage(result)
      }

      "cacheUtil fails to fetch metadata and returns an exception" in {
        when(mockErsUtil.fetch[ErsMetaData](meq("ErsMetaData"), meq(ersRequestObject.getSchemeReference))(any(), any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))
        setAuthMocks()
        val result = TestFileUploadController.validationResults()(request)
        checkGlobalErrorPage(result)
      }
    }
  }

  "failure" must {
    "be authorised" in {
			setUnauthorisedMocks()
      failure() { result =>
        status(result) must equal(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

    "authorised users" must {
      "redirect to the file upload problem page" in {
				setAuthMocks()
          failure() { result =>
            status(result) must equal(BAD_REQUEST)
            contentAsString(result) must include(testMessages("ers.file_problem.heading"))
          }
      }
    }
  }

  "Validation failure" must {
    "be authorised" in {
			setUnauthorisedMocks()
      validationFailure() { result =>
        status(result) must equal(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

    "authorised users" must {
      "respond with a status of OK" in {
        when(
          mockErsUtil.fetch[RequestObject](any())(any(), any(), any(), any())
        ).thenReturn(
          Future.successful(ersRequestObject)
        )
        when(mockErsUtil.fetch[CheckFileType](refEq("check-file-type"), any[String]())(any(), any(), any()))
          .thenReturn(Future.successful(CheckFileType(Some("csv"))))
				setAuthMocks()
        validationFailure() { result =>
          status(result) must be(OK)
          contentAsString(result) must include(testMessages("file_upload_errors.title"))
        }
      }
    }
  }
}
