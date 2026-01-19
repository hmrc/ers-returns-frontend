/*
 * Copyright 2026 HM Revenue & Customs
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

import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.ERSFakeApplicationConfig

import java.io.FileInputStream

class ErsPdfReaderSpec
  extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with MockitoSugar
    with ERSFakeApplicationConfig
    with GuiceOneAppPerSuite {

  def readPDF(path: String): String = {
    val file = new FileInputStream(path)
    val readBuffer = new RandomAccessReadBuffer(file)
    val parser = new PDFParser(readBuffer)
    val cosDoc = parser.parse().getDocument
    val pdDoc = new PDDocument(cosDoc)
    try {
      val strip = new PDFTextStripper
      strip.getText(pdDoc)
    } catch {
      case e: Exception => throw new Exception
    }finally {
      pdDoc.close()
      file.close()
    }
  }

  "confirmation pdf file" should {
    val parsedText = readPDF("test/resources/pdfFiles/confirmation.pdf")
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
