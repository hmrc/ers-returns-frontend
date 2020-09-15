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

import models.ERSAuthData
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.Future

trait AuthHelper extends MockitoSugar {

	type RetrievalType = Enrolments ~ Option[AffinityGroup]
	lazy val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
	val agentOnlyEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", "JARN1234567")), "activated"))
	val invalidEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-TEST-ORG", Seq(EnrolmentIdentifier("TestRefNumber", "XN1200000100001")), "activated"))
	val ersEnrolmentSet: Set[Enrolment] = Set(Enrolment("IR-PAYE", Seq(EnrolmentIdentifier("TaxOfficeNumber", "123"), EnrolmentIdentifier("TaxOfficeReference", "4567890")), "activated"))

	def buildERSAuthData(enrolmentSet: Set[Enrolment],
											 affGroup: Option[AffinityGroup] = None,
											 testEmpRef: EmpRef = EmpRef("", "")
											): ERSAuthData =  {
		ERSAuthData(enrolments = enrolmentSet, affinityGroup = affGroup, empRef = testEmpRef)
	}

	def buildRetrieval(ersAuthData: ERSAuthData): RetrievalType = {
		new ~(
			Enrolments(ersAuthData.enrolments),
			ersAuthData.affinityGroup
		)
	}

	val defaultErsAuthData: ERSAuthData = buildERSAuthData(ersEnrolmentSet, Some(Organisation), EmpRef("123", "ABCDE"))

	def setAuthMocks(): OngoingStubbing[Future[RetrievalType]] = {
		when(mockAuthConnector
			.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
			.thenReturn(Future.successful(buildRetrieval(defaultErsAuthData)))
	}

	def setUnauthorisedMocks(): OngoingStubbing[Future[RetrievalType]] = {
		when(mockAuthConnector
			.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
			.thenReturn(Future.failed(MissingBearerToken("No authenticated bearer token")))
	}
}
