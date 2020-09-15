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

package services.pdf

import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class CursorPositionerSpec extends UnitSpec with MockitoSugar{
  var cursorPositioner = new CursorPositioner
  cursorPositioner.fontSize = 1

  "header block start" should {
    "set start header to top of the page" in {
      cursorPositioner.beginHeader()
      cursorPositioner.currentPos should be ((40, 750))
    }
  }


  "cursor positioner" should {

    "get indented header relative position" in {
      cursorPositioner.getIndentedHeaderPos should be ((45, 720))
    }

    "provide the header Image relative position in the page" in {
      cursorPositioner.getImageRelativeStart should be ((45, 680))
    }

    "get header relative start position" in {
      cursorPositioner.getHeaderRelativeStart should be ((95, 700))
    }

    "report that the next line cursor position" in {
      cursorPositioner = new CursorPositioner
      cursorPositioner.fontSize = 1

      val pos = cursorPositioner.advanceCursorToNextLine

      pos should be ((40, 749))
    }

    "be decremented by a given font size" in {
      val cursorPositioner = new CursorPositioner
      cursorPositioner.fontSize = 2

      cursorPositioner.advanceCursorToNextLine should be ((40, 748))
      cursorPositioner.advanceCursorToNextLine should be ((40, 746))
    }

    "given page dimensions determine if a new page is required and throw exception " in {
      val cursorPositioner = new CursorPositioner
      cursorPositioner.fontSize = 800

      intercept[IndexOutOfBoundsException] {
        cursorPositioner.advanceCursorToNextLine
      }
    }
 }

  "header block finish" should {
    "set cursor position after the block header" in {
      cursorPositioner.endHeader()
      cursorPositioner.currentPos should be ((40, 650))
    }

    "set carret position after the block header" in {
      cursorPositioner.endHeader
      cursorPositioner.advanceCursorToNextLine should be ((40, 649))
    }
  }
}
