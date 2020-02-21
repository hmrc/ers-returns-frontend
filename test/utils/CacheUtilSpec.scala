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

package utils

import java.util.NoSuchElementException

import models._
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import services.SessionService
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import utils.SessionKeys.BUNDLE_REF

class CacheUtilSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with OneAppPerSuite with ERSFakeApplicationConfig {

  override implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId("sessionId")))
  implicit lazy val request = FakeRequest()
  lazy val mockShortLivedCache: ShortLivedCache = mock[ShortLivedCache]
  lazy val mockSessionCache: SessionService = mock[SessionService]

  override def beforeEach() = {
    super.beforeEach()
    reset(mockShortLivedCache)
  }

  lazy val cacheUtil: CacheUtil = new CacheUtil {
    override def shortLivedCache: ShortLivedCache = mockShortLivedCache

    override val sessionService: SessionService = mockSessionCache
  }


  "calling cache" should {

    "saves entry to shortLivedCache" in {
      val altAmends = AltAmends(Option("0"), Option("0"), Option("0"), Option("0"), Option("0"))
      when(
        mockShortLivedCache.cache[AltAmends](anyString(), anyString(), any[AltAmends]())(any(), any(), any())
      ).thenReturn(
        Future.successful(mock[CacheMap])
      )
      val result = await(cacheUtil.cache[AltAmends](CacheUtil.ALT_AMENDS_CACHE_CONTROLLER, altAmends, "123"))
      result.isInstanceOf[CacheMap] shouldBe true
    }

    "saves entry to shortLivedCache by given key and body" in {
      val altAmends = AltAmends(Option("0"), Option("0"), Option("0"), Option("0"), Option("0"))
      when(
        mockShortLivedCache.cache[AltAmends](anyString(), anyString(), any[AltAmends]())(any(), any(), any())
      ).thenReturn(
        Future.successful(mock[CacheMap])
      )
      val result = await(cacheUtil.cache[AltAmends](CacheUtil.ALT_AMENDS_CACHE_CONTROLLER, altAmends))
      result.isInstanceOf[CacheMap] shouldBe true
    }

  }

  "calling fetch with key" should {

    "return required value from cache if no cacheId is given" in {
      val altAmends = AltAmends(Option("0"), Option("0"), Option("0"), Option("0"), Option("0"))
      when(
        mockShortLivedCache.fetchAndGetEntry[JsValue](anyString(), anyString())(any(), any(), any())
      ).thenReturn(
        Future.successful(
          Some(Json.toJson(altAmends))
        )
      )
      val result = await(cacheUtil.fetch[AltAmends]("key"))
      result shouldBe altAmends
    }

		"throw NoSuchElementException if value is not found in cache" in {
			when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyString(), anyString())(any(), any(), any()))
				.thenReturn(Future.successful(None))

			intercept[NoSuchElementException] {
				await(cacheUtil.fetch[AltAmends]("key"))
			}
		}

    "throw Exception if an exception occurs" in {
      when(
        mockShortLivedCache.fetchAndGetEntry[JsValue](anyString(), anyString())(any(), any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      intercept[Exception] {
        await(cacheUtil.fetch[AltAmends]("key"))
      }
    }

    "return Future[Something] if given value from cache" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal)).thenReturn(Option(Json.toJson[String](anyVal)))
      await(cacheUtil.fetch[String](anyVal, anyVal)) shouldBe anyVal
    }

    "throw an NoSuchElementException if nothing is found in the cache" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal)).thenReturn(Future.failed(new NoSuchElementException))
      intercept[NoSuchElementException] {
        await(cacheUtil.fetch[String](anyVal, anyVal))
      }
    }

    "throw an exception if an acception occurs" in {
      val anyVal = "abc"
      when(mockShortLivedCache.fetchAndGetEntry[JsValue](anyVal, anyVal)).thenReturn(Future.failed(new RuntimeException))
      intercept[Exception] {
        await(cacheUtil.fetch[String](anyVal, anyVal))
      }
    }
  }

  "calling fetchOption with key" should {

    "return value from cache if it exists" in {
      when(
        mockShortLivedCache.fetchAndGetEntry[String](anyString, anyString())(any(), any(), any())
      ).thenReturn(
        Future.successful(Some(""))
      )
      val result = await(cacheUtil.fetchOption[String]("key", "cacheId"))
      result shouldBe Some("")
    }

    "throw NoSuchElementException if value doesn't exist" in {
      when(
        mockShortLivedCache.fetchAndGetEntry[String](anyString, anyString())(any(), any(), any())
      ).thenReturn(
        Future.failed(new NoSuchElementException)
      )
      intercept[NoSuchElementException] {
        await(cacheUtil.fetchOption[String]("key", "cacheId"))
      }
    }

    "throw Exception if exception occurs" in {
      when(
        mockShortLivedCache.fetchAndGetEntry[String](anyString, anyString())(any(), any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      intercept[Exception] {
        await(cacheUtil.fetchOption[String]("key", "cacheId"))
      }
    }
  }

  "calling fetchAll with key" should {
    "return Future[CacheMap] if given value from cache" in {

      val anyVal = "abc"
      val cMap = CacheMap(anyVal, Map((anyVal, Json.toJson(anyVal))))
      when(mockShortLivedCache.fetch(anyVal)).thenReturn(Future(Option(cMap)))
      await(cacheUtil.fetchAll(anyVal)) shouldBe cMap
    }

    "throw a NoSuchElementException if nothing is found in the cache" in {

      val anyVal = "abc"
      when(mockShortLivedCache.fetch(anyVal)).thenReturn(Future.failed(new NoSuchElementException))
      intercept[NoSuchElementException] {
        await(cacheUtil.fetchAll(anyVal))
      }
    }

    "throw an exception if an exception occurs" in {

      val anyVal = "abc"
      when(mockShortLivedCache.fetch(anyVal)).thenReturn(Future.failed(new RuntimeException))
      intercept[Exception] {
        await(cacheUtil.fetchAll(anyVal))
      }
    }
  }

  "calling getAllData" should {
    lazy val schemeInfo = SchemeInfo("AA0000000000000", DateTime.now, "1", "2016", "CSOP 2015/16", "CSOP")
    lazy val rsc = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))

    "return valid ERSSummary data" in {
      lazy val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.reportableEvents => Future(Some(ReportableEvents(Some(PageBuilder.OPTION_NIL_RETURN)).asInstanceOf[T]))
            case CacheUtil.FILE_TYPE_CACHE => None
            case CacheUtil.SCHEME_ORGANISER_CACHE => None
            case CacheUtil.TRUSTEES_CACHE => None
            case CacheUtil.GROUP_SCHEME_CACHE_CONTROLLER => None
            case CacheUtil.altAmendsActivity => None
            case CacheUtil.ALT_AMENDS_CACHE_CONTROLLER => Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T])
          }
        }
      }
      val result = await(cacheUtil.getAllData(BUNDLE_REF, rsc))
      result.isNilReturn shouldBe PageBuilder.OPTION_NIL_RETURN
    }

    "return valid ERSSummary data with correct file type" in {
      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.reportableEvents => Future(Some(ReportableEvents(Some(PageBuilder.OPTION_NIL_RETURN)).asInstanceOf[T]))
            case CacheUtil.FILE_TYPE_CACHE => Future(Some(CheckFileType(Some("ods")).asInstanceOf[T]))
            case CacheUtil.SCHEME_ORGANISER_CACHE => None
            case CacheUtil.TRUSTEES_CACHE => None
            case CacheUtil.GROUP_SCHEME_CACHE_CONTROLLER => None
            case CacheUtil.altAmendsActivity => None
            case CacheUtil.ALT_AMENDS_CACHE_CONTROLLER => Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T])
          }
        }
      }
      val result = await(cacheUtil.getAllData(BUNDLE_REF, rsc))
      result.isNilReturn shouldBe PageBuilder.OPTION_NIL_RETURN
      result.fileType shouldBe Option("ods")
    }

    "throws Exception if data is not found" in {
      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.reportableEvents => Future.failed(new NoSuchElementException)
            case CacheUtil.FILE_TYPE_CACHE => Future[Option[T]](Option("ods".asInstanceOf[T]))
            case CacheUtil.SCHEME_ORGANISER_CACHE => None
            case CacheUtil.TRUSTEES_CACHE => None
            case CacheUtil.GROUP_SCHEME_CACHE_CONTROLLER => None
            case CacheUtil.altAmendsActivity => None
            case CacheUtil.ALT_AMENDS_CACHE_CONTROLLER => Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T])
          }
        }
      }
      intercept[Exception] {
        await(cacheUtil.getAllData(BUNDLE_REF, rsc))
      }
    }
  }


  "calling getAltAmmendsData" should {

    "return (AltAmendsActivity = None, AlterationAmends = None) if AltAmendsActivity = None and AlterationAmends are defined" in {

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.altAmendsActivity => None
            case CacheUtil.ALT_AMENDS_CACHE_CONTROLLER => Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T])
          }
        }
      }

      val result = await(cacheUtil.getAltAmmendsData(""))
      result._1 shouldBe None
      result._2 shouldBe None

    }

    "return (AltAmendsActivity = Some(AltAmendsActivity(\"2\")), AlterationAmends = None) if AltAmendsActivity = \"2\" and AlterationAmends are defined" in {

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.altAmendsActivity => Some(AltAmendsActivity(PageBuilder.OPTION_NO).asInstanceOf[T])
            case CacheUtil.ALT_AMENDS_CACHE_CONTROLLER => Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T])
          }
        }
      }

      val result = await(cacheUtil.getAltAmmendsData(""))
      result._1 shouldBe Some(AltAmendsActivity("2"))
      result._2 shouldBe None

    }

    "return (AltAmendsActivity = Some(AltAmendsActivity(\"1\")), AlterationAmends = AlterationAmends(Some(\"1\"), Some(\"1\"), Some(\"1\"), Some(\"1\"), Some(\"1\"))) if AltAmendsActivity = \"1\" and AlterationAmends are defined" in {

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.altAmendsActivity => Some(AltAmendsActivity(PageBuilder.OPTION_YES).asInstanceOf[T])
            case CacheUtil.ALT_AMENDS_CACHE_CONTROLLER => Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")).asInstanceOf[T])
          }
        }
      }

      val result = await(cacheUtil.getAltAmmendsData(""))
      result._1 shouldBe Some(AltAmendsActivity("1"))
      result._2 shouldBe Some(AlterationAmends(Some("1"), Some("1"), Some("1"), Some("1"), Some("1")))

    }

    "return (AltAmendsActivity = Some(AltAmendsActivity(\"1\")), AlterationAmends = None) if AltAmendsActivity = \"1\" and AlterationAmends are not defined" in {

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.altAmendsActivity => Some(AltAmendsActivity(PageBuilder.OPTION_YES).asInstanceOf[T])
            case CacheUtil.ALT_AMENDS_CACHE_CONTROLLER => None
          }
        }
      }

      val result = await(cacheUtil.getAltAmmendsData(""))
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

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.GROUP_SCHEME_CACHE_CONTROLLER => None
            case CacheUtil.GROUP_SCHEME_COMPANIES => Some(schemeCompanies.asInstanceOf[T])
          }
        }
      }

      val result = await(cacheUtil.getGroupSchemeData(""))
      result._1 shouldBe None
      result._2 shouldBe None

    }

    "return (GroupSchemeInfo = Some(GroupSchemeInfo(None, Some(\"\"))), CompanyDetailsList = None) if GroupSchemeInfo.groupScheme = None and CompanyDetailsList are defined" in {

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.GROUP_SCHEME_CACHE_CONTROLLER => Some(GroupSchemeInfo(None, Some("")).asInstanceOf[T])
            case CacheUtil.GROUP_SCHEME_COMPANIES => Some(schemeCompanies.asInstanceOf[T])
          }
        }
      }

      val result = await(cacheUtil.getGroupSchemeData(""))
      result._1 shouldBe Some(GroupSchemeInfo(None, Some("")))
      result._2 shouldBe None

    }

    "return (GroupSchemeInfo = Some(GroupSchemeInfo(Some(\"1\"), Some(\"\"))), CompanyDetailsList = schemeCompanies) if GroupSchemeInfo.groupScheme = 1 and CompanyDetailsList is defined" in {

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.GROUP_SCHEME_CACHE_CONTROLLER => Some(GroupSchemeInfo(Some("1"), Some("")).asInstanceOf[T])
            case CacheUtil.GROUP_SCHEME_COMPANIES => Some(schemeCompanies.asInstanceOf[T])
          }
        }
      }

      val result = await(cacheUtil.getGroupSchemeData(""))
      result._1 shouldBe Some(GroupSchemeInfo(Some("1"), Some("")))
      result._2 shouldBe Some(schemeCompanies)

    }

    "return (GroupSchemeInfo = Some(GroupSchemeInfo(Some(\"1\"), Some(\"\"))), CompanyDetailsList = None) if GroupSchemeInfo.groupScheme = 1 and CompanyDetailsList is not defined" in {

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.GROUP_SCHEME_CACHE_CONTROLLER => Some(GroupSchemeInfo(Some("1"), Some("")).asInstanceOf[T])
            case CacheUtil.GROUP_SCHEME_COMPANIES => None
          }
        }
      }

      val result = await(cacheUtil.getGroupSchemeData(""))
      result._1 shouldBe Some(GroupSchemeInfo(Some("1"), Some("")))
      result._2 shouldBe None

    }

    "return (GroupSchemeInfo = Some(GroupSchemeInfo(Some(\"2\"), Some(\"\"))), CompanyDetailsList = None) if GroupSchemeInfo.groupScheme = 2 and CompanyDetailsList are defined" in {

      val cacheUtil: CacheUtil = new CacheUtil {
        override def shortLivedCache: ShortLivedCache = mockShortLivedCache

        override val sessionService: SessionService = mockSessionCache

        override def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
          key match {
            case CacheUtil.GROUP_SCHEME_CACHE_CONTROLLER => Some(GroupSchemeInfo(Some("2"), Some("")).asInstanceOf[T])
            case CacheUtil.GROUP_SCHEME_COMPANIES => Some(schemeCompanies.asInstanceOf[T])
          }
        }
      }

      val result = await(cacheUtil.getGroupSchemeData(""))
      result._1 shouldBe Some(GroupSchemeInfo(Some("2"), Some("")))
      result._2 shouldBe None

    }
  }

  "cacheUtil" should {
    val cacheUtil: CacheUtil = new CacheUtil {
      override def shortLivedCache: ShortLivedCache = mockShortLivedCache

      val postData = CallbackData(id = "theid", collection = "thecollection", length = 1000L, name = Some("thefilename"), contentType = None, sessionId = Some("testId"), customMetadata
        = None, noOfRows = Some(1000))
      override val sessionService: SessionService = mockSessionCache
      when(sessionService.retrieveCallbackData()(any(), any())).thenReturn(Future.successful(Some
      (postData)))

    }
    "check Nil Return " in {
      cacheUtil.isNilReturn("2") shouldBe true
      cacheUtil.isNilReturn("1") shouldBe false
    }
    "get No of rows of a submission" in {
      val result = Await.result(cacheUtil.getNoOfRows("2"), 10 seconds)
      result shouldBe None
      val result1 = Await.result(cacheUtil.getNoOfRows("1"), 10 seconds)
      result1.get shouldBe 1000

    }

    "getStatus" in {
      cacheUtil.getStatus(Some(20000)).get shouldBe "largefiles"
      cacheUtil.getStatus(Some(10000)).get shouldBe "saved"
      cacheUtil.getStatus(Some(9000)).get shouldBe "saved"
    }
  }

}
