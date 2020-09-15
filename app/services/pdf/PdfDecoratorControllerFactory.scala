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

import models.{AlterationAmends, ErsSummary}
import play.api.i18n.Messages
import utils.{CountryCodes, PageBuilder}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait PdfDecoratorControllerFactory extends PageBuilder {

	val countryCodes: CountryCodes

  def createPdfDecoratorControllerForScheme(scheme: String, ersSummary: ErsSummary, filesUploaded: Option[ListBuffer[String]])
																					 (implicit messages: Messages): DecoratorController = {

    scheme.trim.toLowerCase match {
      case "emi" => new DecoratorController(Array[Decorator]())
				.addYesNoDecorator("ers_choose.emi.question", ersSummary.isNilReturn)
				.addFileNamesDecorator(filesUploaded, ersSummary)
				.addSchemeOrganiserDetailsDecorator("emi", ersSummary, countryCodes)
				.addYesNoDecorator("ers_group_activity.emi.question", ersSummary.groupService.get.groupScheme.get)
				.addGroupSummaryDecorator("emi", ersSummary)

      case "csop" => new DecoratorController(Array[Decorator]())
				.addYesNoDecorator("ers_choose.csop.question", ersSummary.isNilReturn)
				.addFileNamesDecorator(filesUploaded, ersSummary)
				.addSchemeOrganiserDetailsDecorator("csop", ersSummary, countryCodes)
				.addYesNoDecorator("ers_group_activity.csop.question", ersSummary.groupService.get.groupScheme.get)
				.addGroupSummaryDecorator("csop", ersSummary)
				.addYesNoDecorator("ers_alt_activity.csop.question", ersSummary.altAmendsActivity.get.altActivity)
				.addAlterationsAmendsDecorator(createAltAmendOptionsFor(ersSummary, "csop"))

      case "sip" => new DecoratorController(Array[Decorator]())
				.addYesNoDecorator("ers_choose.sip.question", ersSummary.isNilReturn)
				.addFileNamesDecorator(filesUploaded, ersSummary)
				.addSchemeOrganiserDetailsDecorator("sip", ersSummary, countryCodes)
				.addYesNoDecorator("ers_group_activity.sip.question", ersSummary.groupService.get.groupScheme.get)
				.addGroupSummaryDecorator("sip", ersSummary)
				.addTrusteesDecorator(ersSummary.trustees)
				.addYesNoDecorator("ers_alt_activity.sip.question", ersSummary.altAmendsActivity.get.altActivity)
				.addAlterationsAmendsDecorator(createAltAmendOptionsFor(ersSummary, "sip"))

      case "saye" => new DecoratorController(Array[Decorator]())
				.addYesNoDecorator("ers_choose.saye.question", ersSummary.isNilReturn)
				.addFileNamesDecorator(filesUploaded, ersSummary)
				.addSchemeOrganiserDetailsDecorator("saye", ersSummary, countryCodes)
				.addYesNoDecorator("ers_group_activity.saye.question", ersSummary.groupService.get.groupScheme.get)
				.addYesNoDecorator("ers_alt_activity.saye.question", ersSummary.altAmendsActivity.get.altActivity)
				.addAlterationsAmendsDecorator(createAltAmendOptionsFor(ersSummary, "saye"))

      case "other" => new DecoratorController(Array[Decorator]())
				.addYesNoDecorator("ers_choose.other.question", ersSummary.isNilReturn)
				.addFileNamesDecorator(filesUploaded, ersSummary)
				.addSchemeOrganiserDetailsDecorator("other", ersSummary, countryCodes)
				.addYesNoDecorator("ers_group_activity.other.question", ersSummary.groupService.get.groupScheme.get)
				.addGroupSummaryDecorator("other", ersSummary)

      case _ => throw new IllegalArgumentException
    }
  }

	def convertAlterationsAmendsToMap(altAmends: AlterationAmends): Map[String, String] = {
		Map(
			"1" -> altAmends.altAmendsTerms.getOrElse(OPTION_NO),
			"2" -> altAmends.altAmendsEligibility.getOrElse(OPTION_NO),
			"3" -> altAmends.altAmendsExchange.getOrElse(OPTION_NO),
			"4" -> altAmends.altAmendsVariations.getOrElse(OPTION_NO),
			"5" -> altAmends.altAmendsOther.getOrElse(OPTION_NO)
		)
	}

	def createAltAmendOptionsFor(ersSummary: ErsSummary, variant: String)
															(implicit messages: Messages): Map[String, String] = {
		val map: mutable.HashMap[String, String] = mutable.HashMap()

		ersSummary.altAmendsActivity map { value =>
			if (value.altActivity == OPTION_YES) map += ("title" -> Messages("ers_trustee_summary.altamends.section"))

			ersSummary.alterationAmends map { altAmends =>
				convertAlterationsAmendsToMap(altAmends) map { altAmendsMap => val (index, answer ) = altAmendsMap

					if (answer == OPTION_YES) {
						map += (s"option$index" -> s"${Messages(s"ers_alt_amends.$variant.option_$index")}")
					}
				}
			}
		}
		map.toMap
	}
}
