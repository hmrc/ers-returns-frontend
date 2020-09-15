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

import play.api.i18n.Messages
import utils.{DecoratorConstants, PageBuilder}
import DecoratorConstants._

class YesNoDecorator(headingTitle: String,
										 isNilReturn: String,
										 headingFontSize: Float = headingFontSizeDefault,
										 answerFontSize: Float = answerFontSizeDefault,
										 lineSpacer: Float = lineSpacerDefault,
										 blockSpacer: Float = blockSpacerDefault
										) extends Decorator with PageBuilder {

  def decorate(streamer: ErsContentsStreamer)(implicit messages: Messages): Unit = {

    streamer.drawText(headingTitle, headingFontSize)
    streamer.drawText("", lineSpacer)

    streamer.drawText(isNilReturn match {
      case OPTION_YES => Messages("ers.yes")
      case OPTION_NO => Messages("ers.no")
    }, answerFontSize)

    streamer.drawText("", blockSpacer)
    streamer.drawLine()
    streamer.drawText("", blockSpacer)
  }
}
