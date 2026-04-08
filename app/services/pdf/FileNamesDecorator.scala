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

package services.pdf

import play.api.i18n.Messages
import utils.PageBuilder

class FileNamesDecorator(reportableEvents: String, filesUploaded: Option[List[String]])
    extends Decorator with PageBuilder {

  def decorate(implicit messages: Messages): String =
    if (reportableEvents != OPTION_NIL_RETURN) {
      val heading = if (filesUploaded.get.length == 1) {
        messages("ers_summary_declaration.file_name")
      } else {
        messages("ers_summary_declaration.file_names")
      }
      buildEntryMultiple(heading, filesUploaded.get.toArray)
    } else {
      ""
    }

}
