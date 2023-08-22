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

package services.pdf

import play.api.i18n.Messages

class AlterationsAmendsDecorator(map: Map[String, String]) extends Decorator {

  def decorate(implicit messages: Messages): String =
    if (map.nonEmpty) {
      val keys      = Array("option1", "option2", "option3", "option4", "option5")
      val subValues = map.filter(entry => keys.contains(entry._1)).values.toArray
      buildEntryMultiple(map("title"), subValues)
    } else {
      ""
    }
}
