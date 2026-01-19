/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.internal

import play.api.Logging
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject

case class FileSizeUtils @Inject()(val auditEvents: AuditEvents) extends Logging {

  def logFileSize(schemeRef: String, fileSize: Int)(implicit hc: HeaderCarrier): Unit = {
    auditEvents.auditFileSize(schemeRef, fileSize.toString)
    logger.info(s"[FileSizeUtils][logFileSize]: schemeRef: ${schemeRef}, size: ${mapFileSizeToString(fileSize)}")
  }

  def mapFileSizeToString(fileSize: Int): String = {
    val numberBytesInUnit = 1024.0
    fileSize match {
      case size: Int if size < numberBytesInUnit =>
        f"$size%.2f bytes"
      case size: Int if (Math.pow(numberBytesInUnit, 2.0) > size && size >= numberBytesInUnit) =>
        val fileSizeInKb = size / numberBytesInUnit
        f"${fileSizeInKb}%.2f KB (${fileSize} bytes)"
      case size: Int if (Math.pow(numberBytesInUnit, 3.0) > size && size >= Math.pow(numberBytesInUnit, 2.0)) =>
        val fileSizeInMb = size / Math.pow(numberBytesInUnit, 2.0)
        f"${fileSizeInMb}%.2f MB (${fileSize} bytes)"
      case size: Int if size >= Math.pow(numberBytesInUnit, 3.0) =>
        val fileSizeInMb = size / Math.pow(numberBytesInUnit, 3.0)
        f"${fileSizeInMb}%.2f GB (${fileSize} bytes)"
    }
  }

}
