package controllers.trustees

import models.{GroupSchemeInfo, TrusteeAddressOverseas}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status, stubBodyParser}
import services.TrusteeService
import utils.Fixtures.{ersRequestObject, trusteeAddressOverseas}
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, trustee_address_overseas}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeAddressOverseasSpec  extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with ERSFakeApplicationConfig
  with ErsTestHelper
  with GuiceOneAppPerSuite
  with ScalaFutures {

  implicit val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)


  val testController = new TrusteeAddressOverseasController(
    mockMCC,
    mockAuthConnector,
    mockErsConnector,
    app.injector.instanceOf[global_error],
    testAuthAction,
    app.injector.instanceOf[TrusteeService],
    mockCountryCodes,
    mockErsUtil,
    mockAppConfig,
    app.injector.instanceOf[trustee_address_overseas]
  )

  "calling showQuestionPage" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

    "show the empty trustee address overseas question page when there is nothing to prefill" in {
      when(mockErsUtil.fetch[GroupSchemeInfo](any(), any())(any(), any())).thenReturn(Future.successful(GroupSchemeInfo(None, None)))
      when(mockErsUtil.fetchPartFromTrusteeDetailsList[TrusteeAddressOverseas](any(), any())(any(), any())).thenReturn(Future.successful(None))
      val result = testController.showQuestionPage(ersRequestObject, 1)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_address.title"))
      contentAsString(result) should include(testMessages("ers_trustee_address.line1"))
    }

    "show the prefilled trustee address overseas question page when there is data to prefill" in {
      when(mockErsUtil.fetch[GroupSchemeInfo](any(), any())(any(), any())).thenReturn(Future.successful(GroupSchemeInfo(None, None)))
      when(mockErsUtil.fetchPartFromTrusteeDetailsList[TrusteeAddressOverseas](any(), any())(any(), any())).thenReturn(Future.successful(Some(trusteeAddressOverseas)))
      val result = testController.showQuestionPage(ersRequestObject, 1)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_address.title"))
      contentAsString(result) should include(testMessages("ers_trustee_address.line1"))
      contentAsString(result) should include("Overseas line 1")
    }

    "show the global error page if an exception occurs while retrieving cached data" in {
      when(mockErsUtil.fetch[GroupSchemeInfo](any(), any())(any(), any())).thenThrow(new RuntimeException("oh no"))
      val result = testController.showQuestionPage(ersRequestObject, 1)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers.global_errors.title"))
      contentAsString(result) should include(testMessages("ers.global_errors.heading"))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }

  "calling handleQuestionSubmit" should {
    "show the trustee address overseas form page with errors if the form is incorrectly filled" in {
      when(mockErsUtil.fetch[GroupSchemeInfo](any(), any())(any(), any())).thenReturn(Future.successful(GroupSchemeInfo(None, None)))

    }
  }
}
