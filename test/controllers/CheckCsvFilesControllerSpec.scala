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
import models._
import models.upscan._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{check_csv_file, global_error}

import scala.concurrent.{ExecutionContext, Future}

class CheckCsvFilesControllerSpec extends WordSpecLike with Matchers with OptionValues with ERSFakeApplicationConfig with ErsTestHelper with GuiceOneAppPerSuite with ScalaFutures {

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

	implicit lazy val mat: Materializer = app.materializer
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val checkCsvFileView: check_csv_file = app.injector.instanceOf[check_csv_file]

  "calling checkCsvFilesPage" should {

    val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(mockMCC, mockAuthConnector, mockErsUtil, mockAppConfig, globalErrorView, checkCsvFileView) {
      override def showCheckCsvFilesPage()(implicit authContext: ERSAuthData,
																					 request: Request[AnyRef],
																					 hc: HeaderCarrier): Future[Result] = Future.successful(Ok)
    }

    "redirect to company authentication frontend if user is not authenticated" in {
			setUnauthorisedMocks()
      val result = checkCsvFilesController.checkCsvFilesPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe SEE_OTHER
      result.futureValue.header.headers("Location").contains("/gg/sign-in") shouldBe true
    }
  }

  "calling showCheckCsvFilesPage" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(mockMCC, mockAuthConnector, mockErsUtil, mockAppConfig, globalErrorView, checkCsvFileView) {

      when(mockErsUtil.remove(ArgumentMatchers.eq("check-csv-files"))(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))
    }

    "show CheckCsvFilesPage" in {
      reset(mockErsUtil)
      when(
        mockErsUtil.fetch[RequestObject](any())(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      val result = checkCsvFilesController.showCheckCsvFilesPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
      status(result) shouldBe OK
    }
  }

  "calling checkCsvFilesPage" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(mockMCC, mockAuthConnector, mockErsUtil, mockAppConfig, globalErrorView, checkCsvFileView) {
      override def validateCsvFilesPageSelected()(implicit authContext: ERSAuthData,
																									request: Request[AnyRef],
																									hc: HeaderCarrier): Future[Result] = Future.successful(Ok)
    }

    "redirect to company authentication frontend if user is not authenticated to access checkCsvFilesPage" in {
			setUnauthorisedMocks()
      val result = checkCsvFilesController.checkCsvFilesPageSelected().apply(FakeRequest("GET", ""))
      status(result) shouldBe SEE_OTHER
      result.futureValue.header.headers("Location").contains("/gg/sign-in") shouldBe true
    }
  }

  "calling validateCsvFilesPageSelected" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(mockMCC, mockAuthConnector, mockErsUtil, mockAppConfig, globalErrorView, checkCsvFileView) {
      override def performCsvFilesPageSelected(formData: CsvFilesList)
																							(implicit request: Request[AnyRef],
																							 hc: HeaderCarrier): Future[Result] = Future.successful(Ok)
    }

    "return the result of performCsvFilesPageSelected if data is valid" in {

      val csvFilesListData: Map[String, String] = Map(
        ("files[0].fileId", "file0"),
        ("files[0].isSelected", "1"),
        ("files[1].fileId", "file1"),
        ("files[1].isSelected", "2"),
        ("files[2].fileId", "file2"),
        ("files[2].isSelected", "")
      )
      val form = RsFormMappings.csvFileCheckForm.bind(csvFilesListData)

      val request = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = checkCsvFilesController.validateCsvFilesPageSelected()(Fixtures.buildFakeUser, request, hc)
      status(result) shouldBe OK
    }

