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

import javax.imageio.ImageIO
import models.ErsSummary
import org.apache.pdfbox.pdmodel.common.PDMetadata
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.{PDFont, PDTrueTypeFont}
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.xmpbox.XMPMetadata
import org.apache.xmpbox.xml.{XmpSerializationException, XmpSerializer}
import play.api.Logger
import play.api.i18n.Messages
import utils.DateUtils

trait ErsContentsStreamer {
  def saveErsSummary() : ByteArrayOutputStream
  def savePageContent() : Boolean
  def drawText(string : String, fontSize : Float)(implicit messages: Messages) : Boolean
  def createNewPage()(implicit messages: Messages) : Boolean
  def drawLine() : Boolean
}

class ApachePdfContentsStreamer(ersSummary : ErsSummary) extends ErsContentsStreamer {
  Logger.debug("ers-returns-frontend using the apache pdf contents streamer")

  val LEFT_MRGIN = 40
  val RIGHT_MARGIN = 520

  lazy val document: Option[PDDocument] = Some(new PDDocument()) : Option[PDDocument]

  lazy val font: Option[PDFont] = try{
    Logger.debug("ers-returns-frontend about to load arialMt.ttf font")
    Some(PDTrueTypeFont.loadTTF(document.get, getClass.getResourceAsStream("/org/apache/pdfbox/resources/ttf/ArialMT.ttf")))
  }catch {
    case e: Exception =>
			Logger.error("can not load the font for the pdf")
			throw e
	}
  var contentStream : Option[PDPageContentStream] = None
  var cursorPositioner : Option[CursorPositioner] = None

  def saveErsSummary() : ByteArrayOutputStream = {
    Logger.debug("ers-returns-frontend: saving the pdf")
    val catalog = document.get.getDocumentCatalog
    val metadata = new PDMetadata(document.get)
    catalog.setMetadata(metadata)

    val xmp = XMPMetadata.createXMPMetadata()
    try {
      val pdfaid = xmp.createAndAddPFAIdentificationSchema()
      pdfaid.setConformance("B")
      pdfaid.setPart(1)
      pdfaid.setAboutAsSimple("HMRC PDFA Document")
      val serializer = new XmpSerializer
      val byteArrayOutputStream = new ByteArrayOutputStream()
      serializer.serialize(xmp, byteArrayOutputStream, true)
      metadata.importXMPMetadata(byteArrayOutputStream.toByteArray)
    } catch {
      case xmpException: XmpSerializationException =>
        Logger.debug("ers-returns-frontend can not create metadata")
        Logger.error(xmpException.getMessage)
    }

    val colourProfile = getClass.getResourceAsStream("/resources/sRGB-Color-Space-Profile.icm")
    val outputIntent = new PDOutputIntent(document.get, colourProfile)
    outputIntent.setInfo("sRGB IEC61966-2.1")
    outputIntent.setOutputCondition("sRGB IEC61966-2.1")
    outputIntent.setOutputConditionIdentifier("sRGB IEC61966-2.1")
    outputIntent.setRegistryName("http://www.color.org")
    catalog.addOutputIntent(outputIntent)

    val output = new ByteArrayOutputStream
    document.get.save(output)
    document.get.close()

     Logger.debug("ers-returns-frontend meta data is saved")
     output
  }

  def savePageContent() : Boolean = {
    if(contentStream.isDefined) {
     Logger.debug("ers-returns-frontend saving page content")
			contentStream.get.saveGraphicsState()
      contentStream.get.close()
      contentStream = None
      return true
    }
    false
  }

  def createNewPage()(implicit messages: Messages) : Boolean = {
     Logger.debug("ers-returns-frontend creating new pdf page")
     savePageContent()

    val page = new PDPage()
    document.get.addPage(page)
    contentStream = Some(new PDPageContentStream(document.get, page))

    cursorPositioner = Some(CursorPositionerFactory createCursorPositioner)
    addHMlogo(cursorPositioner.get)
  }

  def drawLine() : Boolean = {
    val nextPos = cursorPositioner.get.currentPos
    contentStream.get.drawLine(LEFT_MRGIN, nextPos._2, nextPos._1 + RIGHT_MARGIN, nextPos._2)

    true
  }

  def drawText(string: String, fontSize: Float)(implicit messages: Messages) : Boolean = {

    cursorPositioner.get.fontSize = fontSize
    val lines = getLines(string, font.get, fontSize)

    for (line <- lines) {
      try {
        cursorPositioner.get.advanceCursorToNextLine
      } catch {
        case _: IndexOutOfBoundsException =>
					createNewPage
			}
      val pos = cursorPositioner.get.currentPos
      drawText(line, pos._1, pos._2, fontSize)
    }

    true
  }


  private def drawText(stringToShow: String, x: Int, y: Int, fontSize: Float): (Int, Int) = {
    cursorPositioner.get.fontSize = fontSize

    addTextToPdf(stringToShow, x, y, fontSize)

    cursorPositioner.get.currentPos
  }

  private def addTextToPdf(string: String, x : Int, y : Int, fontSize : Float): Unit = {
    contentStream.get.beginText()
    contentStream.get.setFont(font.get, fontSize)
    contentStream.get.moveTextPositionByAmount(x, y)
    contentStream.get.drawString(string)
    contentStream.get.endText()
  }

  private def getLines(text: String, font: PDFont, fontSize: Float): List[String]= {

    var lines : List[String] = List[String]()

    var myLine : String = ""

    val words = text.split(" ")

    for(word <- words) {

      if(!myLine.isEmpty) {
        myLine += " "
      }

      val size = (fontSize * font.getStringWidth(myLine + word) / 1000).toInt

      if(size > 530) {
        lines = lines :+ myLine
        myLine = word
      } else {
        myLine += word
      }
    }
    lines = lines :+ myLine

    lines

  }

  private def addHMlogo(cursorPositioner: CursorPositioner)(implicit messages: Messages) : Boolean = {

     Logger.debug("ers-returns-frontend adding hm logo")
     cursorPositioner.beginHeader()

    try {
      val pathToCrownPng = getClass.getResource("/resources/crown.png")
      val crownImg = ImageIO.read(pathToCrownPng)
      val imagePos = cursorPositioner.getImageRelativeStart

      contentStream.get.drawImage(new PDJpeg(document.get, crownImg, 1.0f),
        imagePos._1,
        imagePos._2)
    } catch {
      case e:Throwable => Logger.error(s"Cannot draw logo with message ${e.getMessage}")
    }

    addPageHeaderText(cursorPositioner)

    cursorPositioner.endHeader()
    Logger.debug("ers-returns-frontend added the hm logo")

    true
  }

  private def addPageHeaderText(cursorPositioner: CursorPositioner)(implicit messages: Messages) {

    var pos = cursorPositioner.getHeaderRelativeStart
    drawText(Messages("ers.pdf.header"), pos._1, pos._2, 16)

    pos = cursorPositioner.getIndentedHeaderPos
    drawText(s"${ersSummary.metaData.schemeInfo.schemeType} - " +
			s"${ersSummary.metaData.schemeInfo.schemeRef} - " +
			s"${DateUtils.getFullTaxYear(ersSummary.metaData.schemeInfo.taxYear)}", pos._1, pos._2, 12)

    drawText("", 20)

    drawLine()
  }
}
