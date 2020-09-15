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

import models.CompanyDetailsList
import play.api.i18n.Messages
import utils.DecoratorConstants._

class GroupSummaryDecorator(headingTitle: String,
														companiesList: Option[CompanyDetailsList],
														headingFontSize: Float = headingFontSizeDefault,
														answerFontSize: Float = answerFontSizeDefault,
														lineSpacer: Float = lineSpacerDefault,
														blockSpacer: Float = blockSpacerDefault
													 ) extends Decorator {

  def decorate(streamer: ErsContentsStreamer)(implicit messages: Messages): Unit = {
    if(companiesList.isDefined) {
			streamer.drawText("", lineSpacer)
			streamer.drawText(headingTitle, headingFontSize)
			streamer.drawText("", lineSpacer)

			for (company <- companiesList.get.companies) {
				streamer.drawText(company.companyName, answerFontSize)
				streamer.drawText("", lineSpacer)
			}

			streamer.drawText("", blockSpacer)
			streamer.drawLine()
			streamer.drawText("", blockSpacer)
		}
  }
}
