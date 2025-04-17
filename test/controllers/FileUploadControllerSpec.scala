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

import controllers.auth.RequestWithOptionalAuthContext
import models._
import models.upscan.Failed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.UpscanService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{ERSFakeApplicationConfig, ErsTestHelper, UpscanData}
import views.html._

import java.time.ZonedDateTime
import scala.concurrent.{ExecutionContext, Future}

class FileUploadControllerSpec
    extends PlaySpec
    with MockitoSugar
    with LegacyI18nSupport
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

  val testOptString: Option[String] = Some("test")
  val schemeInfo: SchemeInfo = SchemeInfo(
    testOptString.get,
    ZonedDateTime.now,
    testOptString.get,
    testOptString.get,
    testOptString.get,
    "CSOP"
  )
  val validErsMetaData: ErsMetaData   =
    ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
  val ersRequestObject: RequestObject = RequestObject(
    testOptString,
    Some("2023/24"),
    testOptString,
    Some("CSOP"),
    Some("CSOP"),
    testOptString,
    testOptString,
    testOptString,
    testOptString
  )

  lazy val mockUpscanService: UpscanService = mock[UpscanService]
  implicit val mockActorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val fileUploadErrorsView: file_upload_errors = app.injector.instanceOf[file_upload_errors]
  val fileUploadErrorsOdsView: file_upload_errors_ods = app.injector.instanceOf[file_upload_errors_ods]
  val templateFailureView: template_version_problem = app.injector.instanceOf[template_version_problem]
  val upscanOdsFileUploadView: upscan_ods_file_upload = app.injector.instanceOf[upscan_ods_file_upload]
  val fileUploadProblemView: file_upload_problem = app.injector.instanceOf[file_upload_problem]

  implicit lazy val materializer: Materializer = app.materializer

  object TestFileUploadController
      extends FileUploadController(
        mockMCC,
        mockErsConnector,
        mockSessionService,
        mockUpscanService,
        globalErrorView,
        fileUploadErrorsView,
        fileUploadErrorsOdsView,
        templateFailureView,
        upscanOdsFileUploadView,
        fileUploadProblemView,
        testAuthAction
      )

  when(mockSessionService.fetch[CheckFileType](refEq("check-file-type"))(any(), any()))
    .thenReturn(Future.successful(CheckFileType(Some("csv"))))

  def failure(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit =
    handler(TestFileUploadController.failure().apply(request))

  def validationFailure(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(
    handler: Future[Result] => Any
  ): Unit =
    handler(TestFileUploadController.validationFailure().apply(request))

  def templateFailure(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(
    handler: Future[Result] => Any
  ): Unit =
    handler(TestFileUploadController.templateFailure().apply(request))

  def checkGlobalErrorPage(result: Future[Result]): Assertion = {
    status(result) mustBe INTERNAL_SERVER_ERROR
    contentAsString(result) must include(testMessages("ers.global_errors.title"))
  }

  def checkFileUploadProblemPage(result: Future[Result]): Assertion = {
    status(result) mustBe BAD_REQUEST
    contentAsString(result) must include(testMessages("ers.file_problem.heading"))
  }

  "uploadFilePage" must {
    when(mockSessionService.fetch[RequestObject](any())(any(), any()))
      .thenReturn(Future.successful(ersRequestObject))
    "return OK" when {
      "Upscan form data is successfully returned and callback record is created in session cache" in {
        when(mockUpscanService.getUpscanFormDataOds(anyString())(any[HeaderCarrier], any[Request[_]]))
          .thenReturn(Future.successful(upscanInitiateResponse))
        when(mockErsConnector.createCallbackRecord(any(), any()))
          .thenReturn(Future.successful(CREATED))

        setAuthMocks()
        val result = TestFileUploadController.uploadFilePage()(testFakeRequest)
        status(result) mustBe OK
      }
    }

    "return global error page" when {
      "Upscan service returns an exception retrieving form data" in {
        reset(mockErsConnector)
        setAuthMocks()
        when(mockUpscanService.getUpscanFormDataOds(anyString())(any[HeaderCarrier], any[Request[_]]))
          .thenReturn(Future.failed(new Exception("Expected exception")))
        when(mockSessionService.fetch[RequestObject](meq(ERS_REQUEST_OBJECT))(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))

        val result = TestFileUploadController.uploadFilePage()(testFakeRequest)
        checkGlobalErrorPage(result)

        verify(mockErsConnector, never()).createCallbackRecord(any(), any())
      }

      "Session service returns an exception creating callback record" in {
        when(mockUpscanService.getUpscanFormDataOds(anyString())(any[HeaderCarrier], any[Request[_]]))
          .thenReturn(Future.successful(upscanInitiateResponse))
        when(mockErsConnector.createCallbackRecord(any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))

        setAuthMocks()
        val result = TestFileUploadController.uploadFilePage()(testFakeRequest)
        checkGlobalErrorPage(result)
      }
    }
  }

  "success" must {
    "return OK" when {
      "Callback record is returned with a successful upload and file name is cached" in {
        when(mockSessionService.fetch[RequestObject](anyString())(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockErsConnector.getCallbackRecord(any(), any()))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockSessionService.cache(meq("file-name"), meq(uploadedSuccessfully.name))(any(), any()))
          .thenReturn(Future.successful(sessionPair))

        setAuthMocks()
        val result = TestFileUploadController.success()(testFakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileUploadController.validationResults().url)

      }
    }

    "return global error page" when {
      "caching file name fails" in {
        when(mockErsConnector.getCallbackRecord(any(), any))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockSessionService.cache(meq("file-name"), meq(uploadedSuccessfully.name))(any(), any()))
          .thenReturn(Future.failed(new Exception))

        setAuthMocks()
        val result = TestFileUploadController.success()(testFakeRequest)
        checkGlobalErrorPage(result)
      }
    }

    "return file upload problem page" when {
      "file name includes .csv" in {
        when(mockSessionService.fetch[RequestObject](anyString())(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockErsConnector.getCallbackRecord(any(), any))
          .thenReturn(Future.successful(Some(uploadedSuccessfullyCsv)))
        when(mockSessionService.cache(meq("file-name"), meq(uploadedSuccessfullyCsv.name))(any(), any()))
          .thenReturn(Future.successful(sessionPair))

        setAuthMocks()
        val result = TestFileUploadController.success()(testFakeRequest)
        checkFileUploadProblemPage(result)
      }
    }
  }

  "validationResults" must {
    "redirect the user to SchemeOrganiserController.schemeOrganiserPage" when {
      "Ers Meta Data is returned, callback record is uploaded successfully, remove presubmission data returns OK and validate file data returns OK" in {
        when(mockSessionService.fetch[RequestObject](anyString())(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockErsConnector.getCallbackRecord(any(), any))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(
          mockErsConnector
            .removePresubmissionData(any())(any[RequestWithOptionalAuthContext[AnyContent]], any())
        )
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        when(mockSessionService.fetch[ErsMetaData](any())(any(), any()))
          .thenReturn(Future.successful(validErsMetaData))
        when(
          mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(
            any[RequestWithOptionalAuthContext[AnyContent]],
            any()
          )
        )
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.schemeOrganiser.routes.SchemeOrganiserBasedInUkController.questionPage().url)
      }
    }

    "redirect the user to FileUploadController.validationFailure()" when {
      "Ers Meta Data is returned, callback record is uploaded successfully, remove presubmission data returns OK and validate file data returns Accepted" in {
        when(mockSessionService.fetch[RequestObject](anyString())(any(), any())).thenReturn(Future.successful(ersRequestObject))
        when(mockErsConnector.getCallbackRecord(any(), any)).thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockErsConnector.removePresubmissionData(any())(any[RequestWithOptionalAuthContext[AnyContent]], any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        when(mockSessionService.fetch[ErsMetaData](any())(any(), any())).thenReturn(Future.successful(validErsMetaData))
        when(mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(any[RequestWithOptionalAuthContext[AnyContent]], any()))
          .thenReturn(Future.successful(HttpResponse(ACCEPTED, "")))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileUploadController.validationFailure().url)
      }

      "Ers Meta Data is returned, callback record is uploaded successfully, remove presubmission data returns OK, validate file data returns Accepted, for CSOP with Incorrect ERS Template validation error, csopV5Enabled = false" in {
        when(mockAppConfig.csopV5Enabled).thenReturn(false)
        when(mockSessionService.fetch[RequestObject](anyString())(any(), any())).thenReturn(Future.successful(ersRequestObject))
        when(mockErsConnector.getCallbackRecord(any(), any)).thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockErsConnector.removePresubmissionData(any())(any[RequestWithOptionalAuthContext[AnyContent]], any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        when(mockSessionService.fetch[ErsMetaData](any())(any(), any())).thenReturn(Future.successful(validErsMetaData))
        when(mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(any[RequestWithOptionalAuthContext[AnyContent]], any()))
          .thenReturn(Future.successful(HttpResponse(ACCEPTED, "Incorrect ERS Template")))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileUploadController.validationFailure().url)
      }
    }

    "redirect the user to FileUploadController.odsSchemeMismatchFailure()" when {
      "Ers Meta Data is returned, callback record is uploaded successfully, remove presubmission data returns OK, validate file data returns Accepted, where a SchemeMismatchError occurs" in {
        when(mockSessionService.fetch[RequestObject](anyString())(any(), any())).thenReturn(Future.successful(ersRequestObject))
        when(mockErsConnector.getCallbackRecord(any(), any)).thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockErsConnector.removePresubmissionData(any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        when(mockSessionService.fetch[ErsMetaData](any())(any(), any())).thenReturn(Future.successful(validErsMetaData))
        when(mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(any(), any()))
          .thenReturn(Future.successful(HttpResponse(
            status = ACCEPTED,
            json = Json.obj(
              "errorMessage" -> "Incorrect ERS Template - Sheet Name isn't as expected",
              "expectedSchemeType" -> "CSOP",
              "requestSchemeType" -> "SAYE"
            ),
            headers = Map("Content-Type" -> Seq("application/json"))
          )))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        val updatedSession = session(result)
        updatedSession.get("expectedScheme") mustBe Some("CSOP")
        updatedSession.get("requestScheme") mustBe Some("SAYE")
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileUploadController.odsSchemeMismatchFailure().url)
      }
    }

    "redirect the user to FileUploadController.templateFailure()" when {
      "Ers Meta Data is returned, callback record is uploaded successfully, remove presubmission data returns OK, validate file data returns Accepted, for CSOP with Incorrect ERS Template validation error, csopV5Enabled = true" in {
        when(mockAppConfig.csopV5Enabled).thenReturn(true)
        when(mockSessionService.fetch[RequestObject](anyString())(any(), any())).thenReturn(Future.successful(ersRequestObject))
        when(mockErsConnector.getCallbackRecord(any(), any)).thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockErsConnector.removePresubmissionData(any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))
        when(mockSessionService.fetch[ErsMetaData](any())(any(), any())).thenReturn(Future.successful(validErsMetaData))
        when(mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(any[RequestWithOptionalAuthContext[AnyContent]], any()))
          .thenReturn(Future.successful(HttpResponse(ACCEPTED, "Incorrect ERS Template - Sheet Name isn't as expected")))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileUploadController.templateFailure().url)
      }
    }

    "return global error page" when {
      "validate file returns status code other than 200 OR 202" in {
        when(
          mockErsConnector.validateFileData(meq(uploadedSuccessfully), any[SchemeInfo])(
            any[RequestWithOptionalAuthContext[AnyContent]],
            any()
          )
        )
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        checkGlobalErrorPage(result)
      }

      "remove presubmission data returns status code other than 200" in {
        when(
          mockErsConnector.removePresubmissionData(meq(validErsMetaData.schemeInfo))(
            any[RequestWithOptionalAuthContext[AnyContent]],
            any()
          )
        )
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "")))
        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        checkGlobalErrorPage(result)
      }

      "session service throws an exception retrieving callback data" in {
        when(mockErsConnector.getCallbackRecord(any(), any))
          .thenReturn(Future.failed(new Exception))
        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        checkGlobalErrorPage(result)
      }

      "session service returns a file which has not been successfully uploaded" in {
        when(mockErsConnector.getCallbackRecord(any(), any))
          .thenReturn(Future.successful(Some(Failed)))
        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        checkGlobalErrorPage(result)
      }

      "cacheUtil fails to fetch metadata and returns an exception" in {
        when(mockSessionService.fetch[ErsMetaData](meq("ErsMetaData"))(any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))
        setAuthMocks()
        val result = TestFileUploadController.validationResults()(testFakeRequest)
        checkGlobalErrorPage(result)
      }
    }
  }

  "failure" must {
    "be authorised" in {
      setUnauthorisedMocks()
      failure() { result =>
        status(result)               must equal(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

    "authorised users" must {
      "redirect to the file upload problem page" in {
        setAuthMocks()
        failure() { result =>
          status(result)          must equal(BAD_REQUEST)
          contentAsString(result) must include(testMessages("ers.file_problem.heading"))
        }
      }
    }
  }

  "validationFailure" must {
    "be authorised" in {
      setUnauthorisedMocks()
      validationFailure() { result =>
        status(result) must equal(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

    "authorised users" must {
      "respond with a status of OK" in {
        when(mockSessionService.fetch[RequestObject](any())(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockSessionService.fetch[CheckFileType](refEq("check-file-type"))(any(), any()))
          .thenReturn(Future.successful(CheckFileType(Some("csv"))))
        setAuthMocks()
        validationFailure() { result =>
          status(result) must be(OK)
          contentAsString(result) must include(testMessages("file_upload_errors.title"))
        }
      }
    }

    "return fileUploadErrorsView" in {
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetch[CheckFileType](refEq("check-file-type"))(any(), any()))
        .thenReturn(Future.successful(CheckFileType(Some("ods"))))
      setAuthMocks()
      val result = TestFileUploadController.validationFailure()(testFakeRequest)
      status(result) must be(OK)
      contentAsString(result) must include(testMessages("file_upload_errors.title"))
    }
  }

  "odsSchemeMismatchFailure" must {
    "return fileUploadErrorsOdsView when expectedScheme and requestScheme are present in session" in {
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetch[CheckFileType](refEq("check-file-type"))(any(), any()))
        .thenReturn(Future.successful(CheckFileType(Some("ods"))))
      setAuthMocks()
      val requestWithSession = testFakeRequest.withSession(
        "expectedScheme" -> "SAYE",
        "requestScheme" -> "CSOP"
      )
      val result = TestFileUploadController.odsSchemeMismatchFailure()(requestWithSession)
      status(result) must be(OK)
      contentAsString(result) must include(testMessages("file_upload_errors.scheme_mismatch.title"))
    }
  }

  "templateFailure" must {
    "be authorised" in {
      setUnauthorisedMocks()
      validationFailure() { result =>
        status(result) must equal(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

    "authorised users" must {
      "respond with a status of OK" in {
        when(mockSessionService.fetch[RequestObject](any())(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        setAuthMocks()
        templateFailure() { result =>
          status(result) must be(OK)
          contentAsString(result) must include(testMessages("file_upload_errors.problem.title"))
        }
      }
    }

    "return templateFailureView for 2023/24" in {
      val result = TestFileUploadController.templateFailure().apply(testFakeRequest)

      status(result) must be(OK)
      contentAsString(result) must include(testMessages("file_upload_errors.problem.title"))
      contentAsString(result) must include(testMessages("file_upload_errors.problem.message.v5", "2023 to 2024"))
    }

    "return templateFailureView for 2022/23" in {
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(taxYear = Some("2022/23"))))
      val result = TestFileUploadController.templateFailure().apply(testFakeRequest)

      status(result) must be(OK)
      contentAsString(result) must include(testMessages("file_upload_errors.problem.title"))
      contentAsString(result) must include(testMessages("file_upload_errors.problem.message.v4", "2022 to 2023"))
    }
  }
}
