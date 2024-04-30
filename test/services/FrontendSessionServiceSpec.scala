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

package services

import config.ApplicationConfig
import controllers.auth.RequestWithOptionalAuthContext
import models._
import models.upscan.UploadedSuccessfully
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContent
import repositories.FrontendSessionsRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.MongoSupport
import utils.{ErsTestHelper, Fixtures}

import java.time.Instant
import scala.concurrent.Future

class FrontendSessionServiceSpec extends AnyWordSpec with Matchers with ErsTestHelper with GuiceOneAppPerSuite with MockitoSugar with MongoSupport with BeforeAndAfter {

  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> "false")
    .overrides(
      bind(classOf[MongoComponent]).toInstance(mongoComponent)
    )
    .build()

  implicit val fakeRequest: RequestWithOptionalAuthContext[AnyContent] = requestWithAuth

  val frontendSessionsRepository: FrontendSessionsRepository = mock[FrontendSessionsRepository]
  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]

  val testService = new FrontendSessionService(frontendSessionsRepository, mockFileValidatorService, mockApplicationConfig)

  before {
    reset(frontendSessionsRepository)
    reset(mockFileValidatorService)
    reset(frontendSessionsRepository)
    reset(frontendSessionsRepository)
  }

  "getStatus" should {
    "return 'largefiles' status when the number of rows exceeds the limit" in {
      val limit = 1000
      when(mockApplicationConfig.sentViaSchedulerNoOfRowsLimit).thenReturn(limit)

      val result = testService.getStatus(Some(limit + 1))
      result shouldBe Some("largefiles")
    }

    "return 'saved' status when the number of rows is below the limit" in {
      val limit = 1000
      when(mockApplicationConfig.sentViaSchedulerNoOfRowsLimit).thenReturn(limit)

      val result = testService.getStatus(Some(limit - 1))
      result shouldBe Some("saved")
    }

    "return 'saved' status when the number of rows is exactly at the limit" in {
      val limit = 1000
      when(mockApplicationConfig.sentViaSchedulerNoOfRowsLimit).thenReturn(limit)

      val result = testService.getStatus(Some(limit))
      result shouldBe Some("saved")
    }

    "return 'saved' status when the number of rows is not defined" in {
      val result = testService.getStatus(None)
      result shouldBe Some("saved")
    }
  }

  "isNilReturn" should {
    "return true when the input is equal to OPTION_NIL_RETURN" in {
      val nilReturn = "2" //OPTION_NIL_RETURN
      val result = testService.isNilReturn(nilReturn)
      result shouldBe true
    }

    "return false when the input is not equal to OPTION_NIL_RETURN" in {
      val notNilReturn = "SomeOtherValue"
      val result = testService.isNilReturn(notNilReturn)
      result shouldBe false
    }
  }

  "getNoOfRows" should {
    "return None when nilReturn is 2" in {
      val result = testService.getNoOfRows("2")(ec, fakeRequest, hc).futureValue
      result shouldBe None
    }

    "return Some with the number of rows when nilReturn is not OPTION_NIL_RETURN and rows are available" in {
      val numberOfRows = Some(5)
      when(mockFileValidatorService.getSuccessfulCallbackRecord(any(), any())).thenReturn(Future.successful(Some(UploadedSuccessfully("", "", numberOfRows))))
      val result = testService.getNoOfRows("1").futureValue
      result shouldBe numberOfRows
    }

    "return None when nilReturn is not OPTION_NIL_RETURN and no rows are available" in {
      when(mockFileValidatorService.getSuccessfulCallbackRecord(any(), any())).thenReturn(Future.successful(None))
      val result = testService.getNoOfRows("1").futureValue
      result shouldBe None
    }
  }

  "getGroupSchemeData" should {
    "fetch CompanyDetailsList when GroupSchemeInfo is present and groupScheme is OPTION_YES" in {
      val schemeRef = "testSchemeRef"
      val groupSchemeInfo = GroupSchemeInfo(Some("1"), None)
      val companyDetailsList = CompanyDetailsList(List(CompanyDetails(companyName = "testCompanyName", addressLine1= "testAddressLine", None, None, None, None, country = Some("UK"), None, None, true)))

      when(frontendSessionsRepository.getFromSession[GroupSchemeInfo](DataKey(eqTo("group-scheme-controller")))(any(), any()))
        .thenReturn(Future.successful(Some(groupSchemeInfo)))
      when(frontendSessionsRepository.getFromSession[CompanyDetailsList](DataKey(eqTo("subsidiary-companies")))(any(), any()))
        .thenReturn(Future.successful(Some(companyDetailsList)))

      val result = testService.getGroupSchemeData(schemeRef).futureValue
      result shouldBe(Some(groupSchemeInfo), Some(companyDetailsList))
    }

    "not fetch CompanyDetailsList when GroupSchemeInfo is present but groupScheme is not OPTION_YES" in {
      val schemeRef = "testSchemeRef"
      val groupSchemeInfo = GroupSchemeInfo(Some("2"), None)

      when(frontendSessionsRepository.getFromSession[GroupSchemeInfo](DataKey(eqTo("group-scheme-controller")))(any(), any()))
        .thenReturn(Future.successful(Some(groupSchemeInfo)))

      val result = testService.getGroupSchemeData(schemeRef).futureValue
      result shouldBe (Some(groupSchemeInfo), None)
    }

    "not fetch CompanyDetailsList when GroupSchemeInfo is not present" in {
      val schemeRef = "testSchemeRef"

      when(frontendSessionsRepository.getFromSession[GroupSchemeInfo](DataKey(eqTo("group-scheme-controller")))(any(), any()))
        .thenReturn(Future.successful(None))

      val result = testService.getGroupSchemeData(schemeRef).futureValue
      result shouldBe (None, None)
    }
  }

  "getAltAmmendsData" should {
    "fetch AlterationAmends when AltAmendsActivity is present and altActivity is OPTION_YES" in {
      val schemeRef = "testSchemeRef"
      val altAmendsActivity = AltAmendsActivity("1")
      val alterationAmends = AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1"))

      when(frontendSessionsRepository.getFromSession[AltAmendsActivity](DataKey(eqTo("alt-activity")))(any(), any()))
        .thenReturn(Future.successful(Some(altAmendsActivity)))
      when(frontendSessionsRepository.getFromSession[AlterationAmends](DataKey(eqTo("alt-amends-cache-controller")))(any(), any()))
        .thenReturn(Future.successful(Some(alterationAmends)))

      val result = testService.getAltAmmendsData(schemeRef).futureValue
      result shouldBe(Some(altAmendsActivity), Some(alterationAmends))
    }

    "not fetch AlterationAmends when AltAmendsActivity is present but altActivity is not OPTION_YES" in {
      val schemeRef = "testSchemeRef"
      val altAmendsActivity = AltAmendsActivity("2")

      when(frontendSessionsRepository.getFromSession[AltAmendsActivity](DataKey(eqTo("alt-activity")))(any(), any()))
        .thenReturn(Future.successful(Some(altAmendsActivity)))

      val result = testService.getAltAmmendsData(schemeRef).futureValue
      result shouldBe (Some(altAmendsActivity), None)
    }

    "not fetch AlterationAmends when AltAmendsActivity is not present" in {
      val schemeRef = "testSchemeRef"

      when(frontendSessionsRepository.getFromSession[AltAmendsActivity](DataKey(eqTo("alt-activity")))(any(), any()))
        .thenReturn(Future.successful(None))

      val result = testService.getAltAmmendsData(schemeRef).futureValue
      result shouldBe (None, None)
    }
  }

  "fetchAll" should {
    "return a CacheItem when data is found in the session" in {
      val cacheItem: CacheItem = CacheItem("id", Json.toJson(Map("user1234" -> Json.toJson(Fixtures.ersSummary))).as[JsObject], Instant.now(), Instant.now())

      when(frontendSessionsRepository.getAllFromSession()(any())).thenReturn(Future.successful(Some(cacheItem)))

      val result = testService.fetchAll().futureValue
      result shouldBe cacheItem
    }

    "throw Exception with a specific message when no data is found in the session" in {
      when(frontendSessionsRepository.getAllFromSession()(any())).thenReturn(Future.successful(None))

      val thrown = intercept[Exception] {
        testService.fetchAll().futureValue
      }
      thrown.getMessage should include("[FrontendSessionService][fetchAll] No data found in session")
    }

    "throw Exception when there is an error fetching data" in {
      val exception = new RuntimeException("Test exception")
      when(frontendSessionsRepository.getAllFromSession()(any())).thenReturn(Future.failed(exception))

      val thrown = intercept[Exception] {
        testService.fetchAll().futureValue
      }
      thrown.getMessage should include("[FrontendSessionService][fetchAll] Error fetching all keys")
    }
  }

  "fetchOption" should {
    "return Some(data) when data is successfully found in the session" in {
      val key = "testKey"
      val cacheId = "testCacheId"
      val testData = Some("testData")

      when(frontendSessionsRepository.getFromSession[String](DataKey(eqTo(key)))(any(), any()))
        .thenReturn(Future.successful(testData))

      val result = testService.fetchOption[String](key, cacheId).futureValue
      result shouldBe testData
    }

    "return None when no data is found for the key" in {
      val key = "testKey"
      val cacheId = "testCacheId"

      when(frontendSessionsRepository.getFromSession[String](DataKey(eqTo(key)))(any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException("No data found")))

      val result = testService.fetchOption[String](key, cacheId).futureValue
      result shouldBe None
    }

    "return None and log an error when an unexpected exception occurs" in {
      val key = "testKey"
      val cacheId = "testCacheId"
      val exception = new RuntimeException("Unexpected exception")

      when(frontendSessionsRepository.getFromSession[String](DataKey(eqTo(key)))(any(), any()))
        .thenReturn(Future.failed(exception))

      val result = testService.fetchOption[String](key, cacheId).futureValue
      result shouldBe None
    }
  }

  "fetchPartFromTrusteeDetailsList" should {
    "return Some(data) when data is successfully found at the given index" in {
      val index = 0
      val trusteeDetails = Json.toJson(TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("UK"), None, true))
      val trusteesCacheData = Json.toJson(Map("trustees" -> Json.arr(trusteeDetails)))

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("trustees")))(any(), any()))
        .thenReturn(Future.successful(Some(trusteesCacheData)))

      val result = testService.fetchPartFromTrusteeDetailsList[JsValue](index).futureValue
      result shouldBe Some(trusteeDetails)
    }

    "return None when no data is found at the given index" in {
      val index = 1 // Assuming no data at this index
      val trusteesCacheData = Json.toJson(Map("trustees" -> Json.arr(Json.obj("key" -> "value")))) // Single item in the array

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("trustees")))(any(), any()))
        .thenReturn(Future.successful(Some(trusteesCacheData)))

      val result = testService.fetchPartFromTrusteeDetailsList[JsValue](index).futureValue
      result shouldBe None
    }

    "return None and log an info message when an exception occurs" in {
      val index = 0
      val exception = new RuntimeException("Unexpected exception")

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("trustees")))(any(), any()))
        .thenReturn(Future.failed(exception))

      val result = testService.fetchPartFromTrusteeDetailsList[JsValue](index).futureValue
      result shouldBe None
    }
  }

  "fetchTrusteesOptionally" should {
    "return a TrusteeDetailsList when data is successfully fetched" in {
      val trusteeDetailsList = TrusteeDetailsList(List(TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("UK"), None, true)))
      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("trustees")))(any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(trusteeDetailsList))))

      val result = testService.fetchTrusteesOptionally().futureValue
      result shouldBe trusteeDetailsList
    }

    "return an empty TrusteeDetailsList when fetching fails" in {
      when(frontendSessionsRepository.getFromSession[TrusteeDetailsList](DataKey(eqTo("trustees")))(any(), any()))
        .thenReturn(Future.failed(new Exception("Fetching error")))

      val result = testService.fetchTrusteesOptionally().futureValue
      result shouldBe TrusteeDetailsList(List.empty[TrusteeDetails])
    }
  }

  "fetch" should {
    "return data when it is successfully found in the session" in {
      val key = "testKey"
      val testData = "testData"

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo(key)))(any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(testData))))

      val result = testService.fetch[String](key).futureValue
      result shouldBe testData
    }

    "throw Exception when no data is found for the key" in {
      val key = "testKey"

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo(key)))(any(), any()))
        .thenReturn(Future.successful(None))

      val thrown = intercept[Exception] {
        testService.fetch[String](key).futureValue
      }
      thrown.getMessage should include(s"[FrontendSessionService][fetch] No data found for key $key")
    }

    "throw Exception when an unexpected exception occurs" in {
      val key = "testKey"
      val exception = new RuntimeException("Test exception")

      when(frontendSessionsRepository.getFromSession[String](DataKey(eqTo(key)))(any(), any()))
        .thenReturn(Future.failed(exception))

      val thrown = intercept[Exception] {
        testService.fetch[String](key).futureValue
      }
      thrown.getMessage should include("Test exception")
    }
  }

  "cache" should {
    "successfully cache data" in {
      val key = "testKey"
      val testData = "testData"
      val expectedResponse = ("cacheId", "key")

      when(frontendSessionsRepository.putSession[String](DataKey(eqTo(key)), eqTo(testData))(any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = testService.cache(key, testData).futureValue
      result shouldBe expectedResponse
    }

    "handle exceptions during caching" in {
      val key = "testKey"
      val testData = "testData"
      val exception = new RuntimeException("Cache operation failed")

      when(frontendSessionsRepository.putSession[String](DataKey(eqTo(key)), eqTo(testData))(any(), any()))
        .thenReturn(Future.failed(exception))

      val result = testService.cache(key, testData).failed.futureValue
      result shouldBe a[RuntimeException]
      result.getMessage should include("Cache operation failed")
    }
  }

  "remove" should {
    "successfully remove data from the cache" in {
      val key = "testKey"

      when(frontendSessionsRepository.deleteFromSession(DataKey(eqTo(key)))(any()))
        .thenReturn(Future.successful(()))

      noException shouldBe thrownBy(testService.remove(key).futureValue)
    }

    "handle exceptions during removal" in {
      val key = "testKey"
      val exception = new RuntimeException("Removal operation failed")

      when(frontendSessionsRepository.deleteFromSession(DataKey(eqTo(key)))(any()))
        .thenReturn(Future.failed(exception))

      val result = testService.remove(key).failed.futureValue
      result shouldBe a[RuntimeException]
      result.getMessage should include("Removal operation failed")
    }
  }

  "fetchPartFromCompanyDetailsList" should {
    "return Some(data) when data is successfully found at the given index" in {
      val index = 0
      val companyDetails = Json.toJson(CompanyDetails("First Company", "UK line 1", None, None, None, None, None,None,None, true))
      val companiesCacheData = Json.toJson(Map("companies" -> Json.arr(companyDetails)))

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("subsidiary-companies")))(any(), any()))
        .thenReturn(Future.successful(Some(companiesCacheData)))

      val result = testService.fetchPartFromCompanyDetailsList[JsValue](index).futureValue
      result shouldBe Some(companyDetails)
    }

    "return None when no data is found at the given index" in {
      val index = 1 // Assuming no data at this index
      val companiesCacheData = Json.toJson(Map("subsidiary-companies" -> Json.arr(Json.obj("key" -> "value")))) // Single item in the array

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("subsidiary-companies")))(any(), any()))
        .thenReturn(Future.successful(Some(companiesCacheData)))

      val result = testService.fetchPartFromCompanyDetailsList[JsValue](index).futureValue
      result shouldBe None
    }

    "return None and log an info message when an exception occurs" in {
      val index = 0
      val exception = new RuntimeException("Unexpected exception")

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("subsidiary-companies")))(any(), any()))
        .thenReturn(Future.failed(exception))

      val result = testService.fetchPartFromCompanyDetailsList[JsValue](index).futureValue
      result shouldBe None
    }
  }

  "fetchCompaniesOptionally" should {
    "return a CompanyDetailsList when data is successfully fetched" in {
      val companiesDetailsList = CompanyDetailsList(List(CompanyDetails("First Company", "UK line 1", None, None, None, None, None,None,None, true)))
      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("subsidiary-companies")))(any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(companiesDetailsList))))

      val result = testService.fetchCompaniesOptionally().futureValue
      result shouldBe companiesDetailsList
    }

    "return an empty CompanyDetailsList when fetching fails" in {
      when(frontendSessionsRepository.getFromSession[CompanyDetailsList](DataKey(eqTo("subsidiary-companies")))(any(), any()))
        .thenReturn(Future.failed(new Exception("Fetching error")))

      val result = testService.fetchCompaniesOptionally().futureValue
      result shouldBe CompanyDetailsList(List.empty[CompanyDetails])
    }
  }



  "fetchPartFromCompanyDetails" should {
    "return Some(data) when data is successfully found at the given index" in {

      val companyDetails = Json.toJson(CompanyDetails("First Company", "UK line 1", None, None, None, None, None,None,None, true))
      val companyCacheData = Json.toJson(companyDetails)

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("scheme-organiser")))(any(), any()))
        .thenReturn(Future.successful(Some(companyCacheData)))

      val result = testService.fetchPartFromCompanyDetails[JsValue]().futureValue
      result shouldBe Some(companyDetails)
    }

    "return None when no data is found" in {

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("scheme-organiser")))(any(), any()))
        .thenReturn(Future.successful(None))

      val result = testService.fetchPartFromCompanyDetails[JsValue]().futureValue
      result shouldBe None
    }

    "return None and log an info message when an exception occurs" in {

      val exception = new RuntimeException("Unexpected exception")

      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("scheme-organiser")))(any(), any()))
        .thenReturn(Future.failed(exception))

      val result = testService.fetchPartFromCompanyDetails[JsValue]().futureValue
      result shouldBe None
    }
  }

  "fetchSchemeOrganiserOptionally" should {
    "return a CompanyDetails when data is successfully fetched" in {
      val companyDetails = CompanyDetails("First Company", "UK line 1", None, None, None, None, None,None,None, true)
      when(frontendSessionsRepository.getFromSession[JsValue](DataKey(eqTo("scheme-organiser")))(any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(companyDetails))))

      val result = testService.fetchSchemeOrganiserOptionally().futureValue
      result shouldBe Some(companyDetails)
    }

    "return an empty CompanyDetails when fetching fails" in {
      when(frontendSessionsRepository.getFromSession[CompanyDetails](DataKey(eqTo("scheme-organiser")))(any(), any()))
        .thenReturn(Future.failed(new Exception("Fetching error")))

      val result = testService.fetchSchemeOrganiserOptionally().futureValue
      result shouldBe None
    }
  }
}
