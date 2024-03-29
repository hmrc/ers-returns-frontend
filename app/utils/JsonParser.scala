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

package utils

import play.api.libs.json.{JsArray, JsValue, Json}

trait JsonParser {
  def getSubmissionJson(schemeRef: String, schemeType: String, taxYear: String, submissionType: String): JsValue = {
    val jsonString: String = "{\"ERSSubmission\" : {" +
      "\"acknowledgementReference\": \"" + ContentUtil.getAcknowledgementRef + "\", " +
      "\"submissionTimestamp\" : \"" + DateUtils.getCurrentDateTime + "\", " +
      "\"schemeReference\" : \"" + schemeRef + "\", " +
      "\"taxYear\" : \"" + taxYear + "\", " +
      "\"schemeType\" : \"" + schemeType + "\", " +
      "\"submissionType\" : \"" + submissionType + "\"" +
      "}}"
    Json.parse(jsonString)
  }
}

object JsonUtils {
  implicit class RichJsArray(array: JsArray) {
    def applyOption(index: Int): Option[JsValue] = {
      if (index >= 0 && index < array.value.size) Some(array.value(index))
      else None
    }
  }
}