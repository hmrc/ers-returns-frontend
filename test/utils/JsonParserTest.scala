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

package utils

import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import controllers._

class JsonParserTest extends UnitSpec with ERSFakeApplicationConfig with MockitoSugar {
	class TestJsonParser extends JsonParser
	val testJsonParser = new TestJsonParser

	"getSubmissionJson" should {
		"return valid json for a summary submission to ETMP" in {
			val schemeRef = "AA0000000000000"
			val schemeType = "CSOP"
			val taxYear = "2014/15"
			val submissionType = "EOY-RETURN"
			val result = testJsonParser.getSubmissionJson(schemeRef, schemeType, taxYear, submissionType)
			val schemeReference = (result \ "ERSSubmission" \ "schemeReference").as[String]
			schemeReference shouldBe "AA0000000000000"
		}
	}

}