    "return the result of reloadWithError if data is invalid" in {

      val csvFilesListData: Map[String, String] = Map(
        ("files[0].fileId", ""),
        ("files[0].isSelected", "5")
      )
      val form = RsFormMappings.csvFileCheckForm.bind(csvFilesListData)

      val request = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = checkCsvFilesController.validateCsvFilesPageSelected()(Fixtures.buildFakeUser, request, hc)
      status(result) shouldBe SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe "/submit-your-ers-annual-return/choose-csv-files"
    }

  }

  "calling performCsvFilesPageSelected" should {

    val mockListCsvFilesCallback: UpscanCsvFilesList = mock[UpscanCsvFilesList](Mockito.RETURNS_DEEP_STUBS)

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(mockMCC, mockAuthConnector, mockErsUtil, mockAppConfig, globalErrorView, checkCsvFileView) {
      override def createCacheData(csvFilesList: List[CsvFiles]): UpscanCsvFilesList = mockListCsvFilesCallback
    }

    val formData: CsvFilesList = CsvFilesList(
      List(
        CsvFiles("file0"),
        CsvFiles("file1"),
        CsvFiles("file2"),
        CsvFiles("file3"),
        CsvFiles("file4")
      )
    )

    "return the result of reloadWithError if createCacheData returns empty list" in {
      reset(mockListCsvFilesCallback)
      reset(mockErsUtil)
      when(
        mockErsUtil.fetch[RequestObject](refEq("ErsRequestObject"))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)
      when(
        mockListCsvFilesCallback.ids.isEmpty
      ).thenReturn(true)

      val result = checkCsvFilesController.performCsvFilesPageSelected(formData)(Fixtures.buildFakeRequestWithSessionIdCSOP("POST"), hc)
      status(result) shouldBe SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe "/submit-your-ers-annual-return/choose-csv-files"
    }

    "redirect to next page if createCacheData returns list with data and caching is successful" in {
      reset(mockListCsvFilesCallback)
      reset(mockErsUtil)
      when(
        mockErsUtil.fetch[RequestObject](refEq("ErsRequestObject"))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      when(
        mockErsUtil.cache(anyString(), any[UpscanCsvFilesCallbackList](), anyString())(any(), any(), any())
      ) thenReturn Future.successful(mock[CacheMap])

			when(mockErsUtil.ersRequestObject).thenReturn("ErsRequestObject")
			when(mockErsUtil.CSV_FILES_UPLOAD).thenReturn("csv-files-upload")

			val result = checkCsvFilesController.performCsvFilesPageSelected(formData)(Fixtures.buildFakeRequestWithSessionIdCSOP("POST"), hc)
      status(result) shouldBe SEE_OTHER
      result.futureValue.header.headers("Location") should include ("/upload-")
    }

    "direct to ers errors page if createCacheData returns list with data and caching fails" in {
      reset(mockListCsvFilesCallback)
      reset(mockErsUtil)
      when(
        mockErsUtil.fetch[RequestObject](refEq("ErsRequestObject"))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      when(
        mockErsUtil.cache(anyString(), any[UpscanCsvFilesCallbackList](), anyString())(any(), any(), any())
      ) thenReturn Future.failed(new RuntimeException)

			when(mockErsUtil.ersRequestObject).thenReturn("ErsRequestObject")

			val result = checkCsvFilesController.performCsvFilesPageSelected(formData)(Fixtures.buildFakeRequestWithSessionIdCSOP("POST"), hc)
      contentAsString(result) shouldBe contentAsString(Future(checkCsvFilesController.getGlobalErrorPage))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }

    "direct to ers errors page if fetching request object fails fails" in {
      reset(mockListCsvFilesCallback)
      reset(mockErsUtil)
      when(
        mockErsUtil.fetch[RequestObject](refEq("ErsRequestObject"))(any(), any(), any(), any())
      ) thenReturn Future.failed(new Exception)

      when(
        mockErsUtil.cache(anyString(), any[UpscanCsvFilesCallbackList](), anyString())(any(), any(), any())
      ) thenReturn Future.successful(mock[CacheMap])

			when(mockErsUtil.ersRequestObject).thenReturn("ErsRequestObject")

			val result = checkCsvFilesController.performCsvFilesPageSelected(formData)(Fixtures.buildFakeRequestWithSessionIdCSOP("POST"), hc)
      contentAsString(result) shouldBe contentAsString(Future(checkCsvFilesController.getGlobalErrorPage))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }

  }

  "calling createCacheData" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(mockMCC, mockAuthConnector, mockErsUtil, mockAppConfig, globalErrorView, checkCsvFileView)

    val formData: List[CsvFiles] = List(
      CsvFiles("file0"),
      CsvFiles("file1"),
      CsvFiles("file2"),
      CsvFiles("file3"),
      CsvFiles("file4")
    )

    "return only selected files" in {
			when(mockErsUtil.OPTION_YES).thenReturn("1")
			val result = checkCsvFilesController.createCacheData(formData)
      result.ids.size shouldBe 5
      result.ids.foreach {
        _ should matchPattern {
          case UpscanIds(UploadId(_), _: String, NotStarted) =>
        }
      }

      result.noOfUploads shouldBe 0
      result.noOfFilesToUpload shouldBe 5
    }

  }

  "calling reloadWithError" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(mockMCC, mockAuthConnector, mockErsUtil, mockAppConfig, globalErrorView, checkCsvFileView)

		"reload same page, showing error" in {
      val result = checkCsvFilesController.reloadWithError()
      status(result) shouldBe SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }
  }

}
