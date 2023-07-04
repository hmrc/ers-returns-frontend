package controllers.subsidiaries

import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, await, contentType, status}
import uk.gov.hmrc.http.InternalServerException
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import org.mockito.ArgumentMatchers._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class SubCompanyDetailsUKControllerSpec {

  "Show" should {
    "return ok (200)" when {
      "the connector returns data" in {

        mockAuthSuccess()
        mockFetchAllBusinesses(
          Right(Seq(selfEmploymentData))
        )
        val result = TestBusinessNameController.show(id,isEditMode = false)(fakeRequest)
        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
      }
      "the connector returns no data" in {
        mockAuthSuccess()
        mockFetchAllBusinesses(
          Right(Seq(selfEmploymentData.copy(businessName = None)))
        )

        val result = TestBusinessNameController.show(id,isEditMode = false)(fakeRequest)
        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
      }
    }
    "Throw an internal exception error" when {
      "the connector returns an error" in {
        mockAuthSuccess()
        mockFetchAllBusinesses(
          Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )
        intercept[InternalServerException](await(TestBusinessNameController.show(id,isEditMode = false)(fakeRequest)))
      }
    }

  }

}
