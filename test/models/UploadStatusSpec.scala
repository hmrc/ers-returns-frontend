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

import models.upscan._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsError, Json}

class UploadStatusSpec extends AnyWordSpecLike with Matchers with OptionValues {

  val statuses: List[UploadStatus] = List(NotStarted, Failed, InProgress)
  "UploadStats json Reads" should {
    statuses.foreach { status =>
      s"return $status" when {
        s"_type is $status" in {
          val json = s"""{"_type": "$status"}"""
          Json.parse(json).as[UploadStatus] shouldBe status
        }
      }
    }

    "return UploadedSuccessfully" when {
      "_type is UploadedSuccessfully" in {
        val expectedName = "fileName"
        val expectedUrl = "downloadUrl"
        val json = s"""{"_type": "UploadedSuccessfully", "name": "$expectedName", "downloadUrl": "$expectedUrl"}"""
        val expectedResponse = UploadedSuccessfully(expectedName, expectedUrl, None)
        Json.parse(json).as[UploadStatus] shouldBe expectedResponse
      }
    }

    "return JsonError" when {
      "_type is unexpected value" in {
        val unexpectedValue = "RandomValue"
        val json            = s"""{"_type": "$unexpectedValue"}"""
        Json.parse(json).validate[UploadStatus] shouldBe JsError(s"""Unexpected value of _type: "$unexpectedValue"""")
      }

      "_type is missing from JSON" in {
        val json = """{"type": "RandomValue"}"""
        Json.parse(json).validate[UploadStatus] shouldBe JsError("Missing _type field")
      }
    }
  }

  "UploadStatus writes" should {
    statuses.foreach { status =>
      s"set _type as $status" when {
        s"status is $status" in {
          val expectedJson = s"""{"_type":"$status"}"""
          Json.toJson(status).toString() shouldBe expectedJson
        }
      }
    }

    "set _type as UploadedSuccessfully with name, downloadUrl and noOfRows in json" when {
      "status is UploadedSuccessfully" in {
        val expectedName               = "fileName"
        val expectedUrl                = "downloadUrl"
        val noOfRows                   = 2
        val expectedJson               = Json.parse(
          s"""{"name":"$expectedName","downloadUrl":"$expectedUrl","_type":"UploadedSuccessfully","noOfRows":$noOfRows}"""
        )
        val uploadStatus: UploadStatus = UploadedSuccessfully(expectedName, expectedUrl, Some(noOfRows))
        assert(Json.toJson(uploadStatus) == expectedJson, "The upload status does not match its expected Json form")
      }
    }
  }

}
