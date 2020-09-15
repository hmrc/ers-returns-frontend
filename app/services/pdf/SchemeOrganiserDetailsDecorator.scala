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

import models.SchemeOrganiserDetails
import play.api.i18n.Messages
import utils.CountryCodes
import utils.DecoratorConstants._

class SchemeOrganiserDetailsDecorator(title: String,
																			schemeOrganiser: SchemeOrganiserDetails,
																			countryCodes: CountryCodes,
																			headingFontSize: Float = headingFontSizeDefault,
																			answerFontSize: Float = answerFontSizeDefault,
																			lineSpacer: Float = lineSpacerDefault,
																			blockSpacer: Float = blockSpacerDefault
																			) extends Decorator {

  def decorate(streamer: ErsContentsStreamer)(implicit messages: Messages): Unit = {
    streamer.drawText(title, headingFontSize)
    streamer.drawText("", blockSpacer)
    streamer.drawText(schemeOrganiser.companyName, answerFontSize)
    streamer.drawText("", lineSpacer)

    streamer.drawText(schemeOrganiser.addressLine1, answerFontSize)
    streamer.drawText("", lineSpacer)

    if (schemeOrganiser.addressLine2.isDefined) {
      streamer.drawText(schemeOrganiser.addressLine2.getOrElse(""), answerFontSize)
      streamer.drawText("", lineSpacer)
    }
    if (schemeOrganiser.addressLine3.isDefined) {
      streamer.drawText(schemeOrganiser.addressLine3.getOrElse(""), answerFontSize)
      streamer.drawText("", lineSpacer)
    }
    if (schemeOrganiser.addressLine4.isDefined) {
      streamer.drawText(schemeOrganiser.addressLine4.getOrElse(""), answerFontSize)
      streamer.drawText("", lineSpacer)
    }
    if (schemeOrganiser.country.isDefined) {
      streamer.drawText(countryCodes.getCountry(schemeOrganiser.country.getOrElse("")).get, answerFontSize)
      streamer.drawText("", lineSpacer)
    }
    if (schemeOrganiser.postcode.isDefined) {
      streamer.drawText(schemeOrganiser.postcode.getOrElse(""), answerFontSize)
      streamer.drawText("", lineSpacer)
    }
    if (schemeOrganiser.companyReg.isDefined) {
      streamer.drawText(schemeOrganiser.companyReg.getOrElse(""), answerFontSize)
      streamer.drawText("", lineSpacer)
    }
    if (schemeOrganiser.corporationRef.isDefined) {
      streamer.drawText(schemeOrganiser.corporationRef.getOrElse(""), answerFontSize)
      streamer.drawText("", lineSpacer)
    }

    streamer.drawText("", blockSpacer)
    streamer.drawLine()
    streamer.drawText("", blockSpacer)

  }
}
