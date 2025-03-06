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

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import com.openhtmltopdf.util.XRLog
import models.ErsSummary
import org.apache.commons.io.IOUtils
import play.api.Logging
import play.api.i18n.Messages
import utils.{ContentUtil, CountryCodes, DateUtils, ERSUtil}

import java.io.{ByteArrayOutputStream, File}
import javax.inject.{Inject, Singleton}
import scala.io.Source

@Singleton
class ErsReceiptPdfBuilderService @Inject() (val countryCodes: CountryCodes)(implicit val ERSUtil: ERSUtil)
    extends PdfDecoratorControllerFactory
    with Logging {

  XRLog.listRegisteredLoggers.forEach((logger: String) => XRLog.setLevel(logger, java.util.logging.Level.WARNING))

  def createPdf(ersSummary: ErsSummary, filesUploaded: Option[List[String]], dateSubmitted: String)(implicit
    messages: Messages
  ): ByteArrayOutputStream = {

    implicit val decorator: DecoratorController =
      createPdfDecoratorControllerForScheme(ersSummary.metaData.schemeInfo.schemeType, ersSummary, filesUploaded)

    val startBoilerplate =
      s"""
         |<!DOCTYPE html>
         |<html lang="${messages.lang.code}">
         |<head>
         |<title>${messages("ers.pdf.filename")}</title>
         |<meta name="Lang" content="${messages.lang.code}"/>
         |<meta name="subject" content="${messages("ers.pdf.filename")}"/>
         |<meta name="about" content="HMRC PDFA Document"/>
         |<style>
         |  body {margin: 0; font-family: 'arial'; font-size: 16px;}
         |  header {margin: 0; font-family: 'arial'; font-size: 16px;}
         |  h1 {font-size: 36pt; letter-spacing: 0.04em;}
         |  h2 {font-size: 1em;}
         |</style>
         |</head>
         |<body>
         |$pdfHeader
         |""".stripMargin

    val endBoilerplate = "</body></html>"
    val html           = startBoilerplate + addMetaData(ersSummary, dateSubmitted) + addSummary() + endBoilerplate
    buildPdf(html)
  }

  def addMetaData(ersSummary: ErsSummary, dateSubmitted: String)(implicit messages: Messages): String =
    ERSUtil.replaceAmpersand(
      s"""
       |<h1 style="padding-top: 3em;">${messages("ers.pdf.title")}</h1>
       |
       |<p style="padding-bottom: 1em; font-size: 14pt;">${messages(
        "ers.pdf.confirmation.submitted",
        ContentUtil.getSchemeAbbreviation(ersSummary.metaData.schemeInfo.schemeType)
      )}</p>
       |<div style="display: block;">
       |
       |<h2 style="margin-bottom: 0em;">${messages("ers.pdf.scheme")}</h2>
       |<p style="margin-top: 0.3em; padding-left: 0.05em">${ersSummary.metaData.schemeInfo.schemeName}</p>
       |
       |<h2 style="margin-bottom: 0em;">${messages("ers.pdf.unique_scheme_ref")}</h2>
       |<p style="margin-top: 0.3em; padding-left: 0.05em">${ersSummary.metaData.schemeInfo.schemeRef}</p>
       |
       |<h2 style="margin-bottom: 0em;">${messages("ers.pdf.tax_year")}</h2>
       |<p style="margin-top: 0.3em; padding-left: 0.05em">${ersSummary.metaData.schemeInfo.taxYear}</p>
       |
       |<h2 style="margin-bottom: 0em;">${messages("ers.pdf.date_and_time")}</h2>
       |<p style="margin-top: 0.3em; padding-left: 0.05em">${DateUtils.convertDate(dateSubmitted)}</p>
       |
       |</div>
       |<footer>
       |<div style="text-align: center; padding-top: 19em;">
       |<hr/>
       |<br/>
       |<a style="color: black;">https://www.gov.uk/employment-related-securities-files</a>
       |</div>
       |</footer>
       |""".stripMargin
    )

  def pdfHeader(implicit messages: Messages): String = {
    val crownIcon   = Source.fromFile(getClass.getResource("/resources/crown.svg").toURI)
    val startHtml   = s"""<div style="padding-bottom: 0.3em; margin-top: -1em;">"""
    val endHtml     =
      s"""<p style="padding-left: 0.5em; display: inline-block; font-size: 16pt; vertical-align: middle;">${messages(
        "ers.pdf.header"
      )}</p>
            </div><hr/>"""
    val headingHtml = startHtml ++ crownIcon.getLines().mkString ++ endHtml
    crownIcon.close()
    headingHtml
  }

  def addSummary()(implicit decorator: DecoratorController, messages: Messages): String =
    decorator.decorate

  def buildPdf(html: String): ByteArrayOutputStream = {
    val os      = new ByteArrayOutputStream()
    val builder = new PdfRendererBuilder
    builder
      .useColorProfile(IOUtils.toByteArray(getClass.getResourceAsStream("/resources/sRGB-Color-Space-Profile.icm")))
      .useFont(new File(getClass.getResource("/resources/ArialMT.ttf").toURI), "arial")
      .usePdfUaAccessbility(true)
      .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_B)
      .withHtmlContent(html, "/")
      .useFastMode
      .useSVGDrawer(new BatikSVGDrawer())
      .toStream(os)
      .run()
    os
  }

}
