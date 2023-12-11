/*
 * Copyright 2023 HM Revenue & Customs
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

import com.ibm.icu.util.ULocale
import play.api.Logging
import play.api.i18n.Messages

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils extends Logging {

  def getCurrentDateTime: String = {
    val instant: Instant             = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    formatter.format(instant)
  }

  def convertDate(date: String, format: String = "dd MMMM yyyy, hh:mma")(implicit messages: Messages): String = {
    val locale: ULocale = new ULocale(messages.lang.code)
    val timeOut         = new com.ibm.icu.text.SimpleDateFormat("h:mma", locale)
    val dateOut         = new com.ibm.icu.text.SimpleDateFormat("E d MMMM yyyy", locale)
    val dateFrm         = new SimpleDateFormat(format)
    val originalDate    = dateFrm.parse(date)
    val on              = messages("ers-confirmation.submission_on")

    timeOut.format(originalDate) + s" $on " + dateOut.format(originalDate)
  }

  def getFullTaxYear(taxYear: String)(implicit messages: Messages): String =
    s"${taxYear.take(4)} ${messages("ers.taxYear.text")} ${taxYear.take(2)}${taxYear.takeRight(2)}"
}
