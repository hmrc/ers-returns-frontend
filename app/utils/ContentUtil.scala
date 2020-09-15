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

package utils

import play.api.i18n.Messages

object ContentUtil extends ContentUtil

trait ContentUtil {

  def getSchemeName(schemeType: String)(implicit messages: Messages): String = {
    schemeType match {
      case "1" => Messages("ers_pdf_error_report.csop")
      case "2" => Messages("ers_pdf_error_report.emi")
      case "4" => Messages("ers_pdf_error_report.saye")
      case "5" => Messages("ers_pdf_error_report.sip")
      case "3" => Messages("ers_pdf_error_report.other")
      case _ => ""
    }
  }

  def getSchemeAbbreviation(schemeType: String)(implicit messages: Messages): String = {
    schemeType.toLowerCase match {
      case "1"|"csop" => Messages("ers.csop")
      case "2"|"emi" => Messages("ers.emi")
      case "4"|"saye" => Messages("ers.saye")
      case "5"|"sip" => Messages("ers.sip")
      case "3"|"other" => Messages("ers.other")
      case _ => ""
    }
  }

  def getAcknowledgementRef: String = {
    System.currentTimeMillis().toString
  }
}
