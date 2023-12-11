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

package forms

import org.scalatestplus.play.PlaySpec
import play.api.data.FormError

class YesNoFormProviderTest extends PlaySpec {

  val testFormProvider = new YesNoFormProvider

  "stringFormatter" must {
    val errorKey = "error.required"
    val formatter = testFormProvider.stringFormatter(errorKey)

    "successfully bind non-empty strings" in {
      val key = "testKey"
      val value = "testValue"
      val data = Map(key -> value)

      formatter.bind(key, data) mustBe Right(value.trim)
    }

    "return an error for empty strings" in {
      val key = "testKey"
      val data = Map(key -> "")

      formatter.bind(key, data) mustBe Left(Seq(FormError(key, errorKey)))
    }

    "return an error when key is not present" in {
      val key = "testKey"
      val data = Map.empty[String, String]

      formatter.bind(key, data) mustBe Left(Seq(FormError(key, errorKey)))
    }

    "unbind a string to a key-value pair" in {
      val key = "testKey"
      val value = "testValue"

      formatter.unbind(key, value) mustBe Map(key -> value)
    }
  }

  "booleanFormatter" must {
    val requiredKey = "error.required"
    val invalidKey = "error.invalid"
    val formatter = testFormProvider.booleanFormatter(requiredKey, invalidKey)

    "successfully bind 'true' to true" in {
      val key = "testKey"
      val data = Map(key -> "true")

      formatter.bind(key, data) mustBe Right(true)
    }

    "successfully bind 'false' to false" in {
      val key = "testKey"
      val data = Map(key -> "false")

      formatter.bind(key, data) mustBe Right(false)
    }

    "return an error for empty strings" in {
      val key = "testKey"
      val data = Map(key -> "")

      formatter.bind(key, data) mustBe Left(Seq(FormError(key, requiredKey)))
    }

    "return an error for invalid boolean values" in {
      val key = "testKey"
      val data = Map(key -> "maybe")

      formatter.bind(key, data) mustBe Left(Seq(FormError(key, invalidKey)))
    }

    "return an error when key is not present" in {
      val key = "testKey"
      val data = Map.empty[String, String]

      formatter.bind(key, data) mustBe Left(Seq(FormError(key, requiredKey)))
    }

    "unbind a boolean to a key-value pair" in {
      val key = "testKey"
      val value = true

      formatter.unbind(key, value) mustBe Map(key -> "true")
    }
  }
}
