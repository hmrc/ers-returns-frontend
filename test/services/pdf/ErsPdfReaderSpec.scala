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

package services.pdf

import org.apache.pdfbox.cos.COSDocument
import org.apache.pdfbox.io.RandomAccessFile
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logging

import java.io.File

class ErsPdfReaderSpec extends WordSpecLike with Matchers with OptionValues with MockitoSugar with BeforeAndAfterEach with Logging {
  var pdfStripper: PDFTextStripper = mock[PDFTextStripper]
  var pdDoc: PDDocument = mock[PDDocument]
  var cosDocCOSDocument: COSDocument = mock[COSDocument]
  var file: File = new File("test/resources/pdfFiles/confirmation.pdf")
  var parsedText: String = ""

  try {
    val parser: PDFParser = new PDFParser(new RandomAccessFile(file,"r"))
    parser.parse()
    val cosDoc = parser.getDocument()
    pdfStripper = new PDFTextStripper()
    pdDoc = new PDDocument(cosDoc)
    parsedText = pdfStripper.getText(pdDoc)
  } catch {
    case e: Exception =>
      logger.error(s"Error reading pdf file..",e)
      throw new Exception
  }

  "confirmation pdf file" should {
    "contain hmrc header" in {
      parsedText should include("HM Revenue & Customs")
      parsedText should include("Enterprise Management Incentives")
      parsedText should include("Confirmation receipt")
    }

    "contain Scheme name" in {
      parsedText should include ("Scheme name")
      parsedText should include ("EMI")
    }

    "contain scheme reference" in {
      parsedText should include ("Unique scheme reference")
      parsedText should include ("XA11000001231273")
    }

    "contain tax year" in {
      parsedText should include ("Tax Year")
      parsedText should include ("2015/16")
    }

    "contain data and time information" in {
      parsedText should include ("Date and time submitted")
      parsedText should include ("26 May 2016, 13:59 AM")
    }
  }
}
