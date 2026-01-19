/*
 * Copyright 2026 HM Revenue & Customs
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

import models._
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.pdf.ErsReceiptPdfBuilderService
import utils.Fixtures._
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.global_error

import java.io.ByteArrayOutputStream
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GeneratePdfControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with ERSFakeApplicationConfig
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
  val globalErrorView: global_error = app.injector.instanceOf[global_error]

  lazy val pdfBuilderMock: ErsReceiptPdfBuilderService = mock[ErsReceiptPdfBuilderService]
  lazy val schemeInfo: SchemeInfo                      = SchemeInfo("XA1100000000000",Instant.now, "1", "2016", "EMI", "EMI")
  lazy val rsc: ErsMetaData                            =
    ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
  lazy val ersSummary: ErsSummary                      =
    ErsSummary("testbundle", "2", None, Instant.now, rsc, None, None, None, None, None, None, None, None)

  "pdf generation controller" should {
    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()

      val controller = new PdfGenerationController(mockMCC, pdfBuilderMock, mockSessionService, globalErrorView, testAuthAction)

      val result = controller.buildPdfForBundle("", "").apply(FakeRequest("GET", ""))

      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val byteArrayOutputStream: ByteArrayOutputStream = mock[ByteArrayOutputStream]
      val testItem = testCacheItem[ReportableEvents](REPORTABLE_EVENTS, ReportableEvents(Some("2")))

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetch[ErsMetaData](refEq(ERS_META_DATA))(any(), any())).thenReturn(Future.successful(rsc))
      when(mockSessionService.getAllData(any(), any())(any(), any(), any())).thenReturn(Future.successful(Fixtures.ersSummary))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(testItem))
      when(pdfBuilderMock.createPdf(any(), any(), any())(any())).thenReturn(byteArrayOutputStream)
      when(byteArrayOutputStream.toByteArray).thenReturn(Array[Byte]())

      val controller = new PdfGenerationController(mockMCC, pdfBuilderMock, mockSessionService, globalErrorView, testAuthAction)
      val result = controller.buildPdfForBundle("", "").apply(buildFakeRequestWithSessionIdEMI("GET"))

      status(result) shouldBe Status.OK
    }

    "direct to errors page if fetch all res pdf throws exception" in {
      val authRequest = buildRequestWithAuth(buildFakeRequestWithSessionIdEMI("GET"))

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetch[ErsMetaData](refEq(ERS_META_DATA))(any(), any())).thenReturn(Future.successful(rsc))
      when(mockSessionService.getAllData(any(), any())(any(), any(), any())).thenReturn(Future.successful(Fixtures.ersSummary))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.failed(new Exception("error")))

      val controller = new PdfGenerationController(mockMCC, pdfBuilderMock, mockSessionService, globalErrorView, testAuthAction)

      val result = controller.generatePdf("", "")(authRequest)

      contentAsString(result) should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(controller.getGlobalErrorPage()(testFakeRequest, testMessages))
      )
    }

    "direct to errors page if get all data res pdf throws exception" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetch[ErsMetaData](refEq(ERS_META_DATA))(any(), any())).thenReturn(Future.successful(rsc))
      when(mockSessionService.getAllData(any(), any())(any(), any(), any())).thenReturn(Future.failed(new Exception("error")))

      val controller = new PdfGenerationController(mockMCC, pdfBuilderMock, mockSessionService, globalErrorView, testAuthAction)

      val result = controller.generatePdf("", "")(authRequest)

      contentAsString(result) should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(controller.getGlobalErrorPage()(testFakeRequest, testMessages))
      )
    }

    "use bundle ref to generate the confirmation pdf filename (NilReturn)" in {
      val byteArrayOutputStream: ByteArrayOutputStream = mock[ByteArrayOutputStream]
      val testItem = testCacheItem[ReportableEvents](REPORTABLE_EVENTS, ReportableEvents(Some("2")))
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      when(mockSessionService.fetch[ErsMetaData](any())(any(), any())).thenReturn(Future.successful(Fixtures.EMIMetaData))
      when(mockSessionService.getAllData(any(), any())(any(), any(), any())).thenReturn(Future.successful(Fixtures.ersSummary))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(testItem))
      when(pdfBuilderMock.createPdf(any(), any(), any())(any())).thenReturn(byteArrayOutputStream)
      when(byteArrayOutputStream.toByteArray).thenReturn(Array[Byte]())

      val controller = new PdfGenerationController(mockMCC, pdfBuilderMock, mockSessionService, globalErrorView, testAuthAction)

      val res = await(controller.generatePdf("123456", "8 August 2016, 4:28pm")(authRequest))

      res.header.headers("Content-Disposition") should include("123456-confirmation.pdf")
    }

    "use bundle ref to generate the confirmation pdf filename (CSV File submission)" in {
      val byteArrayOutputStream: ByteArrayOutputStream = mock[ByteArrayOutputStream]
      val csvFilesCallBack: UpscanCsvFilesCallback =
        UpscanCsvFilesCallback(UploadId("uploadId"), "file0", UploadedSuccessfully("name", "downloadUrl"))
      val csvFilesCallbackList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(List(csvFilesCallBack))

      val testItem = testCacheItem[ReportableEvents](REPORTABLE_EVENTS, ReportableEvents(Some("1")))
      val testItem2 = testCacheItem[CheckFileType](FILE_TYPE_CACHE, CheckFileType(Some("csv")))
      val testItem3 = testCacheItem[UpscanCsvFilesCallbackList](CHECK_CSV_FILES, csvFilesCallbackList)
      val testItems = mergeCacheItems(Seq(testItem, testItem2, testItem3))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      when(mockSessionService.fetch[ErsMetaData](any())(any(), any())).thenReturn(Future.successful(Fixtures.EMIMetaData))
      when(mockSessionService.getAllData(any(), any())(any(), any(), any())).thenReturn(Future.successful(Fixtures.ersSummary))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(testItems))
      when(pdfBuilderMock.createPdf(any(), any(), any())(any())).thenReturn(byteArrayOutputStream)
      when(byteArrayOutputStream.toByteArray).thenReturn(Array[Byte]())

      val controller = new PdfGenerationController(mockMCC, pdfBuilderMock, mockSessionService, globalErrorView, testAuthAction)

      val res = await(controller.generatePdf("123456", "8 August 2016, 4:28pm")(authRequest))

      res.header.headers("Content-Disposition") should include("123456-confirmation.pdf")
    }

    "use bundle ref to generate the confirmation pdf filename (ODS File submission)" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val testItem = testCacheItem[ReportableEvents](REPORTABLE_EVENTS, ReportableEvents(Some("1")))
      val testItem2 = testCacheItem[CheckFileType](FILE_TYPE_CACHE, CheckFileType(Some("ods")))
      val testItem3 = testCacheItem[String](FILE_NAME_CACHE, "someFileName")
      val testItems = mergeCacheItems(Seq(testItem, testItem2, testItem3))

      when(mockSessionService.fetch[ErsMetaData](any())(any(), any())).thenReturn(Future.successful(Fixtures.EMIMetaData))
      when(mockSessionService.getAllData(any(), any())(any(), any(), any())).thenReturn(Future.successful(Fixtures.ersSummary))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(testItems))

      val controller = new PdfGenerationController(mockMCC, pdfBuilderMock, mockSessionService, globalErrorView, testAuthAction)

      val res = await(controller.generatePdf("123456", "8 August 2016, 4:28pm")(authRequest))

      res.header.headers("Content-Disposition") should include("123456-confirmation.pdf")
    }
  }
}
