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

package utils

import config.ApplicationConfig

sealed trait TemplateSet {

  def selectTemplatesSet(taxYear: Option[String])(implicit appConfig: ApplicationConfig): String = {
    val isAfter2023 = taxYear.exists(_.split("/")(0).toInt >= 2023)

    (isAfter2023, appConfig.enableV6AndV7) match {
      case (true, true)   => V7.toString
      case (true, false)  => V5.toString
      case (false, true)  => V6.toString
      case (false, false) => V4.toString
    }
  }

}

case object V4 extends TemplateSet {
  override def toString: String = "v4"
}

case object V5 extends TemplateSet {
  override def toString: String = "v5"
}

case object V6 extends TemplateSet {
  override def toString: String = "v6"
}

case object V7 extends TemplateSet {
  override def toString: String = "v7"
}
