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

package services.pdf

import akka.stream.Materializer
import models.{AltAmendsActivity, AlterationAmends, ErsSummary}
import org.joda.time.DateTime
import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec
import utils.{CountryCodes, ERSFakeApplicationConfig, Fixtures}

class PdfDecoratorFactoryControllerSpec extends UnitSpec with MockitoSugar with ERSFakeApplicationConfig with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val materializer: Materializer = app.materializer

  lazy val altAmends: AlterationAmends = AlterationAmends(altAmendsTerms = Some("1"),
    altAmendsEligibility = Some("1"),
    altAmendsExchange = Some("1"),
    altAmendsVariations = Some("1"),
    altAmendsOther = Some("1")
  )

  lazy val ersSummary: ErsSummary = ErsSummary(
    bundleRef = "",
    isNilReturn = "",
    fileType = None,
    confirmationDateTime = new DateTime(2016, 6, 8, 11, 45),
    metaData = Fixtures.EMIMetaData,
    altAmendsActivity = Some(AltAmendsActivity(altActivity = "1")),
    alterationAmends = Some(altAmends),
    groupService = Some(Fixtures.groupScheme),
    schemeOrganiser = Some(Fixtures.schemeOrganiserDetails),
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus = None
  )

  // a function to get matching an instance to be of certain type
  def anInstanceOf[T](implicit manifest: Manifest[T]): BePropertyMatcher[AnyRef] = {
    val clazz = manifest.runtimeClass.asInstanceOf[Class[T]]
    new BePropertyMatcher[AnyRef] {
      def apply(left: AnyRef): BePropertyMatchResult =
        BePropertyMatchResult(clazz.isAssignableFrom(left.getClass), "an instance of " + clazz.getName)
    }
  }

	class TestPdfDecoratorControllerFactory extends PdfDecoratorControllerFactory {
		val mockCountryCodes: CountryCodes = mock[CountryCodes]
		override val countryCodes: CountryCodes = mockCountryCodes
	}


  "when scheme is not defiend pdf decorator factory" should {

    "throw invalid argument" in new TestPdfDecoratorControllerFactory {
      intercept[IllegalArgumentException] {
        createPdfDecoratorControllerForScheme("blah", Fixtures.ersSummary, None)
      }
    }
  }

  "When scheme is emi, pdf decorator controller factory" should {

    "add 5 decorators" in new TestPdfDecoratorControllerFactory {

      val ersSummary: ErsSummary = ErsSummary("testbundle",
				"1",
				None,
				new DateTime(2016, 6, 8, 11, 45),
				metaData = Fixtures.EMIMetaData,
				None,
				None,
        Some(Fixtures.groupScheme),
				Some(Fixtures.schemeOrganiserDetails),
				Some(Fixtures.companiesList),
				None,
				None,
				None
			)

      val decoratorController: DecoratorController = createPdfDecoratorControllerForScheme("emi", ersSummary, None)
      val decorators: Array[Decorator] = decoratorController.decorators

      decoratorController.decorators.size shouldEqual 5
      decorators(0) should be(anInstanceOf[YesNoDecorator])
      decorators(1) should be(anInstanceOf[FileNamesDecorator])
      decorators(2) should be(anInstanceOf[SchemeOrganiserDetailsDecorator])
      decorators(3) should be(anInstanceOf[YesNoDecorator])
      decorators(4) should be(anInstanceOf[GroupSummaryDecorator])
    }
  }

  "when scheme is csop, pdf decorator controller factory" should {

    "add 7 decorators" in new TestPdfDecoratorControllerFactory {

      val decoratorController: DecoratorController = createPdfDecoratorControllerForScheme("csop", ersSummary, None)
      val decorators: Array[Decorator] = decoratorController.decorators

      decoratorController.decorators.length shouldEqual 7
      decorators(0) should be(anInstanceOf[YesNoDecorator])
      decorators(1) should be(anInstanceOf[FileNamesDecorator])
      decorators(2) should be(anInstanceOf[SchemeOrganiserDetailsDecorator])
      decorators(3) should be(anInstanceOf[YesNoDecorator])
      decorators(4) should be(anInstanceOf[GroupSummaryDecorator])
      decorators(5) should be(anInstanceOf[YesNoDecorator])
      decorators(6) should be(anInstanceOf[AlterationsAmendsDecorator])
    }
  }

  "when scheme is sip, pdf decorator controller factory" should {

    "add 8 decorators" in new TestPdfDecoratorControllerFactory {

      val decoratorController: DecoratorController = createPdfDecoratorControllerForScheme("sip", ersSummary, None)
      val decorators: Array[Decorator] = decoratorController.decorators

      decoratorController.decorators.length shouldEqual 8
      decorators(0) should be(anInstanceOf[YesNoDecorator])
      decorators(1) should be(anInstanceOf[FileNamesDecorator])
      decorators(2) should be(anInstanceOf[SchemeOrganiserDetailsDecorator])
      decorators(3) should be(anInstanceOf[YesNoDecorator])
      decorators(4) should be(anInstanceOf[GroupSummaryDecorator])
      decorators(5) should be(anInstanceOf[TrusteesDecorator])
      decorators(6) should be(anInstanceOf[YesNoDecorator])
      decorators(7) should be(anInstanceOf[AlterationsAmendsDecorator])
    }
  }

  "when scheme is saye, pdf decorator controller factory" should {

    "add 6 decorators" in new TestPdfDecoratorControllerFactory {

      val decoratorController: DecoratorController = createPdfDecoratorControllerForScheme("saye", ersSummary, None)
      val decorators: Array[Decorator] = decoratorController.decorators

      decoratorController.decorators.length shouldEqual 6
      decorators(0) should be(anInstanceOf[YesNoDecorator])
      decorators(1) should be(anInstanceOf[FileNamesDecorator])
      decorators(2) should be(anInstanceOf[SchemeOrganiserDetailsDecorator])
      decorators(3) should be(anInstanceOf[YesNoDecorator])
      decorators(4) should be(anInstanceOf[YesNoDecorator])
      decorators(5) should be(anInstanceOf[AlterationsAmendsDecorator])
    }
  }

  "when scheme is other, pdf decorator controller factory" should {

    "add 5 decorators" in new TestPdfDecoratorControllerFactory {

      val decoratorController: DecoratorController = createPdfDecoratorControllerForScheme("other", ersSummary, None)
      val decorators: Array[Decorator] = decoratorController.decorators

      decoratorController.decorators.length shouldEqual 5
      decorators(0) should be(anInstanceOf[YesNoDecorator])
      decorators(1) should be(anInstanceOf[FileNamesDecorator])
      decorators(2) should be(anInstanceOf[SchemeOrganiserDetailsDecorator])
      decorators(3) should be(anInstanceOf[YesNoDecorator])
      decorators(4) should be(anInstanceOf[GroupSummaryDecorator])
    }
  }

  "pdf decorator controller factory" should {
    "map given alt amends for given schemes" in new TestPdfDecoratorControllerFactory {

      Array("csop", "sip", "saye").map { scheme =>
        val mappedAltAmends = createAltAmendOptionsFor(ersSummary, scheme)

        mappedAltAmends("title") shouldEqual Messages("ers_trustee_summary.altamends.section")
        mappedAltAmends("option1") shouldEqual Messages(s"ers_alt_amends.$scheme.option_1")
        mappedAltAmends("option2") shouldEqual Messages(s"ers_alt_amends.$scheme.option_2")
        mappedAltAmends("option3") shouldEqual Messages(s"ers_alt_amends.$scheme.option_3")
        mappedAltAmends("option4") shouldEqual Messages(s"ers_alt_amends.$scheme.option_4")
        mappedAltAmends("option5") shouldEqual Messages(s"ers_alt_amends.$scheme.option_5")
      }
    }
  }
}
