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

import controllers.routes
import models.CsvFiles
import play.api.i18n.Messages

trait PageBuilder {

  val DEFAULT = ""
  val DEFAULT_COUNTRY = "UK"

  // Schemes
  val CSOP: String = "CSOP"
  val EMI: String = "EMI"
  val SAYE: String = "SAYE"
  val SIP: String = "SIP"
  val OTHER: String = "OTHER"

  // Schemes
  val SCHEME_CSOP: String = "1"
  val SCHEME_EMI: String = "2"
  val SCHEME_SAYE: String = "4"
  val SCHEME_SIP: String = "5"
  val SCHEME_OTHER: String = "3"

  // Files
  val FILE_CSOP_GRANTED: String = "file0"
  val FILE_CSOP_RCL: String = "file1"
  val FILE_CSOP_Exercised: String = "file2"

  val FILE_EMI_ADJUSTMENTS: String = "file0"
  val FILE_EMI_REPLACED: String = "file1"
  val FILE_EMI_RCL: String = "file2"
  val FILE_EMI_NONTAXABLE: String = "file3"
  val FILE_EMI_TAXABLE: String = "file4"

  val FILE_SAYE_GRANTED: String = "file0"
  val FILE_SAYE_RCL: String = "file1"
  val FILE_SAYE_EXERCISED: String = "file2"

  val FILE_SIP_AWARDS: String = "file0"
  val FILE_SIP_OUT: String = "file1"

  val FILE_OTHER_GRANTS: String = "file0"
  val FILE_OTHER_OPTIONS: String = "file1"
  val FILE_OTHER_ACQUISITION: String = "file2"
  val FILE_OTHER_RESTRICTED: String = "file3"
  val FILE_OTHER_BENEFITS: String = "file4"
  val FILE_OTHER_CONVERTABLE: String = "file5"
  val FILE_OTHER_NOTIONAL: String = "file6"
  val FILE_OTHER_ENCHANCEMENT: String = "file7"
  val FILE_OTHER_SOLD: String = "file8"

  // pageId's
  val PAGE_START = "ers_start"
  val PAGE_CHOOSE = "ers_choose"
  val PAGE_CHECK_FILE = "ers_check_file"
  val PAGE_CHECK_CSV_FILE = "ers_check_csv_file"
  val PAGE_CONFIRMATION = "ers_confirmation"
  val PAGE_ERROR_REPORT = "ers_error_report"
  val PAGE_ALT_ACTIVITY = "ers_alt_activity"
  val PAGE_ALT_AMENDS = "ers_alt_amends"
  val PAGE_GROUP_ACTIVITY = "ers_group_activity"
  val PAGE_SUMMARY_DECLARATION = "ers_summary_declaration"
  val PAGE_GROUP_SCHEME_TYPE = "ers_group_scheme_type"
  val PAGE_GROUP_FILE_CHECK = "ers_group_file_check"
  val PAGE_GROUP_SUMMARY = "ers_group_summary"
  val PAGE_MANUAL_COMPANY_DETAILS = "ers_manual_company_details"
  val PAGE_SCHEME_ORGANISER = "ers_scheme_organiser"
  val PAGE_TRUSTEE_DETAILS = "ers_trustee_details"
  val PAGE_TRUSTEE_SUMMARY = "ers_trustee_summary"
  val PAGE_FILE_UPLOAD = "file_upload"
  val PAGE_SUCCESS = "ers.bulk.success"
  val PAGE_FILE_UPLOAD_ERRORS = "file_upload_errors"

  // Options
  val OPTION_YES = "1"
  val OPTION_NO = "2"
  val OPTION_UPLOAD_SPREEDSHEET = "1"
  val OPTION_NIL_RETURN = "2"
  val OPTION_ODS = "ods"
  val OPTION_CSV = "csv"
  val OPTION_MANUAL = "man"

  // message file entry prefix
  val MSG_ERS: String = "ers"
  val MSG_CSOP: String = ".csop."
  val MSG_EMI: String = ".emi."
  val MSG_SAYE: String = ".saye."
  val MSG_SIP: String = ".sip."
  val MSG_OTHER: String = ".other."

