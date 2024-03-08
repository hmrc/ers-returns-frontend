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

import models.{ErsSummary, TrusteeDetailsList}
import play.api.i18n.Messages
import utils.{CountryCodes, ERSUtil}

import javax.inject.Inject

class DecoratorController @Inject()(val decorators: Array[Decorator])(implicit ERSUtil: ERSUtil) {

  def addDecorator(decorator: Decorator): DecoratorController = new DecoratorController(decorators :+ decorator)

  def decorate(implicit messages: Messages): String =
    ERSUtil.replaceAmpersand(
      decorators.map(decorator => decorator.decorate).mkString
    )

  def addFileNamesDecorator(filesUploaded: Option[List[String]], ersSummary: ErsSummary): DecoratorController =
    addDecorator(new FileNamesDecorator(ersSummary.isNilReturn, filesUploaded))

  def addTrusteesDecorator(trusteesList: Option[TrusteeDetailsList]): DecoratorController =
    addDecorator(new TrusteesDecorator(trusteesList))

  def addAlterationsAmendsDecorator(altAmendsMap: Map[String, String]): DecoratorController =
    addDecorator(new AlterationsAmendsDecorator(altAmendsMap))

  def addYesNoDecorator(msgKey: String, ersSummaryValue: String)(implicit messages: Messages): DecoratorController =
    addDecorator(new YesNoDecorator(Messages(msgKey), ersSummaryValue))

  def addGroupSummaryDecorator(key: String, ersSummary: ErsSummary)(implicit messages: Messages): DecoratorController =
    addDecorator(new GroupSummaryDecorator(Messages(s"ers_group_summary.$key.title"), ersSummary.companies))

  def addSchemeOrganiserDetailsDecorator(key: String, ersSummary: ErsSummary, countryCodes: CountryCodes)(implicit
    messages: Messages
  ): DecoratorController =
    addDecorator(
      new SchemeOrganiserDetailsDecorator(
        Messages(s"ers_summary_declaration.$key.organiser"),
        ersSummary.schemeOrganiser.get,
        countryCodes
      )
    )

}
