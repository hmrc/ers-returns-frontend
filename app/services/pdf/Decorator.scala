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

trait Decorator {

  def decorate(implicit messages: Messages): String

  def buildEntry(title: String, content: String): String =
    s"""<div style="display: block;">
      <h2 style="margin-bottom: 0em;">$title</h2>
      <p style="margin-top: 0.3em; padding-left: 0.05em">$content</p>
      <hr/>
      </div>
      """

  def buildEntryMultiple(title: String, content: Array[String]): String = {
    val start    = s"""
                <div style="display: block;">
                <h2 style="margin-bottom: 0em;">$title</h2>
      """
    val elements = content.map(item => s"""<p style="margin-top: 0.3em; padding-left: 0.05em">$item</p>""").mkString
    start + elements + "<hr/></div>"
  }

}
