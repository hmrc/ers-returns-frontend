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

package services.pdf

import org.apache.pdfbox.cos.COSDocument
import org.apache.pdfbox.io.RandomAccessReadBufferedFile
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.ERSFakeApplicationConfig

import java.io.File

class ErsPdfReaderSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with MockitoSugar
    with ERSFakeApplicationConfig
    with GuiceOneAppPerSuite {

  var pdfStripper: PDFTextStripper   = mock[PDFTextStripper]
  var pdDoc: PDDocument              = mock[PDDocument]
  var cosDocCOSDocument: COSDocument = mock[COSDocument]
  var file: File                     = new File("test/resources/pdfFiles/confirmation.pdf")
  var parsedText: String             = ""

  try {
    val parser: PDFParser = new PDFParser(new RandomAccessReadBufferedFile(file))
    val cosDoc            = parser.parse()
    pdfStripper = new PDFTextStripper()
//    pdDoc = new PDDocument(cosDoc)
    pdDoc = cosDoc
    parsedText = pdfStripper.getText(pdDoc)
  } catch {
    case e: Exception => throw new Exception
  }

  "confirmation pdf file" should {
    "contain hmrc header" in {
      parsedText should include("HM Revenue & Customs")
      parsedText should include("Enterprise Management Incentives")
      parsedText should include("Submission receipt")
    }

    "contain Scheme name" in {
      parsedText should include("Scheme name")
      parsedText should include("EMI")
    }

    "contain scheme reference" in {
      parsedText should include("Unique scheme reference")
      parsedText should include("XA1100000000000")
    }

    "contain tax year" in {
      parsedText should include("Tax year")
      parsedText should include("2021/22")
    }

    "contain data and time information" in {
      parsedText should include("Time and date of submission")
      parsedText should include("12:08PM on Tue 25 January 2022")
    }
  }
}
