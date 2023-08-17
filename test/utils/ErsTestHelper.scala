/*
 * Copyright 2023 HM Revenue & Customs
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

import akka.stream.Materializer
import config.{ApplicationConfig, ERSShortLivedCache}
import connectors.ErsConnector
import controllers.auth.{AuthAction, AuthActionGovGateway, RequestWithOptionalAuthContext}
import metrics.Metrics
import models.ERSAuthData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n
import play.api.i18n.MessagesImpl
import play.api.mvc.BodyParsers.Default
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, stubBodyParser, stubControllerComponents, stubMessagesApi}
import play.twirl.api.Html
import services.SessionService
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext

trait ErsTestHelper extends MockitoSugar with AuthHelper with ERSFakeApplicationConfig {

  def doc(result: Html): Document = Jsoup.parse(contentAsString(result))

  val messagesActionBuilder: MessagesActionBuilder =
    new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
  val cc: ControllerComponents                     = stubControllerComponents()
  val mockMaterializer: Materializer               = mock[Materializer]
  val defaultParser                                = new Default()(mockMaterializer)

  def buildRequestWithAuth(
    req: Request[AnyContent],
    authData: ERSAuthData = Fixtures.buildFakeUser
  ): RequestWithOptionalAuthContext[AnyContent] =
    RequestWithOptionalAuthContext(req, authData)

  implicit val hc: HeaderCarrier                        = HeaderCarrier()
  implicit val ec: ExecutionContext                     = cc.executionContext
  implicit val testFakeRequest: FakeRequest[AnyContent] = FakeRequest()
  implicit lazy val messages: MessagesImpl              = MessagesImpl(i18n.Lang("en"), stubMessagesApi())

  val requestWithAuth: RequestWithOptionalAuthContext[AnyContent] =
    RequestWithOptionalAuthContext(testFakeRequest, defaultErsAuthData)

  val OPTION_YES                = "1"
  val OPTION_NO                 = "2"
  val SCHEME_ORGANISER_CACHE    = "scheme-organiser"
  val GROUP_SCHEME_COMPANIES    = "group-scheme-companies"
  val FILE_TYPE_CACHE           = "check-file-type"
  val FILE_NAME_CACHE           = "file-name"
  val CHECK_CSV_FILES           = "check-csv-files"
  val ersMetaData               = "ErsMetaData"
  val REPORTABLE_EVENTS         = "ReportableEvents"
  val ErsRequestObject          = "ErsRequestObject"
  val SCHEME_CSOP               = "1"
  val OPTION_CSV                = "csv"
  val OPTION_ODS                = "ods"
  val OPTION_NIL_RETURN         = "2"
  val OPTION_UPLOAD_SPREADSHEET = "1"

  val mockHttp: DefaultHttpClient             = mock[DefaultHttpClient]
  val mockAppConfig: ApplicationConfig        = mock[ApplicationConfig]
  val mockErsConnector: ErsConnector          = mock[ErsConnector]
  val mockErsUtil: ERSUtil                    = mock[ERSUtil]
  val mockMetrics: Metrics                    = mock[Metrics]
  val mockAuditEvents: AuditEvents            = mock[AuditEvents]
  val mockShortLivedCache: ERSShortLivedCache = mock[ERSShortLivedCache]
  val mockSessionCache: SessionService        = mock[SessionService]
  val mockCountryCodes: CountryCodes          = mock[CountryCodes]

  val testAuthAction    = new AuthAction(mockAuthConnector, mockAppConfig, mockErsUtil, defaultParser)(ec)
  val testAuthActionGov = new AuthActionGovGateway(mockAuthConnector, mockAppConfig, mockErsUtil, defaultParser)(ec)

  when(mockCountryCodes.countriesMap).thenReturn(List(Country("United Kingdom", "UK"), Country("South Africa", "ZA")))

  when(mockAppConfig.ggSignInUrl).thenReturn("http://localhost:9949/gg/sign-in")
  when(mockAppConfig.appName).thenReturn("ers-returns-frontend")
  when(mockAppConfig.loginCallback).thenReturn("http://localhost:9290/submit-your-ers-annual-return")
  when(mockAppConfig.signOut).thenReturn(
    "http://localhost:9025/gg/sign-out?continue=http://localhost:9514/feedback/ERS"
  )
  when(mockAppConfig.sentViaSchedulerNoOfRowsLimit).thenReturn(10000)
  when(mockAppConfig.odsSuccessRetryAmount).thenReturn(5)
  when(mockAppConfig.odsValidationRetryAmount).thenReturn(1)
  when(mockAppConfig.urBannerLink).thenReturn("http://")
  when(mockAppConfig.ampersandRegex).thenReturn("(?!&amp;)(?:&)".r)

  import scala.concurrent.duration._
  when(mockAppConfig.retryDelay).thenReturn(3.milliseconds)

  when(mockErsUtil.PAGE_ALT_ACTIVITY).thenReturn("ers_alt_activity")
  when(mockErsUtil.CSV_FILES_UPLOAD).thenReturn("csv-files-upload")
  when(mockErsUtil.ersRequestObject).thenReturn("ErsRequestObject")
  when(mockErsUtil.GROUP_SCHEME_COMPANIES).thenReturn("group-scheme-companies")
  when(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER).thenReturn("group-scheme-controller")
  when(mockErsUtil.SCHEME_ORGANISER_CACHE).thenReturn("scheme-organiser")
  when(mockErsUtil.ersMetaData).thenReturn("ErsMetaData")
  when(mockErsUtil.CHECK_CSV_FILES).thenReturn("check-csv-files")
  when(mockErsUtil.FILE_TYPE_CACHE).thenReturn("check-file-type")
  when(mockErsUtil.FILE_NAME_CACHE).thenReturn("file-name")
  when(mockErsUtil.reportableEvents).thenReturn("ReportableEvents")
  when(mockErsUtil.TRUSTEES_CACHE).thenReturn("trustees")
  when(mockErsUtil.ALT_AMENDS_CACHE_CONTROLLER).thenReturn("alt-amends-cache-controller")
  when(mockErsUtil.altAmendsActivity).thenReturn("alt-activity")
  when(mockErsUtil.VALIDATED_SHEEETS).thenReturn("validated-sheets")
  when(mockErsUtil.DEFAULT_COUNTRY).thenReturn("UK")
  when(mockErsUtil.DEFAULT).thenReturn("")
  when(mockErsUtil.OPTION_MANUAL).thenReturn("man")

  //PageBuilder Stuff
  when(mockErsUtil.SCHEME_CSOP).thenReturn("1")
  when(mockErsUtil.SCHEME_EMI).thenReturn("2")
  when(mockErsUtil.SCHEME_OTHER).thenReturn("3")
  when(mockErsUtil.SCHEME_SAYE).thenReturn("4")
  when(mockErsUtil.SCHEME_SIP).thenReturn("5")

  when(mockErsUtil.OPTION_CSV).thenReturn("csv")
  when(mockErsUtil.OPTION_ODS).thenReturn("ods")
  when(mockErsUtil.OPTION_YES).thenReturn("1")
  when(mockErsUtil.OPTION_NO).thenReturn("2")
  when(mockErsUtil.OPTION_UPLOAD_SPREEDSHEET).thenReturn("1")
  when(mockErsUtil.OPTION_NIL_RETURN).thenReturn("2")
}
