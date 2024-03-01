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

package utils

import models._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json
import play.api.libs.json.{Format, JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.cache.client.CacheMap
import play.api.i18n.Messages
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

class ErsUtilSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach
    with ERSFakeApplicationConfig
    with ErsTestHelper
    with ScalaFutures {

  override implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId")))
  implicit val countryCodes: CountryCodes = mockCountryCodes

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockShortLivedCache)
    reset(mockSessionService)
  }

  "calling cache" should {
    val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig)

    "saves entry to shortLivedCache" in {
      val altAmends = AltAmends(Option("0"), Option("0"), Option("0"), Option("0"), Option("0"))
      when(
        mockShortLivedCache.cache[AltAmends](anyString(), anyString(), any[AltAmends]())(any(), any(), any())
      ).thenReturn(
        Future.successful(mock[CacheMap])
      )
      val result    = ersUtil.cache[AltAmends]("alt-amends-cache-controller", altAmends, "123").futureValue
      result.isInstanceOf[CacheMap] shouldBe true
    }

    "saves entry to shortLivedCache by given key and body" in {
      val altAmends = AltAmends(Option("0"), Option("0"), Option("0"), Option("0"), Option("0"))
      when(
        mockShortLivedCache.cache[AltAmends](anyString(), anyString(), any[AltAmends]())(any(), any(), any())
      ).thenReturn(
        Future.successful(mock[CacheMap])
      )
      val result    = ersUtil.cache[AltAmends]("alt-amends-cache-controller", altAmends).futureValue
      result.isInstanceOf[CacheMap] shouldBe true
    }

  }

  "calling fetch with key" should {
    val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig)

    "return required value from cache if no cacheId is given" in {
      val altAmends = AltAmends(Option("0"), Option("0"), Option("0"), Option("0"), Option("0"))
      when(
        mockShortLivedCache.fetchAndGetEntry[JsValue](anyString(), anyString())(any(), any(), any())
      ).thenReturn(
        Future.successful(
          Some(Json.toJson(altAmends))
        )
      )
      val result    = ersUtil.fetch[AltAmends]("key")
      result.futureValue shouldBe altAmends
    }

    "throw NoSuchElementException if value is not found in cache" in {
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyString(), anyString())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      intercept[NoSuchElementException] {
        await(ersUtil.fetch[AltAmends]("key"), 1, SECONDS)
      }
    }

    "throw Exception if an exception occurs" in {
      when(
        mockShortLivedCache.fetchAndGetEntry[JsValue](anyString(), anyString())(any(), any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      intercept[Exception] {
        await(ersUtil.fetch[AltAmends]("key"), 1, SECONDS)
      }
    }

    "return Future[Something] if given value from cache" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal)) thenReturn Future(
        Option(Json.toJson[String](anyVal))
      )
      ersUtil.fetch[String](anyVal, anyVal).futureValue shouldBe anyVal
    }

    "throw an NoSuchElementException if nothing is found in the cache" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal))
        .thenReturn(Future.failed(new NoSuchElementException))
      intercept[NoSuchElementException] {
        await(ersUtil.fetch[String](anyVal, anyVal), 1, SECONDS)
      }
    }

    "throw an exception if an exception occurs" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal))
        .thenReturn(Future.failed(new RuntimeException))
      intercept[Exception] {
        await(ersUtil.fetch[String](anyVal, anyVal), 1, SECONDS)
      }
    }
  }

  "calling fetchOption with key" should {
    val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig)

    "return value from cache if it exists" in {
      when(
        mockShortLivedCache.fetchAndGetEntry[String](anyString, anyString())(any(), any(), any())
      ).thenReturn(
        Future.successful(Some(""))
      )
      val result = ersUtil.fetchOption[String]("key", "cacheId").futureValue
      result shouldBe Some("")
    }

    "throw NoSuchElementException if value doesn't exist" in {
      when(
        mockShortLivedCache.fetchAndGetEntry[String](anyString, anyString())(any(), any(), any())
      ).thenReturn(
        Future.failed(new NoSuchElementException)
      )
      intercept[NoSuchElementException] {
        await(ersUtil.fetchOption[String]("key", "cacheId"), 1, SECONDS)
      }
    }

    "throw Exception if exception occurs" in {
      when(
        mockShortLivedCache.fetchAndGetEntry[String](anyString, anyString())(any(), any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      intercept[Exception] {
        await(ersUtil.fetchOption[String]("key", "cacheId"), 1, SECONDS)
      }
    }
  }

  "calling fetchAll with key" should {
    val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig)

    "return Future[CacheMap] if given value from cache" in {
      val anyVal = "abc"
      val cMap   = CacheMap(anyVal, Map((anyVal, Json.toJson(anyVal))))
      when(mockShortLivedCache.fetch(anyVal)).thenReturn(Future(Option(cMap)))
      await(ersUtil.fetchAll(anyVal), 1, SECONDS) shouldBe cMap
    }

    "throw a NoSuchElementException if nothing is found in the cache" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetch(anyVal)).thenReturn(Future.failed(new NoSuchElementException))
      intercept[NoSuchElementException] {
        await(ersUtil.fetchAll(anyVal), 1, SECONDS)
      }
    }

    "throw an exception if an exception occurs" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetch(anyVal)).thenReturn(Future.failed(new RuntimeException))
      intercept[Exception] {
        await(ersUtil.fetchAll(anyVal), 1, SECONDS)
      }
    }
  }

  "calling getAllData" should {
    lazy val schemeInfo = SchemeInfo("AA0000000000000", DateTime.now, "1", "2016", "CSOP 2015/16", "CSOP")
    lazy val rsc        = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))

    "return valid ERSSummary data" in {
      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {

        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "ReportableEvents"            => Future(Some(ReportableEvents(Some(OPTION_NIL_RETURN)).asInstanceOf[T]))
            case "check-file-type"             => Future(None)
            case "scheme-organiser"            => Future(None)
            case "trustees"                    => Future(None)
            case "group-scheme-controller"     => Future(None)
            case "alt-activity"                => Future(None)
            case "alt-amends-cache-controller" =>
              Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
      }
      val result = ersUtil.getAllData(BUNDLE_REF, rsc).futureValue
      result.isNilReturn shouldBe "2"
    }

    "return valid ERSSummary data with correct file type" in {
      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: Format[T]
        ): Future[Option[T]] =
          key match {
            case "ReportableEvents"            => Future(Some(ReportableEvents(Some(OPTION_NIL_RETURN)).asInstanceOf[T]))
            case "check-file-type"             => Future(Some(CheckFileType(Some("ods")).asInstanceOf[T]))
            case "scheme-organiser"            => Future(None)
            case "trustees"                    => Future(None)
            case "group-scheme-controller"     => Future(None)
            case "alt-activity"                => Future(None)
            case "alt-amends-cache-controller" =>
              Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
      }
      val result           = ersUtil.getAllData(BUNDLE_REF, rsc).futureValue
      result.isNilReturn shouldBe "2"
      result.fileType    shouldBe Option("ods")
    }

    "throws Exception if data is not found" in {
      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "ReportableEvents"            => Future.failed(new NoSuchElementException)
            case "check-file-type"             => Future[Option[T]](Option("ods".asInstanceOf[T]))
            case "scheme-organiser"            => Future(None)
            case "trustees"                    => Future(None)
            case "group-scheme-controller"     => Future(None)
            case "alt-activity"                => Future(None)
            case "alt-amends-cache-controller" =>
              Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
      }
      intercept[Exception] {
        await(ersUtil.getAllData(BUNDLE_REF, rsc), 1, SECONDS)
      }
    }
  }

  "calling getAltAmendsData" should {

    "return (AltAmendsActivity = None, AlterationAmends = None) if AltAmendsActivity = None and AlterationAmends are defined" in {

      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "alt-activity"                => Future(None)
            case "alt-amends-cache-controller" =>
              Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
      }

      val result = ersUtil.getAltAmmendsData("").futureValue
      result._1 shouldBe None
      result._2 shouldBe None

    }

    "return (AltAmendsActivity = Some(AltAmendsActivity(\"2\")), AlterationAmends = None) if AltAmendsActivity = \"2\" and AlterationAmends are defined" in {

      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "alt-activity"                => Future(Some(AltAmendsActivity(OPTION_NO).asInstanceOf[T]))
            case "alt-amends-cache-controller" =>
              Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
      }

      val result = ersUtil.getAltAmmendsData("").futureValue
      result._1 shouldBe Some(AltAmendsActivity("2"))
      result._2 shouldBe None

    }

    "return the expected AltAmendsActivity and AlterationAmends are defined" in {

      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "alt-activity"                => Future(Some(AltAmendsActivity(OPTION_YES).asInstanceOf[T]))
            case "alt-amends-cache-controller" =>
              Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
      }

      val result = ersUtil.getAltAmmendsData("").futureValue
      result._1 shouldBe Some(AltAmendsActivity("1"))
      result._2 shouldBe Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")))

    }

    "return the expected AltAmendsActivity and if AltAmendsActivity = \"1\" and AlterationAmends are not defined" in {

      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "alt-activity"                => Future(Some(AltAmendsActivity(OPTION_YES).asInstanceOf[T]))
            case "alt-amends-cache-controller" => Future(None)
          }
      }

      val result = ersUtil.getAltAmmendsData("").futureValue
      result._1 shouldBe Some(AltAmendsActivity("1"))
      result._2 shouldBe None

    }

  }

  "calling getGroupSchemeData" should {

    val schemeCompanies = CompanyDetailsList(
      List(
        CompanyDetails("Company name", "Company address", None, None, None, None, None, None, None, true)
      )
    )

    "return (GroupSchemeInfo = None, CompanyDetailsList = None) if GroupSchemeInfo = None and CompanyDetailsList is defined" in {
      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "group-scheme-controller" => Future(None)
            case "group-scheme-companies"  => Future(Some(schemeCompanies.asInstanceOf[T]))
          }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe None
      result._2 shouldBe None

    }

    "return the expected GroupSchemeInfo and if GroupSchemeInfo.groupScheme = None and CompanyDetailsList are defined" in {
      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "group-scheme-controller" => Future(Some(GroupSchemeInfo(None, Some("")).asInstanceOf[T]))
            case "group-scheme-companies"  => Future(Some(schemeCompanies.asInstanceOf[T]))
          }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe Some(GroupSchemeInfo(None, Some("")))
      result._2 shouldBe None

    }

    "return the expected GroupSchemeInfo and if GroupSchemeInfo.groupScheme = 1 and CompanyDetailsList is defined" in {
      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "group-scheme-controller" => Future(Some(GroupSchemeInfo(Some("1"), Some("")).asInstanceOf[T]))
            case "group-scheme-companies"  => Future(Some(schemeCompanies.asInstanceOf[T]))
          }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe Some(GroupSchemeInfo(Some("1"), Some("")))
      result._2 shouldBe Some(schemeCompanies)

    }

    "return the expected GroupSchemeInfo and if GroupSchemeInfo.groupScheme = 1 and CompanyDetailsList is not defined" in {
      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "group-scheme-controller" => Future(Some(GroupSchemeInfo(Some("1"), Some("")).asInstanceOf[T]))
            case "group-scheme-companies"  => Future(None)
          }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe Some(GroupSchemeInfo(Some("1"), Some("")))
      result._2 shouldBe None

    }

    "return the expected GroupSchemeInfo and if GroupSchemeInfo.groupScheme = 2 and CompanyDetailsList are defined" in {
      val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
        override def fetchOption[T](key: String, cacheId: String)(implicit
          hc: HeaderCarrier,
          formats: json.Format[T]
        ): Future[Option[T]] =
          key match {
            case "group-scheme-controller" => Future(Some(GroupSchemeInfo(Some("2"), Some("")).asInstanceOf[T]))
            case "group-scheme-companies"  => Future(Some(schemeCompanies.asInstanceOf[T]))
          }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe Some(GroupSchemeInfo(Some("2"), Some("")))
      result._2 shouldBe None

    }
  }

  "cacheUtil" should {
    val THOUSAND         = 1000
    val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig) {
      when(sessionService.getSuccessfulCallbackRecord(any())) thenReturn Future(
        Some(UploadedSuccessfully("name", "downloadUrl", Some(THOUSAND)))
      )
    }
    "check Nil Return " in {
      ersUtil.isNilReturn("2") shouldBe true
      ersUtil.isNilReturn("1") shouldBe false
    }
    "get No of rows of a submission" in {
      val result = Await.result(ersUtil.getNoOfRows("2"), 10 seconds)
      result shouldBe None
      val result1 = Await.result(ersUtil.getNoOfRows("1"), 10 seconds)
      result1.get shouldBe THOUSAND

    }

    "getStatus" in {
      ersUtil.getStatus(Some(THOUSAND * 20)).get shouldBe "largefiles"
      ersUtil.getStatus(Some(THOUSAND * 10)).get shouldBe "saved"
      ersUtil.getStatus(Some(THOUSAND * 9)).get  shouldBe "saved"
    }
  }

  "calling buildAddressSummary" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "build an address summary from CompanyDetails" in {
      val companyDetails = CompanyDetails(
        "name",
         addressLine1 = "ADDRESS1",
         addressLine2 = Some("ADDRESS2"),
         addressLine3 = None,
         addressLine4 = None,
         addressLine5 = Some("AB123CD"),
         country = Some("UK"),
         companyReg = Some("ABC"),
         corporationRef = Some("DEF"),
        basedInUk = true
      )
      val expected = "ADDRESS1, ADDRESS2, AB123CD, United Kingdom"
      val addressSummary = ersUtil.buildAddressSummary(companyDetails)
      assert(addressSummary == expected)
    }

    "build an address summary from TrusteeDetails" in {
      val companyDetails = TrusteeDetails(
        name = "NAME",
        addressLine1 = "ADDRESS1",
        addressLine2 = Some("ADDRESS2"),
        addressLine3 = None,
        addressLine4 = None,
        country = Some("UK"),
        addressLine5 = Some("AB123CD"),
        basedInUk = true
      )
      val expected = "ADDRESS1, ADDRESS2, AB123CD, United Kingdom"
      val addressSummary = ersUtil.buildAddressSummary(companyDetails)
      assert(addressSummary == expected)
    }

    "build an empty String for anything else" in {
      val expected = ""
      assert(ersUtil.buildAddressSummary(null) == expected)
      assert(ersUtil.buildAddressSummary("Hello") == expected)
      assert(ersUtil.buildAddressSummary(3.14) == expected)
    }
  }

  "calling replaceAmpersand" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "do nothing to a string with no ampersands" in {
      val input = "I am some test input"
      ersUtil.replaceAmpersand(input) shouldBe "I am some test input"
    }

    "replace any ampersands with &amp;" in {
      val input = "I am some test input & stuff &"
      ersUtil.replaceAmpersand(input) shouldBe "I am some test input &amp; stuff &amp;"
    }

    "not affect any &amp; that already exists" in {
      val input = "I am some test input & stuff &amp;"
      ersUtil.replaceAmpersand(input) shouldBe "I am some test input &amp; stuff &amp;"
    }
  }

  "fetchPartFromCompanyDetailsList" should {
    val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig)

    "return the requested data from SUBSIDIARY_COMPANIES_CACHE" in {
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](any(), any())(any(),any(),any()))
        .thenReturn(Future.successful(Some(Json.toJson(Fixtures.exampleCompanies))))

    val result = ersUtil.fetchPartFromCompanyDetailsList[Company](0, "cacheId")
    await(result) shouldBe Some(Company("Company1", Some("AA123456"), Some("1234567890")))
    }

    "return nothing when the cache is empty" in {
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = ersUtil.fetchPartFromCompanyDetailsList[Company](0, "cacheId")
      await(result) shouldBe None
    }
  }

  "fetchPartFromCompanyDetails" should {
    val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig)

    "return the requested data from SCHEME_ORGANISER_CACHE" in {
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(Fixtures.exampleSchemeOrganiserUk))))

      val result = ersUtil.fetchPartFromCompanyDetails[Company]("cacheId")
      await(result) shouldBe Some(Company("Company1", Some("AA123456"), Some("1234567890")))
    }

    "return nothing when the cache is empty" in {
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = ersUtil.fetchPartFromCompanyDetails[Company]("cacheId")
      await(result) shouldBe None
      }
    }

  "fetchPartFromTrusteeDetailsList" should {
    val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig)

    "return the requested data from TRUSTEES_CACHE" in {
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](any(), any())(any(),any(),any()))
        .thenReturn(Future.successful(Some(Json.toJson(Fixtures.exampleTrustees))))

      val result = ersUtil.fetchPartFromTrusteeDetailsList[TrusteeName](0, "cacheId")
      await(result) shouldBe Some(TrusteeName("John Bonson"))
    }

    "return nothing when the cache is empty" in {
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = ersUtil.fetchPartFromTrusteeDetailsList[Company](0, "cacheId")
      await(result) shouldBe None
    }
  }






  "concatEntity" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "concatenate all defined strings with existing entity lines" in {
      val optionalLines = List(Some("Line2"), Some("Line3"))
      val existingLines = "Company, Line1"
      ersUtil.concatEntity(optionalLines, existingLines) shouldBe "Company, Line1, Line2, Line3"
    }

    "handle Some and None values correctly" in {
      val optionalLines = List(Some("Line2"), None, Some("Line3"))
      val existingLines = "Company, Line1"
      ersUtil.concatEntity(optionalLines, existingLines) shouldBe "Company, Line1, Line2, Line3"
    }

    "return existing lines only when all optional lines are None" in {
      val optionalLines = List(None, None)
      val existingLines = "Company, Line1"
      ersUtil.concatEntity(optionalLines, existingLines) shouldBe "Company, Line1"
    }
  }

  "buildEntitySummary" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "build summary with all fields present" in {
      val entity = SchemeOrganiserDetails("Company", "Line1", Some("Line2"), Some("Line3"), Some("Line4"), Some("Country"), Some("Postcode"), Some("Reg"), Some("Ref"))
      ersUtil.buildEntitySummary(entity) shouldBe "Company, Line1, Line2, Line3, Line4, Country, Postcode, Reg, Ref"
    }

    "handle missing optional fields" in {
      val entity = SchemeOrganiserDetails("Company", "Line1", None, Some("Line3"), None, Some("Country"), None, None, None)
      ersUtil.buildEntitySummary(entity) shouldBe "Company, Line1, Line3, Country"
    }
  }

  "buildCompanyNameList" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "handle an empty list" in {
      ersUtil.buildCompanyNameList(List.empty) shouldBe ""
    }

    "handle a non-empty list" in {
      val companies = List(
        CompanyDetails(companyName = "Company1", addressLine1= "", None, None, None, country = Some("UK"), None, None, None),
        CompanyDetails(companyName = "Company2", addressLine1= "", None, None, None, country = Some("UK"), None, None, None))
      ersUtil.buildCompanyNameList(companies) shouldBe "Company1<br>Company2<br>"
    }
  }

  "buildTrusteeNameList" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "handle an empty list" in {
      ersUtil.buildTrusteeNameList(List.empty) shouldBe ""
    }

    "handle a non-empty list" in {
      val trustees = List(
        TrusteeDetails("Trustee1", "1 The Street", None, None, None, Some("UK"), None, true),
        TrusteeDetails("Trustee2", "1 The Street", None, None, None, Some("UK"), None, true))
      ersUtil.buildTrusteeNameList(trustees) shouldBe "Trustee1<br>Trustee2<br>"
    }
  }

  "companyLocation" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "return OVERSEAS for non-default country" in {
      ersUtil.companyLocation(CompanyDetails(companyName = "", addressLine1= "", None, None, None, country = Some("FR"), None, None, None)) shouldBe "Overseas"
    }

    "return default country name" in {
      ersUtil.companyLocation(CompanyDetails(companyName = "", addressLine1= "", None, None, None, country = Some("UK"), None, None, None)) shouldBe "UK"
    }

    "return DEFAULT for None country" in {
      ersUtil.companyLocation(CompanyDetails(companyName = "", addressLine1= "", None, None, None, country = None, None, None, None)) shouldBe ""
    }
  }

  "trusteeLocationMessage" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "return ers_trustee_based.uk for UK-based trustee" in {
      ersUtil.trusteeLocationMessage(TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("UK"), None, true)) shouldBe "ers_trustee_based.uk"
    }

    "return ers_trustee_based.overseas for overseas-based trustee" in {
      ersUtil.trusteeLocationMessage(TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("FR"), None, false)) shouldBe "ers_trustee_based.overseas"
    }
  }

  "addCompanyMessage" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "return appropriate message for Some scheme option" in {
      val messages = mock[Messages]
      when(messages.apply("ers_group_summary.csop.add_company")).thenReturn("Add CSOP company")

      ersUtil.addCompanyMessage(messages, Some("CSOP")) shouldBe "Add CSOP company"
    }

    "return appropriate message for None scheme option" in {
      val messages = mock[Messages]
      when(messages.apply("ers_group_summary..add_company")).thenReturn("Add company")

      ersUtil.addCompanyMessage(messages, None) shouldBe "Add company"
    }
  }
}