  val CSOP_CSV_FILES: Int = 3
  val EMI_CSV_FILES: Int = 5
  val SAYE_CSV_FILES: Int = 3
  val SIP_CSV_FILES: Int = 2
  val OTHER_CSV_FILES: Int = 9

  val AUDIT_ADD = "ADD"
  val AUDIT_EDIT = "EDIT"
  val AUDIT_DELETE = "DELETE"

  val CSVFilesList = Map(
    (
      EMI, List(
      CsvFiles(FILE_EMI_ADJUSTMENTS, None),
      CsvFiles(FILE_EMI_REPLACED, None),
      CsvFiles(FILE_EMI_RCL, None),
      CsvFiles(FILE_EMI_NONTAXABLE, None),
      CsvFiles(FILE_EMI_TAXABLE, None)
    )),
    (
      CSOP, List(
      CsvFiles(FILE_CSOP_GRANTED, None),
      CsvFiles(FILE_CSOP_RCL, None),
      CsvFiles(FILE_CSOP_Exercised, None)
    )),
    (
      OTHER, List(
      CsvFiles(FILE_OTHER_GRANTS, None),
      CsvFiles(FILE_OTHER_OPTIONS, None),
      CsvFiles(FILE_OTHER_ACQUISITION, None),
      CsvFiles(FILE_OTHER_RESTRICTED, None),
      CsvFiles(FILE_OTHER_BENEFITS, None),
      CsvFiles(FILE_OTHER_CONVERTABLE, None),
      CsvFiles(FILE_OTHER_NOTIONAL, None),
      CsvFiles(FILE_OTHER_ENCHANCEMENT, None),
      CsvFiles(FILE_OTHER_SOLD, None)
    )),
    (
      SAYE, List(
      CsvFiles(FILE_SAYE_GRANTED, None),
      CsvFiles(FILE_SAYE_RCL, None),
      CsvFiles(FILE_SAYE_EXERCISED, None)
    )),
    (
      SIP, List(
      CsvFiles(FILE_SIP_AWARDS, None),
      CsvFiles(FILE_SIP_OUT, None)
    ))
  )

  def getCsvFilesList(scheme: String): List[CsvFiles] = {
    CSVFilesList.getOrElse(scheme, List[CsvFiles]())
  }

  def getPageElement(scheme: String, pageId: String, element: String, para: String = "")(implicit messages: Messages): String = {
    scheme match {
      case SCHEME_CSOP => messages(pageId + MSG_CSOP + element, para)
      case SCHEME_EMI => messages(pageId + MSG_EMI + element, para)
      case SCHEME_SAYE => messages(pageId + MSG_SAYE + element, para)
      case SCHEME_SIP => messages(pageId + MSG_SIP + element, para)
      case SCHEME_OTHER => messages(pageId + MSG_OTHER + element, para)
      case _ => DEFAULT
    }
  }

