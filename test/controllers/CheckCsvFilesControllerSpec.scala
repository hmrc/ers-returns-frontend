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
import models._
import models.upscan._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, Fixtures}

import scala.concurrent.Future

class CheckCsvFilesControllerSpec extends UnitSpec with ERSFakeApplicationConfig with OneAppPerSuite with ErsTestHelper {

	override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
	implicit lazy val mat: Materializer = app.materializer

	lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
	implicit lazy val messages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))

  "calling checkCsvFilesPage" should {

    val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig) {
      override def showCheckCsvFilesPage()(implicit authContext: ERSAuthData,
																					 request: Request[AnyRef],
																					 hc: HeaderCarrier): Future[Result] = Future.successful(Ok)
    }

    "redirect to company authentication frontend if user is not authenticated" in {
			setUnauthorisedMocks()
      val result = checkCsvFilesController.checkCsvFilesPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location").contains("/gg/sign-in") shouldBe true
    }
  }

  "calling showCheckCsvFilesPage" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig) {

      when(mockErsUtil.remove(ArgumentMatchers.eq("check-csv-files"))(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))
    }

    "show CheckCsvFilesPage" in {
      reset(mockErsUtil)
      when(
        mockErsUtil.fetch[RequestObject](any())(any(), any(), any(), any())
      ).thenReturn(ersRequestObject)

      val result = await(checkCsvFilesController.showCheckCsvFilesPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc))
      status(result) shouldBe OK
    }
  }

  "calling checkCsvFilesPage" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig) {
      override def validateCsvFilesPageSelected()(implicit authContext: ERSAuthData,
																									request: Request[AnyRef],
																									hc: HeaderCarrier): Future[Result] = Future.successful(Ok)
    }

    "redirect to company authentication frontend if user is not authenticated to access checkCsvFilesPage" in {
			setUnauthorisedMocks()
      val result = checkCsvFilesController.checkCsvFilesPageSelected().apply(FakeRequest("GET", ""))
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location").contains("/gg/sign-in") shouldBe true
    }
  }

  "calling validateCsvFilesPageSelected" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig) {
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
      val result = await(checkCsvFilesController.validateCsvFilesPageSelected()(Fixtures.buildFakeUser, request, hc))
      status(result) shouldBe OK
    }

    "return the result of reloadWithError if data is invalid" in {

      val csvFilesListData: Map[String, String] = Map(
        ("files[0].fileId", ""),
        ("files[0].isSelected", "5")
      )
      val form = RsFormMappings.csvFileCheckForm.bind(csvFilesListData)

      val request = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = await(checkCsvFilesController.validateCsvFilesPageSelected()(Fixtures.buildFakeUser, request, hc))
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") shouldBe "/submit-your-ers-annual-return/choose-csv-files"
    }

  }

  "calling performCsvFilesPageSelected" should {

    val mockListCsvFilesCallback: UpscanCsvFilesList = mock[UpscanCsvFilesList](Mockito.RETURNS_DEEP_STUBS)

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig) {
      override def createCacheData(csvFilesList: List[CsvFiles]): UpscanCsvFilesList = mockListCsvFilesCallback
    }

    val formData: CsvFilesList = CsvFilesList(
      List(
        CsvFiles("file0", None),
        CsvFiles("file1", Some("1")),
        CsvFiles("file2", None),
        CsvFiles("file3", None),
        CsvFiles("file4", Some("1"))
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

      val result = await(checkCsvFilesController.performCsvFilesPageSelected(formData)(Fixtures.buildFakeRequestWithSessionIdCSOP("POST"), hc))
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") shouldBe "/submit-your-ers-annual-return/choose-csv-files"
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

			val result = await(checkCsvFilesController.performCsvFilesPageSelected(formData)(Fixtures.buildFakeRequestWithSessionIdCSOP("POST"), hc))
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should include ("/upload-")
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

			val result = await(checkCsvFilesController.performCsvFilesPageSelected(formData)(Fixtures.buildFakeRequestWithSessionIdCSOP("POST"), hc))
      contentAsString(result) shouldBe contentAsString(checkCsvFilesController.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
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

			val result = await(checkCsvFilesController.performCsvFilesPageSelected(formData)(Fixtures.buildFakeRequestWithSessionIdCSOP("POST"), hc))
      contentAsString(result) shouldBe contentAsString(checkCsvFilesController.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

  }

  "calling createCacheData" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig)

    val formData: List[CsvFiles] = List(
      CsvFiles("file0", None),
      CsvFiles("file1", Some("1")),
      CsvFiles("file2", None),
      CsvFiles("file3", None),
      CsvFiles("file4", Some("1"))
    )

    "return only selected files" in {
			when(mockErsUtil.OPTION_YES).thenReturn("1")
			val result = checkCsvFilesController.createCacheData(formData)
      result.ids.size shouldBe 2
      result.ids.foreach {
        _ should matchPattern {
          case UpscanIds(UploadId(_), _: String, NotStarted) =>
        }
      }

      result.noOfUploads shouldBe 0
      result.noOfFilesToUpload shouldBe 2
    }

  }

  "calling reloadWithError" should {

		val checkCsvFilesController: CheckCsvFilesController = new CheckCsvFilesController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig)

		"reload same page, showing error" in {
      val result = await(checkCsvFilesController.reloadWithError())
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }
  }

}
