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

import java.io.ByteArrayOutputStream

import javax.inject.{Inject, Singleton}
import models.ErsSummary
import play.api.Logger
import play.api.i18n.Messages
import utils.{ContentUtil, CountryCodes, DateUtils}

import scala.collection.mutable.ListBuffer

@Singleton
class ErsReceiptPdfBuilderService @Inject()(val countryCodes: CountryCodes) extends PdfDecoratorControllerFactory {

  def createPdf(contentStreamer: ErsContentsStreamer, ersSummary: ErsSummary,
                filesUploaded: Option[ListBuffer[String]], dateSubmitted: String)(implicit messages: Messages): ByteArrayOutputStream = {
    implicit val streamer : ErsContentsStreamer = contentStreamer
    implicit val decorator: DecoratorController = createPdfDecoratorControllerForScheme(ersSummary.metaData.schemeInfo.schemeType, ersSummary, filesUploaded)

    addMetaData(ersSummary, dateSubmitted)
    addSummary()
    streamer.saveErsSummary()
  }

  def addMetaData(ersSummary : ErsSummary, dateSubmitted: String)
								 (implicit streamer: ErsContentsStreamer, decorator: DecoratorController, messages: Messages): Unit = {

    val headingFontSize = 16
    val answerFontSize = 12
    val lineSpacer = 12
    val blockSpacer = 36

    val ersMetaData = ersSummary.metaData

    streamer.createNewPage

    Logger.info("Adding metadata")
    streamer.drawText("", blockSpacer)

    val confirmationMessage = Messages("ers.pdf.confirmation.submitted",
      ContentUtil.getSchemeAbbreviation(ersMetaData.schemeInfo.schemeType), DateUtils.getFullTaxYear(ersSummary.metaData.schemeInfo.taxYear))

    streamer.drawText(confirmationMessage, headingFontSize)
    streamer.drawText("", lineSpacer)

    streamer.drawText("", blockSpacer)
    streamer.drawText(Messages("ers.pdf.scheme"), headingFontSize)
    streamer.drawText("", lineSpacer)
    streamer.drawText(ersMetaData.schemeInfo.schemeName, answerFontSize)

    streamer.drawText("", blockSpacer)
    streamer.drawText(Messages("ers.pdf.refcode"), headingFontSize)
    streamer.drawText("", lineSpacer)
    streamer.drawText(ersSummary.bundleRef, answerFontSize)

    Logger.info("Writing Date")
    val convertedDate = DateUtils.convertDate(dateSubmitted)

    streamer.drawText("", blockSpacer)
    streamer.drawText(Messages("ers.pdf.date_and_time"), headingFontSize)
    streamer.drawText("", lineSpacer)
    streamer.drawText(convertedDate, answerFontSize)
    Logger.info("Date Wrote:" + convertedDate)

    Logger.info("Save page content")
    streamer.savePageContent()
  }

  private def addSummary()(implicit streamer: ErsContentsStreamer, decorator: DecoratorController, messages: Messages): Unit = {
    val blockSpacer = 20

    Logger.info("Adding ERS Summary")

    streamer.createNewPage

    streamer.drawText(Messages("ers.pdf.summary_information"), 18)
    streamer.drawText("", blockSpacer)
    streamer.drawLine()
    streamer.drawText("", blockSpacer)

    decorator.decorate(streamer)

    Logger.info("Adding ERS Summary complete")

    streamer.savePageContent()
  }
}
