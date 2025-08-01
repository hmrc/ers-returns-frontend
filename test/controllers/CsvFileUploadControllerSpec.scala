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
import models.upscan._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.api.mvc.request.RequestTarget
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.UpscanService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.mongo.cache.CacheItem
import utils.Fixtures.ersRequestObject
import utils._
import views.html._

import java.time.{Instant, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class CsvFileUploadControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ERSFakeApplicationConfig
    with MockitoSugar
    with BeforeAndAfterEach
    with UpscanData
    with ScalaFutures
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
  implicit lazy val mat: Materializer = app.materializer
  implicit val mockActorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val upscanCsvFileUploadView: upscan_csv_file_upload = app.injector.instanceOf[upscan_csv_file_upload]
  val fileUploadErrorsView: file_upload_errors = app.injector.instanceOf[file_upload_errors]
  val fileUploadProblemView: file_upload_problem = app.injector.instanceOf[file_upload_problem]
  val fileSizeLimitErrorView: views.html.file_size_limit_error = app.injector.instanceOf[file_size_limit_error]

  val mockUpscanService: UpscanService   = mock[UpscanService]

  lazy val csvFileUploadController: CsvFileUploadController =
    new CsvFileUploadController(
      mockMCC,
      mockErsConnector,
      mockUpscanService,
      mockSessionService,
      globalErrorView,
      upscanCsvFileUploadView,
      fileSizeLimitErrorView,
      fileUploadErrorsView,
      fileUploadProblemView,
      testAuthAction
    ) {
      override lazy val allCsvFilesCacheRetryAmount: Int = 1
    }

  override def beforeEach(): Unit = {
    when(mockErsUtil.CSV_FILES_UPLOAD).thenReturn("csv-files-upload")
    when(mockErsUtil.CHECK_CSV_FILES).thenReturn("check-csv-files")
    when(mockSessionService.fetch[RequestObject](any())(any(), any()))
      .thenReturn(Future.successful(ersRequestObject))
    setAuthMocks()
  }

  "uploadFilePage" should {
    "display file upload page" when {
      "form data is successfully retrieved from upscan" in {
        when(
          mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any())
        ).thenReturn(Future.successful(notStartedUpscanCsvFilesList))
        when(mockUpscanService.getUpscanFormDataCsv(UploadId(anyString()), any())(any(), any())) thenReturn Future
          .successful(UpscanInitiateResponse(Reference("Reference"), "postTarget", formFields = Map()))

        val result = csvFileUploadController.uploadFilePage()(testFakeRequest)
        status(result)        shouldBe OK
        contentAsString(result) should include(testMessages("csv_file_upload.upload_your_file", ""))
      }
    }

    "display global error page" when {
      "upscanService throws an exception" in {
        val upscanCsvFilesCallbackList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(
          List(
            UpscanCsvFilesCallback(testUploadId, "file1", NotStarted),
            UpscanCsvFilesCallback(UploadId("ID2"), "file4", NotStarted)
          )
        )
        when(
          mockSessionService.fetch[UpscanCsvFilesCallbackList](meq("check-csv-files"))(any(), any())
        ) thenReturn Future.successful(upscanCsvFilesCallbackList)

        when(mockUpscanService.getUpscanFormDataCsv(UploadId(anyString()), any())(any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))

        val result = csvFileUploadController.uploadFilePage()(testFakeRequest)
        status(result)        shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) should include(testMessages("ers.global_errors.title"))
      }

      "fetching cache data throws an exception" in {
        when(mockSessionService.fetch[UpscanCsvFilesCallbackList](meq("check-csv-files"))(any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))

        val result = csvFileUploadController.uploadFilePage()(testFakeRequest)
        status(result)        shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) should include(testMessages("ers.global_errors.title"))
      }

      "there are no files to upload" in {
        val upscanCsvFilesCallbackList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(
          List(
            UpscanCsvFilesCallback(testUploadId, "file1", InProgress)
          )
        )
        when(
          mockSessionService.fetch[UpscanCsvFilesCallbackList](meq("check-csv-files"))(any(), any())
        ) thenReturn Future.successful(upscanCsvFilesCallbackList)

        val result = csvFileUploadController.uploadFilePage()(testFakeRequest)

        status(result)        shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) should include(testMessages("ers.global_errors.title"))
      }
    }
  }

  "success" should {
    "update the cache for the relevant uploadId to InProgress" in {
      val updatedCallbackCaptor: ArgumentCaptor[UpscanCsvFilesList] =
        ArgumentCaptor.forClass(classOf[UpscanCsvFilesList])
      when(mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any()))
        .thenReturn(Future.successful(notStartedUpscanCsvFilesList))
      when(mockSessionService.cache[UpscanCsvFilesList](meq("csv-files-upload"), updatedCallbackCaptor.capture())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      await(csvFileUploadController.success(testUploadId)(testFakeRequest))
      updatedCallbackCaptor.getValue shouldBe inProgressUpscanCsvFilesList
    }

    "redirect the user to validation results" when {
      "no file in the cache has UploadStatus of NotStarted after update" in {
        when(mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any()))
          .thenReturn(Future.successful(notStartedUpscanCsvFilesList))
        when(mockSessionService.cache(meq("csv-files-upload"), any[UpscanCsvFilesList])(any(), any()))
          .thenReturn(Future.successful(sessionPair))

        val result = csvFileUploadController.success(testUploadId)(testFakeRequest)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.CsvFileUploadController.validationResults().url)
      }
    }
  }

  "redirect the user to upload a file" when {
    "a file in the cache has an UploadStatus of NotStarted after update" in {
      when(mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any()))
        .thenReturn(Future.successful(multipleNotStartedUpscanCsvFilesList))
      when(mockSessionService.cache(meq("csv-files-upload"), any[UpscanCsvFilesList])(any(), any()))
        .thenReturn(Future.successful(sessionPair))
      val result = csvFileUploadController.success(testUploadId)(testFakeRequest)
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.CsvFileUploadController.uploadFilePage().url)

    }
  }

  "display global error page" when {
    "Fetching the cache fails" in {
      when(mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any()))
        .thenReturn(Future.failed(new Exception("Expected Exception")))

      val result = csvFileUploadController.success(testUploadId)(testFakeRequest)
      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include(testMessages("ers.global_errors.title"))
    }

    "saving the cache fails" in {
      when(mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any()))
        .thenReturn(Future.successful(multipleNotStartedUpscanCsvFilesList))
      when(mockSessionService.cache(meq("csv-files-upload"), any[UpscanCsvFilesCallbackList])(any(), any()))
        .thenReturn(Future.failed(new Exception("Expected Exception")))

      val result = csvFileUploadController.success(testUploadId)(testFakeRequest)
      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include(testMessages("ers.global_errors.title"))
    }
  }

  "calling failure" should {
    "redirect for unauthorised users to login page" in {
      setUnauthorisedMocks()
      val result = csvFileUploadController.failure().apply(FakeRequest("GET", ""))
      status(result)                                                        shouldBe SEE_OTHER
      result.futureValue.header.headers("Location").contains("/gg/sign-in") shouldBe true
    }
    "redirect users to the file upload problem page" in {
      val result = contentAsString(csvFileUploadController.failure().apply(FakeRequest("GET", "")))
      assert(result.contains(testMessages("ers.file_problem.heading")))
    }


    "redirect to the file size limit error page" in {
      val request = FakeRequest().withTarget(RequestTarget("123", "/file-upload/failure", Map("errorCode" -> Seq("EntityTooLarge"))))
      val result = contentAsString(csvFileUploadController.failure().apply(request))
      assert(result.contains(testMessages("There is a problem – Employment Related Securities – GOV.UK")))
    }

  }

  "calling validationFailure" should {
    "redirect for unauthorised users to login page" in {
      setUnauthorisedMocks()
      val result = csvFileUploadController.validationFailure().apply(FakeRequest("GET", ""))
      status(result)                                                        shouldBe SEE_OTHER
      result.futureValue.header.headers("Location").contains("/gg/sign-in") shouldBe true
    }

    "show the result of processValidationFailure() for authorised users" in {
      when(
        mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any())
      ).thenReturn(
        Future.successful(ersRequestObject)
      )
      when(
        mockSessionService.fetch[CheckFileType](refEq(mockErsUtil.FILE_TYPE_CACHE))(any(), any())
      ).thenReturn(
        Future.successful(CheckFileType(Some("csv")))
      )
      val result = csvFileUploadController.validationFailure()(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result)        shouldBe OK
      contentAsString(result) should include(testMessages("file_upload_errors.title"))
    }

  }

  "calling processValidationFailure" should {

    lazy val csvFileUploadController: CsvFileUploadController =
      new CsvFileUploadController(
        mockMCC,
        mockErsConnector,
        mockUpscanService,
        mockSessionService,
        globalErrorView,
        upscanCsvFileUploadView,
        fileSizeLimitErrorView,
        fileUploadErrorsView,
        fileUploadProblemView,
        testAuthAction
      ) {
        override lazy val allCsvFilesCacheRetryAmount: Int = 1
      }

    "return Ok if fetching CheckFileType from cache is successful" in {
      when(
        mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any())
      ).thenReturn(
        Future.successful(ersRequestObject)
      )
      when(
        mockSessionService.fetch[CheckFileType](refEq(mockErsUtil.FILE_TYPE_CACHE))(any(), any())
      ).thenReturn(
        Future.successful(CheckFileType(Some("csv")))
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController.processValidationFailure()(authRequest)
      status(result)        shouldBe OK
      contentAsString(result) should include(testMessages("file_upload_errors.title"))
    }

    "return the globalErrorPage if fetching CheckFileType from cache fails" in {
      when(
        mockSessionService.fetch[CheckFileType](refEq("check-file-type"))(any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      when(
        mockSessionService.fetch[RequestObject](any())(any(), any())
      ).thenReturn(
        Future.successful(ersRequestObject)
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController.processValidationFailure()(authRequest)
      status(result)          shouldBe 500
      contentAsString(result) shouldBe contentAsString(
        Future.successful(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "return the globalErrorPage if fetching requestObject from cache fails" in {
      when(
        mockSessionService.fetch[CheckFileType](refEq("check-file-type"))(any(), any())
      ).thenReturn(
        Future.successful(CheckFileType(Some("csv")))
      )
      when(
        mockSessionService.fetch[RequestObject](any())(any(), any())
      ).thenReturn(
        Future.failed(new Exception)
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController.processValidationFailure()(authRequest)
      status(result)          shouldBe 500
      contentAsString(result) shouldBe contentAsString(
        Future.successful(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

  }

  "calling validationResults" should {

    lazy val csvFileUploadController: CsvFileUploadController =
      new CsvFileUploadController(
        mockMCC,
        mockErsConnector,
        mockUpscanService,
        mockSessionService,
        globalErrorView,
        upscanCsvFileUploadView,
        fileSizeLimitErrorView,
        fileUploadErrorsView,
        fileUploadProblemView,
        testAuthAction
      ) {
        override def processValidationResults()(implicit
          request: RequestWithOptionalAuthContext[AnyContent],
          hc: HeaderCarrier
        ): Future[Result]                                  = Future(Ok)
        override lazy val allCsvFilesCacheRetryAmount: Int = 1
      }

    "redirect for unauthorised users to login page" in {
      reset(mockAuthConnector)
      setUnauthorisedMocks()
      val result = csvFileUploadController.validationResults().apply(FakeRequest("GET", ""))
      status(result)                                                        shouldBe SEE_OTHER
      result.futureValue.header.headers("Location").contains("/gg/sign-in") shouldBe true
    }

    "show the result of processValidationFailure() for authorised users" in {
      val result = csvFileUploadController.validationResults()(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe OK
    }

  }

  "calling processValidationResults" should {

    lazy val csvFileUploadController: CsvFileUploadController =
      new CsvFileUploadController(
        mockMCC,
        mockErsConnector,
        mockUpscanService,
        mockSessionService,
        globalErrorView,
        upscanCsvFileUploadView,
        fileSizeLimitErrorView,
        fileUploadErrorsView,
        fileUploadProblemView,
        testAuthAction
      ) {
        override def removePresubmissionData(schemeInfo: SchemeInfo)(implicit
          request: RequestWithOptionalAuthContext[AnyContent],
          hc: HeaderCarrier
        ): Future[Result]                                  = Future(Ok)
        override lazy val allCsvFilesCacheRetryAmount: Int = 1
      }

    "return result of removePresubmissionData if fetching from the cache is successful" in {
      when(
        mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any())
      ).thenReturn(
        Future.successful(ersRequestObject)
      )
      when(
        mockSessionService.fetch[ErsMetaData](refEq(mockErsUtil.ERS_METADATA))(any(), any())
      ).thenReturn(
        Future.successful(ErsMetaData(SchemeInfo("", ZonedDateTime.now, "", "", "", ""), "", None, "", None, None))
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController.processValidationResults()(authRequest, hc)
      status(result) shouldBe OK
    }

    "direct to ers errors page if fetching metaData from cache fails" in {
      when(
        mockSessionService.fetch[ErsMetaData](anyString())(any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )

      when(
        mockSessionService.fetch[RequestObject](any())(any(), any())
      ).thenReturn(
        Future.successful(ersRequestObject)
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(csvFileUploadController.processValidationResults()(authRequest, hc))

    }

    "direct to ers errors page if fetching requestObject from cache fails" in {
      when(
        mockSessionService.fetch[ErsMetaData](anyString())(any(), any())
      ).thenReturn(
        Future.successful(ErsMetaData(SchemeInfo("", ZonedDateTime.now, "", "", "", ""), "", None, "", None, None))
      )

      when(
        mockSessionService.fetch[RequestObject](anyString())(any(), any())
      ).thenReturn(
        Future.failed(new Exception)
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(csvFileUploadController.processValidationResults()(authRequest, hc))

    }

  }

  "calling removePresubmissionData" should {
    lazy val csvFileUploadController: CsvFileUploadController =
      new CsvFileUploadController(
        mockMCC,
        mockErsConnector,
        mockUpscanService,
        mockSessionService,
        globalErrorView,
        upscanCsvFileUploadView,
        fileSizeLimitErrorView,
        fileUploadErrorsView,
        fileUploadProblemView,
        testAuthAction
      ) {
        override def extractCsvCallbackData(schemeInfo: SchemeInfo)(implicit
          request: RequestWithOptionalAuthContext[AnyContent],
          hc: HeaderCarrier
        ): Future[Result]                                  = Future(Redirect(""))
        override lazy val allCsvFilesCacheRetryAmount: Int = 1
      }

    "return the result of extractCsvCallbackData if deleting presubmission data is successful" in {
      reset(mockErsConnector)
      when(
        mockErsConnector.removePresubmissionData(any[SchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(HttpResponse(OK, ""))
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController.removePresubmissionData(mock[SchemeInfo])(authRequest, hc)
      status(result)                                           shouldBe SEE_OTHER
      result.futureValue.header.headers("Location").equals("") shouldBe true
    }

    "return Internal Server Error and show error page if deleting presubmission data fails" in {
      reset(mockErsConnector)
      when(
        mockErsConnector.removePresubmissionData(any[SchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, ""))
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController.removePresubmissionData(mock[SchemeInfo])(authRequest, hc)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "direct to ers errors page if deleting presubmission data throws exception" in {
      reset(mockErsConnector)
      when(
        mockErsConnector.removePresubmissionData(any[SchemeInfo]())(any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      contentAsBytes(
        csvFileUploadController.removePresubmissionData(mock[SchemeInfo])(authRequest, hc)
      ) shouldBe contentAsBytes(Future(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages)))

    }

  }

  "calling extractCsvCallbackData" should {
    def csvFileUploadControllerWithRetry(retryTimes: Int): CsvFileUploadController =
      new CsvFileUploadController(
        mockMCC,
        mockErsConnector,
        mockUpscanService,
        mockSessionService,
        globalErrorView,
        upscanCsvFileUploadView,
        fileSizeLimitErrorView,
        fileUploadErrorsView,
        fileUploadProblemView,
        testAuthAction
      ) {
        override lazy val allCsvFilesCacheRetryAmount: Int = retryTimes

        override def checkFileNames(csvCallbackData: List[UploadedSuccessfully], schemeInfo: SchemeInfo)(implicit
          request: RequestWithOptionalAuthContext[AnyContent],
          hc: HeaderCarrier
        ): Future[Result] = Future.successful(Ok("Validated"))
      }

    lazy val csvFileUploadController = csvFileUploadControllerWithRetry(1)

    "return global error page" when {
      "fetching data from cache util fails" in {
        when(mockSessionService.fetch[UpscanCsvFilesCallbackList](anyString())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
        contentAsString(
          csvFileUploadController.extractCsvCallbackData(Fixtures.EMISchemeInfo)(authRequest, hc)
        ) shouldBe contentAsString(Future(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages)))
      }

      "data is missing from the cache map" in {
        when(
          mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any())
        ).thenReturn(Future.successful(multipleInPrgoressUpscanCsvFilesList))
        when(
          mockSessionService.fetchAll()(any())
        ) thenReturn Future.successful(
          CacheItem("id", Json.toJson(Map(
            s"${"check-csv-files"}-${testUploadId.value}" ->
              Json.toJson(asUploadStatus(uploadedSuccessfully))
          )).as[JsObject], Instant.now(), Instant.now())
        )
        when(
          mockSessionService.cache(any(), any())(any(), any())
        ) thenReturn Future.successful(sessionPair)

        val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
        val result      = csvFileUploadController.extractCsvCallbackData(Fixtures.EMISchemeInfo)(authRequest, hc)
        status(result)          shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) shouldBe contentAsString(
          Future(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages))
        )
      }
    }

    "call the cache multiple times when the data does not exist the first time" in {
      reset(mockErsUtil, mockSessionService)
      when(mockErsUtil.CHECK_CSV_FILES).thenReturn("check-csv-files")
      when(mockErsUtil.CSV_FILES_UPLOAD).thenReturn("csv-files-upload")

      when(mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any()))
        .thenReturn(Future.successful(multipleInPrgoressUpscanCsvFilesList))

      when(mockSessionService.fetchAll()(any())).thenReturn(
        Future(
          CacheItem("id", Json.toJson(Map(
            s"${"check-csv-files"}-${testUploadId.value}" ->
              Json.toJson(asUploadStatus(uploadedSuccessfully))
          )).as[JsObject], Instant.now(), Instant.now())
        ),
        Future(
          CacheItem("id", Json.toJson(Map(
            s"${"check-csv-files"}-${testUploadId.value}" -> Json.toJson(asUploadStatus(uploadedSuccessfully)),
            s"${"check-csv-files"}-ID1" -> Json.toJson(asUploadStatus(uploadedSuccessfully))
          )).as[JsObject], Instant.now(), Instant.now())
        )
      )
      when(mockSessionService.cache(any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      await(csvFileUploadControllerWithRetry(3).extractCsvCallbackData(Fixtures.EMISchemeInfo)(authRequest, hc))
      verify(mockSessionService, times(2)).fetchAll()(any())
    }

    "return the result of validateCsv if fetching from cache is successful for one file" in {
      reset(mockErsUtil, mockSessionService)
      when(mockErsUtil.CHECK_CSV_FILES).thenReturn("check-csv-files")
      when(mockErsUtil.CSV_FILES_UPLOAD).thenReturn("csv-files-upload")

      when(
        mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any())
      ).thenReturn(Future.successful(inProgressUpscanCsvFilesList))
      when(
        mockSessionService.fetchAll()(any())
      ) thenReturn Future.successful(
        CacheItem("id", Json.toJson(Map(
          s"${"check-csv-files"}-${testUploadId.value}" ->
            Json.toJson(asUploadStatus(uploadedSuccessfully))
        )).as[JsObject], Instant.now(), Instant.now())
      )
      when(
        mockSessionService.cache(any(), any())(any(), any())
      ) thenReturn Future.successful(sessionPair)

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result = csvFileUploadControllerWithRetry(3).extractCsvCallbackData(Fixtures.EMISchemeInfo)(authRequest, hc)

      status(result) shouldBe OK
      contentAsString(result) shouldBe "Validated"

      verify(mockSessionService, times(1)).fetchAll()(any())
    }

    "return the result of validateCsv if fetching from cache is successful for multiple files" in {
      when(
        mockSessionService.fetch[UpscanCsvFilesList](meq("csv-files-upload"))(any(), any())
      ).thenReturn(Future.successful(multipleInPrgoressUpscanCsvFilesList))
      when(
        mockSessionService.fetchAll()(any())
      ) thenReturn Future.successful(
        CacheItem("id", Json.toJson(Map(
          s"${"check-csv-files"}-${testUploadId.value}" -> Json.toJson(asUploadStatus(uploadedSuccessfully)),
          s"${"check-csv-files"}-ID1" -> Json.toJson(asUploadStatus(uploadedSuccessfully))
        )).as[JsObject], Instant.now(), Instant.now())
      )
      when(
        mockSessionService.cache(any(), any())(any(), any())
      ) thenReturn Future.successful(sessionPair)

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController.extractCsvCallbackData(Fixtures.EMISchemeInfo)(authRequest, hc)
      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Validated"
    }

    "direct to ers errors" when {
      "fetching from cache is successful but there is no callbackData" in {
        when(
          mockSessionService.fetch[UpscanCsvFilesCallbackList](anyString())(any(), any())
        ).thenReturn(Future.successful(UpscanCsvFilesCallbackList(List())))

        val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
        contentAsString(
          csvFileUploadController.extractCsvCallbackData(Fixtures.EMISchemeInfo)(authRequest, hc)
        ) shouldBe contentAsString(Future(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages)))
      }

      "one of the files are not complete" in {
        when(
          mockSessionService.fetch[UpscanCsvFilesCallbackList](anyString())(any(), any())
        ).thenReturn(Future.successful(UpscanCsvFilesCallbackList(List())))

        val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
        contentAsString(
          csvFileUploadController.extractCsvCallbackData(Fixtures.EMISchemeInfo)(authRequest, hc)
        ) shouldBe contentAsString(Future(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages)))
      }
    }

  }

  "calling validateCsv" should {

    lazy val csvFileUploadController: CsvFileUploadController =
      new CsvFileUploadController(
        mockMCC,
        mockErsConnector,
        mockUpscanService,
        mockSessionService,
        globalErrorView,
        upscanCsvFileUploadView,
        fileSizeLimitErrorView,
        fileUploadErrorsView,
        fileUploadProblemView,
        testAuthAction
      ) {
        override lazy val allCsvFilesCacheRetryAmount: Int = 1
      }

    "redirect to schemeOrganiserPage if validating is successful" in {
      reset(mockErsConnector)
      when(
        mockErsConnector.validateCsvFileData(any[List[UploadedSuccessfully]](), any[SchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(HttpResponse(OK, ""))
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      =
        csvFileUploadController.validateCsv(mock[List[UploadedSuccessfully]], mock[SchemeInfo])(authRequest, hc)
      status(result)         shouldBe SEE_OTHER
      result.futureValue.header
        .headers("Location") shouldBe controllers.schemeOrganiser.routes.SchemeOrganiserBasedInUkController.questionPage().toString
    }

    "redirect to validationFailure if validating fails" in {
      reset(mockErsConnector)
      when(
        mockErsConnector.validateCsvFileData(any[List[UploadedSuccessfully]](), any[SchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(HttpResponse(ACCEPTED, ""))
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      =
        csvFileUploadController.validateCsv(mock[List[UploadedSuccessfully]], mock[SchemeInfo])(authRequest, hc)
      status(result)                                shouldBe SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe controllers.routes.CsvFileUploadController.validationFailure().toString
    }

    "show error page if validating returns result other than OK and ACCEPTED" in {
      reset(mockErsConnector)
      when(
        mockErsConnector.validateCsvFileData(any[List[UploadedSuccessfully]](), any[SchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, ""))
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      =
        csvFileUploadController.validateCsv(mock[List[UploadedSuccessfully]], mock[SchemeInfo])(authRequest, hc)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "direct to ers errors page if connecting with validator is not successful" in {
      reset(mockErsConnector)
      when(
        mockErsConnector.validateCsvFileData(any[List[UploadedSuccessfully]](), any[SchemeInfo]())(any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      contentAsString(
        csvFileUploadController.validateCsv(mock[List[UploadedSuccessfully]], mock[SchemeInfo])(authRequest, hc)
      ) shouldBe contentAsString(Future(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages)))
    }
  }

  "calling checkFileNames" should {

    lazy val csvFileUploadController: CsvFileUploadController =
      new CsvFileUploadController(
        mockMCC,
        mockErsConnector,
        mockUpscanService,
        mockSessionService,
        globalErrorView,
        upscanCsvFileUploadView,
        fileSizeLimitErrorView,
        fileUploadErrorsView,
        fileUploadProblemView,
        testAuthAction
      ) {
        override lazy val allCsvFilesCacheRetryAmount: Int = 1
      }

    "redirect to validateCsv if file name check is successful" in {
      reset(mockErsConnector)

      val testUploadedSuccessfully   = new UploadedSuccessfully(
        "CSOP_OptionsGranted_V4.csv",
        "http://somedownloadlink.com/034099340"
      )
      val testCsvCallbackData        = List[UploadedSuccessfully](testUploadedSuccessfully)
      val testCacheFileIds           = List[UpscanIds](
        new UpscanIds(new UploadId(Random.nextString(10)), "file0", InProgress),
        new UpscanIds(new UploadId(Random.nextString(10)), "file1", InProgress),
        new UpscanIds(new UploadId(Random.nextString(10)), "file2", InProgress)
      )
      val testUpscanCsvFileList      = new UpscanCsvFilesList(testCacheFileIds)
      val mockSchemeInfo: SchemeInfo = mock[SchemeInfo]

      when(
        mockSessionService.fetch[UpscanCsvFilesList](any())(any(), any())
      ) thenReturn Future.successful(testUpscanCsvFileList)
      when(
        mockErsUtil.getPageElement(any(), any(), any(), any())(any())
      ) thenReturn "CSOP_OptionsGranted_V4.csv"
      when(
        mockErsConnector.validateCsvFileData(any[List[UploadedSuccessfully]](), any[SchemeInfo]())(any(), any())
      ) thenReturn Future.successful(HttpResponse(OK, ""))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController
        .checkFileNames(testCsvCallbackData, mockSchemeInfo)(authRequest, hc)
      status(result)         shouldBe SEE_OTHER
      result.futureValue.header
        .headers("Location") shouldBe controllers.schemeOrganiser.routes.SchemeOrganiserBasedInUkController.questionPage().toString
    }

    "redirect to getFileUploadProblemPage() if file name check is unsuccessful" in {
      reset(mockErsConnector)

      val testUploadedSuccessfully   = new UploadedSuccessfully(
        "CSOP_OptionsExercised_V4",
        "http://somedownloadlink.com/034099340"
      )
      val testCsvCallbackData        = List[UploadedSuccessfully](testUploadedSuccessfully)
      val testCacheFileIds           = List[UpscanIds](
        new UpscanIds(new UploadId(Random.nextString(10)), "file0", InProgress),
        new UpscanIds(new UploadId(Random.nextString(10)), "file1", InProgress),
        new UpscanIds(new UploadId(Random.nextString(10)), "file2", InProgress)
      )
      val testUpscanCsvFileList      = new UpscanCsvFilesList(testCacheFileIds)
      val mockSchemeInfo: SchemeInfo = mock[SchemeInfo]

      when(
        mockSessionService.fetch[UpscanCsvFilesList](any())(any(), any())
      ) thenReturn Future.successful(testUpscanCsvFileList)
      when(
        mockErsUtil.getPageElement(any(), any(), any(), any())(any())
      ) thenReturn "CSOP_OptionsGranted_V4.csv"

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController
        .checkFileNames(testCsvCallbackData, mockSchemeInfo)(authRequest, hc)
      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) shouldBe contentAsString(
        Future(csvFileUploadController.getFileUploadProblemPage()(testFakeRequest, testMessages))
      )
    }

    "redirect to getGlobalErrorPage if checkFileNames throws an exception" in {
      val testUploadedSuccessfully   = new UploadedSuccessfully(
        "CSOP_OptionsExercised_V4",
        "http://somedownloadlink.com/034099340"
      )
      val testCsvCallbackData        = List[UploadedSuccessfully](testUploadedSuccessfully)
      val mockSchemeInfo: SchemeInfo = mock[SchemeInfo]

      when(
        mockSessionService.fetch[UpscanCsvFilesList](any())(any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = csvFileUploadController
        .checkFileNames(testCsvCallbackData, mockSchemeInfo)(authRequest, hc)
      status(result)          shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe contentAsString(
        Future(csvFileUploadController.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }
  }
}
