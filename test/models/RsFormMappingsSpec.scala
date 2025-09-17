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

package models

import models.RsFormMappings._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import utils.ErsTestHelper

import scala.concurrent.ExecutionContext

class RsFormMappingsSpec extends PlaySpec with ErsTestHelper with GuiceOneAppPerSuite {

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  "companyName" must {
    "return an error if companyName missing" in {
      val postData = Json.obj(
        companyNameFields.companyName -> ""

      )
      val validatedForm = companyNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.summary.company_name_required"))
    }

    "return an error if companyName size too large" in {
      val postData = Json.obj(
        companyNameFields.companyName -> randomString(121)

      )
      val validatedForm = companyNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.company_name"))
    }

    "return an error if companyName contains invalid chars" in {
      val postData = Json.obj(
        companyNameFields.companyName -> "<script>rm *.*</script>"
      )
      val validatedForm = companyNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.company_name"))
    }

    "return an error if companyReg size too large" in {
      val postData = Json.obj(
        companyNameFields.companyName -> " company name",
        companyNameFields.companyReg -> randomString(121)

      )
      val validatedForm = companyNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyReg)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.company_reg"))
    }

    "return an error if companyReg contains invalid chars" in {
      val postData = Json.obj(
        companyNameFields.companyName -> " company name",
        companyNameFields.companyReg -> "<script>rm *.*</script>"
      )
      val validatedForm = companyNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyReg)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.company_reg"))
    }

    "return an error if corporationRef contains invalid chars" in {
      val postData = Json.obj(
        companyNameFields.companyName -> " company name",
        companyNameFields.companyReg -> "12345678",
        companyNameFields.corporationRef -> "<script>rm *.*</script>"
      )
      val validatedForm = companyNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.corporationRef)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.corporation_ref"))
    }
  }

    "companyAddressUk" must {
    "return an error if addressLine1 missing" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> ""
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.summary.address_line1_required"))
    }

    "return an error if addressLine1 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> randomString(28)
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line1"))
    }

    "return an error if addressLine1 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line1"))
    }

    "return an error if addressLine2 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> randomString(28)
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line2"))
    }

    "return an error if addressLine2 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line2"))
    }

    "return an error if addressLine3 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> randomString(28)
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line3"))
    }

    "return an error if addressLine3 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line3"))
    }

    "return an error if addressLine4 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "Address Line 3",
        companyAddressFields.addressLine4 -> randomString(30)
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine4)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line4"))
    }

    "return an error if addressLine4 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "Address Line 3",
        companyAddressFields.addressLine4 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine4)
      val errors = validatedForm.errors.map(formError => formError.messages.head)
      assert(errors.contains(Messages("ers_manual_company_details.err.invalidChars.address_line4")))
    }

    "return an error if postCode size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "Address Line 3",
        companyAddressFields.addressLine4 -> "Address Line 4",
        companyAddressFields.addressLine5 -> randomString(9)
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
    }

    "return an error if postCode contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "Address Line 3",
        companyAddressFields.addressLine4 -> "Address Line 4",
        companyAddressFields.addressLine5 ->"??&&$$"
      )
      val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
    }

      "accept postcode if space is missing space" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "EC1A1BB"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.isEmpty)
      }

      "accept a valid postcode with lowercase" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 ->"sw1a 1aa"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.isEmpty)
      }

      "accept a valid postcode with mixed case" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 ->"Sw1A 1aA"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.isEmpty)
      }

      //Added postcode.trim in isValidPostcode for leading/trailing spaces in postcode
      "accept a valid postcode with leading/trailing spaces" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> " M1 1AE "
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.isEmpty)
      }

      "accept a valid UK postcodes" in {
        val validUKPostcodes = Seq(
          "EC1A 1BB",
          "W1A 0AX",
          "M1 1AE",
          "B33 8TH",
          "CR2 6XH",
          "DN55 1PT",
          "GIR 0AA", // Special case
          "SW1A 1AA",
          "L1 8JQ",
          "BS98 1TL",
          "BX1 1LT",
          "BX9 1AS",
          "BX5 5AT"
        )
        validUKPostcodes.foreach { postcode =>
          val postData = Json.obj(
            companyAddressFields.addressLine1 -> "Address Line 1",
            companyAddressFields.addressLine5 -> postcode
          )
          val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
          assert(validatedForm.errors.isEmpty)
        }
      }

      "return an error if postcode GIR0AA is without space" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "GIR0AA"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode has invalid separator" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "W1A-0AX"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode has invalid inward code" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "SW1A 1A1"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode has invalid outward code" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "ZZ1 1ZZ"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode contains symbols" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 ->  "CR2 6X#"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postCode size too small" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 ->  "A1"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode contains tab character" in {
        val postData = Json.obj(
          companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 ->  "EC1A\t1BB"
        )
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode has double space between segments" in {
        val postData = Json.obj(companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "DN5  1PT")
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode is numeric only" in {
        val postData = Json.obj(companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "123456")
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode is alphabetic only" in {
        val postData = Json.obj(companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "ABCDE")
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode has extra segment" in {
        val postData = Json.obj(companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "EC1A 1BB 1AA")
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode is missing inward code" in {
        val postData = Json.obj(companyAddressFields.addressLine1 -> "Address Line 1",
          companyAddressFields.addressLine5 -> "EC1A")
        val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
        assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
        assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
      }

      "return an error if postcode has invalid area letters Q, V, X" in {
        val invalidAreaPostcodes = Seq("Q1A 1BB", "V1A 1BB", "X1A 1BB")
        invalidAreaPostcodes.foreach { postcode =>
          val postData = Json.obj(
            companyAddressFields.addressLine1 -> "Address Line 1",
            companyAddressFields.addressLine5 -> postcode
          )
          val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
          assert(validatedForm.errors.head.key == companyAddressFields.addressLine5, s"Failed for postcode: $postcode")
          assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"), s"Unexpected error message for postcode: $postcode")
        }
      }

      "return an error if postcode has invalid second letter I, J, Z" in {
        val invalidAreaPostcodes = Seq("AI1A 1BB", "AJ2A 1BB", "AZ3A 1BB")
        invalidAreaPostcodes.foreach { postcode =>
          val postData = Json.obj(
            companyAddressFields.addressLine1 -> "Address Line 1",
            companyAddressFields.addressLine5 -> postcode
          )
          val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
          assert(validatedForm.errors.head.key == companyAddressFields.addressLine5, s"Failed for postcode: $postcode")
          assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"), s"Unexpected error message for postcode: $postcode")
        }
      }

      "return an error if postcode has invalid inward code letters C,I,K,M,O,V" in {
        val invalidAreaPostcodes = Seq("EC1A 1CB", "EC1A 1IB", "EC1A 1KB","EC1A 1MB", "EC1A 1OB", "EC1A 1VB")
        invalidAreaPostcodes.foreach { postcode =>
          val postData = Json.obj(
            companyAddressFields.addressLine1 -> "Address Line 1",
            companyAddressFields.addressLine5 -> postcode
          )
          val validatedForm = companyAddressUkForm().bind(postData, Form.FromJsonMaxChars)
          assert(validatedForm.errors.head.key == companyAddressFields.addressLine5, s"Failed for postcode: $postcode")
          assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"), s"Unexpected error message for postcode: $postcode")
        }
      }

    }

  "companyAddressOverseas" must {
    "return an error if addressLine1 missing" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> ""
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.summary.address_line1_required"))
    }

    "return an error if addressLine1 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> randomString(28)
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line1"))
    }

    "return an error if addressLine1 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line1"))
    }

    "return an error if addressLine2 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> randomString(28)
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line2"))
    }

    "return an error if addressLine2 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line2"))
    }

    "return an error if addressLine3 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> randomString(28)
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line3"))
    }

    "return an error if addressLine3 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line3"))
    }

    "return an error if addressLine4 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "Address Line 3",
        companyAddressFields.addressLine4 -> randomString(30)
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine4)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line4"))
    }

    "return an error if addressLine4 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "Address Line 3",
        companyAddressFields.addressLine4 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine4)
      val errors = validatedForm.errors.map(formError => formError.messages.head)
      assert(errors.contains(Messages("ers_manual_company_details.err.invalidChars.address_line4")))
    }

    "return an error if addressLine5 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "Address Line 3",
        companyAddressFields.addressLine4 -> "Address Line 4",
        companyAddressFields.addressLine5 -> randomString(19)
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line5"))
    }

    "return an error if addressLine5 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "Address Line 3",
        companyAddressFields.addressLine4 -> "Address Line 4",
        companyAddressFields.addressLine5 ->"??&&$$"
      )
      val validatedForm = companyAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line5"))
    }
  }

  "trusteeName" must {
    "return an error if trusteeName missing" in {
      val postData = Json.obj(
        trusteeNameFields.name -> ""

      )
      val validatedForm = trusteeNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeNameFields.name)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.summary.name_required"))
    }

    "return an error if trusteeName size too large" in {
      val postData = Json.obj(
        trusteeNameFields.name -> randomString(121)

      )
      val validatedForm = trusteeNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeNameFields.name)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.name"))
    }

    "return an error if trusteeName contains invalid chars" in {
      val postData = Json.obj(
        trusteeNameFields.name -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeNameForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeNameFields.name)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.invalidChars.name"))
    }
  }

  "trusteeAddressUk" must {
    "return an error if addressLine1 missing" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> ""
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.summary.address_line1_required"))
    }

    "return an error if addressLine1 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> randomString(28)
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line1"))
    }

    "return an error if addressLine1 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.invalidChars.address_line1"))
    }

    "return an error if addressLine2 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> randomString(28)
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line2"))
    }

    "return an error if addressLine2 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.invalidChars.address_line2"))
    }

    "return an error if addressLine3 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> randomString(28)
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line3"))
    }

    "return an error if addressLine3 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.invalidChars.address_line3"))
    }

    "return an error if addressLine4 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "Address Line 3",
        trusteeAddressFields.addressLine4 -> randomString(30)
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine4)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line4"))
    }

    "return an error if addressLine4 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "Address Line 3",
        trusteeAddressFields.addressLine4 -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine4)
      val errors = validatedForm.errors.map(formError => formError.messages.head)
      assert(errors.contains(Messages("ers_trustee_details.err.invalidChars.address_line4")))
    }

    "return an error if postCode size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "Address Line 3",
        trusteeAddressFields.addressLine4 -> "Address Line 4",
        trusteeAddressFields.addressLine5 -> randomString(9)
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.postcode"))
    }

    "return an error if postCode contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "Address Line 3",
        trusteeAddressFields.addressLine4 -> "Address Line 4",
        trusteeAddressFields.addressLine5 ->"??&&$$"
      )
      val validatedForm = trusteeAddressUkForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.postcode"))
    }
  }

  "trusteeAddressOverseas" must {
    "return an error if addressLine1 missing" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> ""
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.summary.address_line1_required"))
    }

    "return an error if addressLine1 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> randomString(28)
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line1"))
    }

    "return an error if addressLine1 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.invalidChars.address_line1"))
    }

    "return an error if addressLine2 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> randomString(28)
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line2"))
    }

    "return an error if addressLine2 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.invalidChars.address_line2"))
    }

    "return an error if addressLine3 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> randomString(28)
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line3"))
    }

    "return an error if addressLine3 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.invalidChars.address_line3"))
    }

    "return an error if addressLine4 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "Address Line 3",
        trusteeAddressFields.addressLine4 -> randomString(30)
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine4)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line4"))
    }

    "return an error if addressLine4 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "Address Line 3",
        trusteeAddressFields.addressLine4 -> "<script>rm *.*</script>"
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine4)
      val errors = validatedForm.errors.map(formError => formError.messages.head)
      assert(errors.contains(Messages("ers_trustee_details.err.invalidChars.address_line4")))
    }

    "return an error if addressLine5 size too large" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "Address Line 3",
        trusteeAddressFields.addressLine4 -> "Address Line 4",
        trusteeAddressFields.addressLine5 -> randomString(19)
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.address_line5"))
    }

    "return an error if addressLine5 contains invalid chars" in {
      val postData = Json.obj(
        trusteeAddressFields.addressLine1 -> "Address Line 1",
        trusteeAddressFields.addressLine2 -> "Address Line 2",
        trusteeAddressFields.addressLine3 -> "Address Line 3",
        trusteeAddressFields.addressLine4 -> "Address Line 4",
        trusteeAddressFields.addressLine5 ->"??&&$$"
      )
      val validatedForm = trusteeAddressOverseasForm().bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == trusteeAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_trustee_details.err.invalidChars.address_line5"))
    }
  }
  def randomString(length: Int): String = scala.util.Random.alphanumeric.take(length).mkString
}
