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

trait CursorPositionerFactory {
  def createCursorPositioner : CursorPositioner
}

object CursorPositionerFactory extends CursorPositionerFactory {
  def createCursorPositioner : CursorPositioner = {
    new CursorPositioner
  }
}

class CursorPositioner() {
  val pageLimit = 40
  var currentPos: (Int, Int) = (40, 750)
  var fontSize = 12.0f

  def beginHeader(): Unit = {
    currentPos = (40, 750)
  }

  def endHeader(): Unit = {
    currentPos = (40, 650)
  }

  // functions that are used to draw the header block
  def getIndentedHeaderPos: (Int, Int) = {
    currentPos = (45, currentPos._2 - 30)
    currentPos
  }

  def getHeaderRelativeStart: (Int, Int) = {
    currentPos = (currentPos._1 + 50, currentPos._2 + 20)
    currentPos
  }

  def getImageRelativeStart: (Int, Int) = {
    currentPos = (currentPos._1, currentPos._2 - 40)
    currentPos
  }

  // functions to start positioning cursor for text block
  def advanceCursorToNextLine : (Int, Int) ={

    if(pageLimitReached){
      throw new IndexOutOfBoundsException
    } else {
      currentPos = (40, currentPos._2 - fontSize.toInt)
    }

    currentPos
  }

  def pageLimitReached : Boolean = {
    (currentPos._2 - fontSize.toInt) <= pageLimit
  }
}