  def getPageBackLink(schemeId: String, pageId: String, condition: String = "", reportableEvents: String = "2"): String = {
    val backLink: String = schemeId match {
      case SCHEME_CSOP =>
				pageId match {
					case PAGE_SCHEME_ORGANISER =>
						reportableEvents match {
							case OPTION_UPLOAD_SPREEDSHEET =>
								condition match {
									case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
									case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
								}
							case OPTION_NIL_RETURN => routes.ReportableEventsController.reportableEventsPage().toString
						}
					case PAGE_ALT_ACTIVITY =>
						condition match {
							case OPTION_YES => routes.GroupSchemeController.groupPlanSummaryPage().toString
							case OPTION_NO => routes.GroupSchemeController.groupSchemePage().toString
							case _ => DEFAULT
						}
					case PAGE_ALT_AMENDS => routes.AltAmendsController.altActivityPage().toString
					case PAGE_GROUP_SUMMARY =>
						routes.GroupSchemeController.manualCompanyDetailsPage().toString
					case PAGE_SUMMARY_DECLARATION =>
						condition match {
							case OPTION_YES => routes.AltAmendsController.altAmendsPage().toString
							case OPTION_NO => routes.AltAmendsController.altActivityPage().toString
							case _ => DEFAULT
						}
					case _ => DEFAULT
				}
			case SCHEME_EMI =>
				pageId match {
					case PAGE_SCHEME_ORGANISER =>
						reportableEvents match {
							case OPTION_UPLOAD_SPREEDSHEET =>
								condition match {
									case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
									case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
								}
							case OPTION_NIL_RETURN => routes.ReportableEventsController.reportableEventsPage().toString
						}
					case PAGE_GROUP_SUMMARY =>
						condition match {
							case OPTION_MANUAL => routes.GroupSchemeController.manualCompanyDetailsPage().toString
							case _ => DEFAULT
						}
					case PAGE_SUMMARY_DECLARATION =>
						condition match {
							case OPTION_YES => routes.GroupSchemeController.groupPlanSummaryPage().toString
							case OPTION_NO => routes.GroupSchemeController.groupSchemePage().toString
							case _ => DEFAULT
						}
					case _ => DEFAULT
				}
			case SCHEME_SAYE =>
				pageId match {
					case PAGE_SCHEME_ORGANISER =>
						reportableEvents match {
							case OPTION_UPLOAD_SPREEDSHEET =>
								condition match {
									case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
									case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
								}
							case OPTION_NIL_RETURN => routes.ReportableEventsController.reportableEventsPage().toString
						}
					case PAGE_ALT_ACTIVITY =>
						condition match {
							case OPTION_YES => routes.GroupSchemeController.groupPlanSummaryPage().toString
							case OPTION_NO => routes.GroupSchemeController.groupSchemePage().toString
							case _ => DEFAULT
						}
					case PAGE_ALT_AMENDS => routes.AltAmendsController.altActivityPage().toString
					case PAGE_GROUP_SUMMARY =>
						condition match {
							case OPTION_MANUAL => routes.GroupSchemeController.manualCompanyDetailsPage().toString
							case _ => DEFAULT
						}
					case PAGE_SUMMARY_DECLARATION =>
						condition match {
							case OPTION_YES => routes.AltAmendsController.altAmendsPage().toString
							case OPTION_NO => routes.GroupSchemeController.groupSchemePage().toString
							case _ => DEFAULT
						}
					case _ => DEFAULT
				}
			case SCHEME_SIP =>
				pageId match {
					case PAGE_SCHEME_ORGANISER =>
						reportableEvents match {
							case OPTION_UPLOAD_SPREEDSHEET =>
								condition match {
									case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
									case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
								}
							case OPTION_NIL_RETURN => routes.ReportableEventsController.reportableEventsPage().toString
						}
					case PAGE_ALT_ACTIVITY => routes.TrusteeController.trusteeSummaryPage().toString
					case PAGE_ALT_AMENDS => routes.AltAmendsController.altActivityPage().toString
					case PAGE_GROUP_SUMMARY =>
						routes.GroupSchemeController.manualCompanyDetailsPage().toString
					case PAGE_SUMMARY_DECLARATION =>
						condition match {
							case OPTION_YES => routes.AltAmendsController.altAmendsPage().toString
							case OPTION_NO => routes.AltAmendsController.altActivityPage().toString
							case _ => DEFAULT
						}
					case PAGE_TRUSTEE_DETAILS =>
						condition match {
							case OPTION_YES => routes.GroupSchemeController.groupPlanSummaryPage().toString
							case OPTION_NO => routes.GroupSchemeController.groupSchemePage().toString
							case _ => DEFAULT
						}
					case _ => DEFAULT
				}
			case SCHEME_OTHER =>
				pageId match {
					case PAGE_SCHEME_ORGANISER =>
						reportableEvents match {
							case OPTION_UPLOAD_SPREEDSHEET =>
								condition match {
									case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
									case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
								}
							case OPTION_NIL_RETURN => routes.ReportableEventsController.reportableEventsPage().toString
						}
					case PAGE_GROUP_SUMMARY =>
						condition match {
							case OPTION_MANUAL => routes.GroupSchemeController.manualCompanyDetailsPage().toString
							case _ => DEFAULT
						}
					case PAGE_SUMMARY_DECLARATION =>
						condition match {
							case OPTION_YES => routes.GroupSchemeController.groupPlanSummaryPage().toString
							case OPTION_NO => routes.GroupSchemeController.groupSchemePage().toString
							case _ => DEFAULT
						}
					case _ => DEFAULT
				}
			case _ => DEFAULT
    }
    backLink
  }

}
