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

package utils

import java.util.NoSuchElementException
import models._
import models.upscan.UploadedSuccessfully
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.http.cache.client.CacheMap
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.test.Helpers.await
import utils.SessionKeys.BUNDLE_REF
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class ErsUtilSpec extends WordSpecLike with Matchers with OptionValues with MockitoSugar with BeforeAndAfterEach with ERSFakeApplicationConfig with ErsTestHelper with ScalaFutures {

  override implicit val hc: HeaderCarrier =  HeaderCarrier(sessionId = Some(SessionId("sessionId")))
  implicit val countryCodes: CountryCodes = mockCountryCodes

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockShortLivedCache)
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
      val result = ersUtil.cache[AltAmends]("alt-amends-cache-controller", altAmends, "123").futureValue
      result.isInstanceOf[CacheMap] shouldBe true
    }

    "saves entry to shortLivedCache by given key and body" in {
      val altAmends = AltAmends(Option("0"), Option("0"), Option("0"), Option("0"), Option("0"))
      when(
        mockShortLivedCache.cache[AltAmends](anyString(), anyString(), any[AltAmends]())(any(), any(), any())
      ).thenReturn(
        Future.successful(mock[CacheMap])
      )
      val result = ersUtil.cache[AltAmends]("alt-amends-cache-controller", altAmends).futureValue
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
      val result = ersUtil.fetch[AltAmends]("key")
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
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal)) thenReturn Future(Option(Json.toJson[String](anyVal)))
      ersUtil.fetch[String](anyVal, anyVal).futureValue shouldBe anyVal
    }

    "throw an NoSuchElementException if nothing is found in the cache" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal)).thenReturn(Future.failed(new NoSuchElementException))
      intercept[NoSuchElementException] {
        await(ersUtil.fetch[String](anyVal, anyVal), 1, SECONDS)
      }
    }

    "throw an exception if an acception occurs" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal)).thenReturn(Future.failed(new RuntimeException))
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
      val cMap = CacheMap(anyVal, Map((anyVal, Json.toJson(anyVal))))
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
    lazy val rsc = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))

    "return valid ERSSummary data" in {
			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){

        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
            case "ReportableEvents" => Future(Some(ReportableEvents(Some(OPTION_NIL_RETURN)).asInstanceOf[T]))
            case "check-file-type" => Future(None)
            case "scheme-organiser" => Future(None)
            case "trustees" => Future(None)
            case "group-scheme-controller" => Future(None)
            case "alt-activity" => Future(None)
            case "alt-amends-cache-controller" => Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
        }
      }
      val result = ersUtil.getAllData(BUNDLE_REF, rsc).futureValue
      result.isNilReturn shouldBe "2"
    }

    "return valid ERSSummary data with correct file type" in {
			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
					key match {
						case "ReportableEvents" => Future(Some(ReportableEvents(Some(OPTION_NIL_RETURN)).asInstanceOf[T]))
						case "check-file-type" => Future(Some(CheckFileType(Some("ods")).asInstanceOf[T]))
						case "scheme-organiser" => Future(None)
						case "trustees" => Future(None)
						case "group-scheme-controller" => Future(None)
						case "alt-activity" => Future(None)
						case "alt-amends-cache-controller" => Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
					}
        }
      }
      val result = ersUtil.getAllData(BUNDLE_REF, rsc).futureValue
      result.isNilReturn shouldBe "2"
      result.fileType shouldBe Option("ods")
    }

    "throws Exception if data is not found" in {
			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
					key match {
						case "ReportableEvents" => Future.failed(new NoSuchElementException)
						case "check-file-type" =>  Future[Option[T]](Option("ods".asInstanceOf[T]))
						case "scheme-organiser" => Future(None)
						case "trustees" => Future(None)
						case "group-scheme-controller" => Future(None)
						case "alt-activity" => Future(None)
						case "alt-amends-cache-controller" => Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
					}
        }
      }
      intercept[Exception] {
        await(ersUtil.getAllData(BUNDLE_REF, rsc), 1, SECONDS)
      }
    }
  }


  "calling getAltAmendsData" should {

    "return (AltAmendsActivity = None, AlterationAmends = None) if AltAmendsActivity = None and AlterationAmends are defined" in {

			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
            case "alt-activity" => Future(None)
            case "alt-amends-cache-controller" => Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
        }
      }

      val result = ersUtil.getAltAmmendsData("").futureValue
      result._1 shouldBe None
      result._2 shouldBe None

    }

    "return (AltAmendsActivity = Some(AltAmendsActivity(\"2\")), AlterationAmends = None) if AltAmendsActivity = \"2\" and AlterationAmends are defined" in {

			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
						case "alt-activity" => Future(Some(AltAmendsActivity(OPTION_NO).asInstanceOf[T]))
						case "alt-amends-cache-controller" => Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
        }
      }

      val result = ersUtil.getAltAmmendsData("").futureValue
      result._1 shouldBe Some(AltAmendsActivity("2"))
      result._2 shouldBe None

    }

    "return the expected AltAmendsActivity and AlterationAmends are defined" in {

			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
            case "alt-activity" => Future(Some(AltAmendsActivity(OPTION_YES).asInstanceOf[T]))
            case "alt-amends-cache-controller" => Future(Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T]))
          }
        }
      }

      val result = ersUtil.getAltAmmendsData("").futureValue
      result._1 shouldBe Some(AltAmendsActivity("1"))
      result._2 shouldBe Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")))

    }

    "return the expected AltAmendsActivity and if AltAmendsActivity = \"1\" and AlterationAmends are not defined" in {

			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
            case "alt-activity" => Future(Some(AltAmendsActivity(OPTION_YES).asInstanceOf[T]))
            case "alt-amends-cache-controller" => Future(None)
          }
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
        CompanyDetails("Company name", "Company address", None, None, None, None, None, None, None)
      )
    )

    "return (GroupSchemeInfo = None, CompanyDetailsList = None) if GroupSchemeInfo = None and CompanyDetailsList is defined" in {
			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
            case "group-scheme-controller" => Future(None)
            case "group-scheme-companies" => Future(Some(schemeCompanies.asInstanceOf[T]))
          }
        }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe None
      result._2 shouldBe None

    }

    "return the expected GroupSchemeInfo and if GroupSchemeInfo.groupScheme = None and CompanyDetailsList are defined" in {
			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
						case "group-scheme-controller" => Future(Some(GroupSchemeInfo(None, Some("")).asInstanceOf[T]))
            case "group-scheme-companies" => Future(Some(schemeCompanies.asInstanceOf[T]))
          }
        }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe Some(GroupSchemeInfo(None, Some("")))
      result._2 shouldBe None

    }

    "return the expected GroupSchemeInfo and if GroupSchemeInfo.groupScheme = 1 and CompanyDetailsList is defined" in {
			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
						case "group-scheme-controller" => Future(Some(GroupSchemeInfo(Some("1"), Some("")).asInstanceOf[T]))
            case "group-scheme-companies" => Future(Some(schemeCompanies.asInstanceOf[T]))
          }
        }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe Some(GroupSchemeInfo(Some("1"), Some("")))
      result._2 shouldBe Some(schemeCompanies)

    }

    "return the expected GroupSchemeInfo and if GroupSchemeInfo.groupScheme = 1 and CompanyDetailsList is not defined" in {
			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
						case "group-scheme-controller" => Future(Some(GroupSchemeInfo(Some("1"), Some("")).asInstanceOf[T]))
            case "group-scheme-companies" => Future(None)
          }
        }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe Some(GroupSchemeInfo(Some("1"), Some("")))
      result._2 shouldBe None

    }

    "return the expected GroupSchemeInfo and if GroupSchemeInfo.groupScheme = 2 and CompanyDetailsList are defined" in {
			val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
        override def fetchOption[T](key: String, cacheId: String)
																	 (implicit hc: HeaderCarrier,
																		formats: json.Format[T],
																		request: Request[AnyRef]
																	 ): Future[Option[T]] = {
          key match {
						case "group-scheme-controller" => Future(Some(GroupSchemeInfo(Some("2"), Some("")).asInstanceOf[T]))
            case "group-scheme-companies" => Future(Some(schemeCompanies.asInstanceOf[T]))
          }
        }
      }

      val result = ersUtil.getGroupSchemeData("").futureValue
      result._1 shouldBe Some(GroupSchemeInfo(Some("2"), Some("")))
      result._2 shouldBe None

    }
  }

  "cacheUtil" should {
		val THOUSAND = 1000
		val ersUtil: ERSUtil = new ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){
      when(sessionService.getSuccessfulCallbackRecord(any(), any())) thenReturn Future(Some(UploadedSuccessfully("name", "downloadUrl", Some(THOUSAND))))
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
      ersUtil.getStatus(Some(THOUSAND*20)).get shouldBe "largefiles"
      ersUtil.getStatus(Some(THOUSAND*10)).get shouldBe "saved"
      ersUtil.getStatus(Some(THOUSAND*9)).get shouldBe "saved"
    }
  }

}
