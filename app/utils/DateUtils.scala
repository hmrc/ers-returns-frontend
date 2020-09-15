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


import java.text.SimpleDateFormat

import com.ibm.icu.util.ULocale
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.Logger
import play.api.i18n.Messages
import uk.gov.hmrc.time.DateTimeUtils

object DateUtils {

  def getCurrentDateTime: String = {
    val date: DateTime = DateTimeUtils.now
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val str: String = date.toString(fmt)
    str
  }

  def convertDate(date: String, format: String = "dd MMMM yyyy, hh:mma")(implicit messages: Messages): String = {
    Logger.debug("Converting date : " + date)
    val locale: ULocale = new ULocale(messages.lang.code)
    val dateOut = new com.ibm.icu.text.SimpleDateFormat("d MMMM yyyy, h:mma", locale)
    val dateFrm = new  SimpleDateFormat(format)
    val originalDate = dateFrm.parse(date)

    Logger.debug("The output is " + dateOut.format(originalDate))
    dateOut.format(originalDate)
  }

  def getFullTaxYear(taxYear: String)(implicit messages: Messages): String =
    s"${taxYear.take(4)} ${messages("ers.taxYear.text")} ${taxYear.take(2)}${taxYear.takeRight(2)}"
}
