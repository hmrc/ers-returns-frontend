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

package models

import models.RsFormMappings._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import utils.{ErsTestHelper, Fixtures}

import scala.concurrent.ExecutionContext

class RsFormMappingsSpec extends PlaySpec with ErsTestHelper with GuiceOneAppPerSuite {

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  "companyDetailsForm" must {
    "return no errors with valid data" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1"
      )
      val validateForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validateForm.errors.isEmpty)
    }
  }

  "companyName" must {
    "return an error if companyName missing" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> "",
        companyDetailsFields.addressLine1 -> "Address Line 1"
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.summary.company_name_required"))
    }

    "return an error if companyName size too large" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> randomString(121),
        companyDetailsFields.addressLine1 -> "Address Line 1"
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.company_name"))
    }

    "return an error if companyName contains invalid chars" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> "<script>rm *.*</script>",
        companyDetailsFields.addressLine1 -> "Address Line 1"
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.companyName)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.company_name"))
    }
  }

  "addressLine1" must {
    "return an error if addressLine1 missing" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> ""
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.summary.address_line1_required"))
    }

    "return an error if addressLine1 size too large" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> randomString(28)
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line1"))
    }

    "return an error if addressLine1 contains invalid chars" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine1)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line1"))
    }
  }

  "addressLine2" must {
    "return an error if addressLine2 size too large" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1",
        companyDetailsFields.addressLine2 -> randomString(28)
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line2"))
    }

    "return an error if addressLine2 contains invalid chars" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1",
        companyDetailsFields.addressLine2 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine2)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line2"))
    }
  }

  "addressLine3" must {
    "return an error if addressLine3 size too large" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1",
        companyDetailsFields.addressLine2 -> "Address Line 2",
        companyDetailsFields.addressLine3 -> randomString(28)
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line3"))
    }

    "return an error if addressLine3 contains invalid chars" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1",
        companyDetailsFields.addressLine2 -> "Address Line 2",
        companyDetailsFields.addressLine3 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine3)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.invalidChars.address_line3"))
    }
  }

  "addressLine4" must {
    "return an error if addressLine4 size too large" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1",
        companyDetailsFields.addressLine2 -> "Address Line 2",
        companyDetailsFields.addressLine3 -> "Address Line 3",
        companyDetailsFields.addressLine4 -> randomString(19)
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine4)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.address_line4"))
    }

    "return an error if addressLine4 contains invalid chars" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1",
        companyDetailsFields.addressLine2 -> "Address Line 2",
        companyDetailsFields.addressLine3 -> "Address Line 3",
        companyDetailsFields.addressLine4 -> "<script>rm *.*</script>"
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.addressLine4)
      val errors = validatedForm.errors.map(formError => formError.messages.head)
      assert(errors.contains(Messages("ers_manual_company_details.err.invalidChars.address_line4")))
      assert(errors.contains(Messages("ers_manual_company_details.err.address_line4")))
    }
  }

  "postCode" must {
    "return an error if postCode size too large" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1",
        companyDetailsFields.addressLine2 -> "Address Line 2",
        companyDetailsFields.addressLine3 -> "Address Line 3",
        companyDetailsFields.addressLine4 -> "Address Line 4",
        companyDetailsFields.postcode -> randomString(9)
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.postcode)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
    }

    "return an error if postCode contains invalid chars" in {
      val postData = Json.obj(
        companyDetailsFields.companyName -> Fixtures.companyName,
        companyDetailsFields.addressLine1 -> "Address Line 1",
        companyDetailsFields.addressLine2 -> "Address Line 2",
        companyDetailsFields.addressLine3 -> "Address Line 3",
        companyDetailsFields.addressLine4 -> "Address Line 4",
        companyDetailsFields.postcode -> "??&&$$"
      )
      val validatedForm = companyDetailsForm.bind(postData, Form.FromJsonMaxChars)
      assert(validatedForm.errors.head.key == companyDetailsFields.postcode)
      assert(validatedForm.errors.head.messages.head == Messages("ers_manual_company_details.err.postcode"))
    }
  }

  def randomString(length: Int): String = scala.util.Random.alphanumeric.take(length).mkString
}
