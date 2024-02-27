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
      val validatedForm = companyNameForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.summary.company_name_required"))
    }

    "return an error if companyName size too large" in {
      val postData = Json.obj(
        companyNameFields.companyName -> randomString(121)

      )
      val validatedForm = companyNameForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.company_name"))
    }

    "return an error if companyName contains invalid chars" in {
      val postData = Json.obj(
        companyNameFields.companyName -> "<script>rm *.*</script>"
      )
      val validatedForm = companyNameForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.company_name"))
    }

    "return an error if companyReg size too large" in {
      val postData = Json.obj(
        companyNameFields.companyName -> " company name",
        companyNameFields.companyReg -> randomString(121)

      )
      val validatedForm = companyNameForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyReg)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.company_reg"))
    }

    "return an error if companyReg contains invalid chars" in {
      val postData = Json.obj(
        companyNameFields.companyName -> " company name",
        companyNameFields.companyReg -> "<script>rm *.*</script>"
      )
      val validatedForm = companyNameForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyNameFields.companyReg)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.company_reg"))
    }

    "return an error if corporationRef contains invalid chars" in {
      val postData = Json.obj(
        companyNameFields.companyName -> " company name",
        companyNameFields.companyReg -> "12345678",
        companyNameFields.corporationRef -> "<script>rm *.*</script>"
      )
      val validatedForm = companyNameForm.bind(postData, Form.FromJsonMaxChars)
      println(validatedForm.errors)
      assert(validatedForm.errors.head.key == companyNameFields.corporationRef)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.corporation_ref"))
    }
  }

    "companyAddressUk" must {
    "return an error if addressLine1 missing" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> ""
      )
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.summary.address_line1_required"))
    }

    "return an error if addressLine1 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> randomString(28)
      )
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line1"))
    }

    "return an error if addressLine1 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line1"))
    }

    "return an error if addressLine2 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> randomString(28)
      )
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line2"))
    }

    "return an error if addressLine2 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line2"))
    }

    "return an error if addressLine3 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> randomString(28)
      )
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line3"))
    }

    "return an error if addressLine3 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
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
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
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
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
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
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
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
      val validatedForm = companyAddressUkForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
    }
  }

  "companyAddressOverseas" must {
    "return an error if addressLine1 missing" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> ""
      )
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.summary.address_line1_required"))
    }

    "return an error if addressLine1 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> randomString(28)
      )
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line1"))
    }

    "return an error if addressLine1 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line1"))
    }

    "return an error if addressLine2 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> randomString(28)
      )
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line2"))
    }

    "return an error if addressLine2 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line2"))
    }

    "return an error if addressLine3 size too large" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> randomString(28)
      )
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line3"))
    }

    "return an error if addressLine3 contains invalid chars" in {
      val postData = Json.obj(
        companyAddressFields.addressLine1 -> "Address Line 1",
        companyAddressFields.addressLine2 -> "Address Line 2",
        companyAddressFields.addressLine3 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
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
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
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
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
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
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
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
      val validatedForm = companyAddressOverseasForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyAddressFields.addressLine5)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line5"))
    }
  }

  def randomString(length: Int): String = scala.util.Random.alphanumeric.take(length).mkString
}
