/*
 * Copyright 2016 HM Revenue & Customs
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

package unit.connectors

import java.util.UUID

import acceptance.stubs.DelegatedAuthorityStub
import com.github.tomakehurst.wiremock.client.WireMock._
import config.WSHttp
import connectors.DelegatedAuthorityConnector
import models.{AppAuthorisation, Scope, ThirdPartyApplication}
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class DelegatedAuthorityConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new DelegatedAuthorityConnector {
      override val delegatedAuthorityUrl = wireMockUrl
      override val http = WSHttp
    }
  }

  "fetchApplicationAuthorities" should {

    "retrieve all third party delegated authorities granted by a user" in new Setup {

      val authorities = Seq(anApplicationAuthority(), anApplicationAuthority())

      DelegatedAuthorityStub.stubSuccessfulFetchApplicationAuthorities(authorities)

      await(connector.fetchApplicationAuthorities()) shouldBe authorities
    }

    "return an empty set if there are no authorised applications" in new Setup {
      stubFor(get(urlEqualTo(s"/authority/granted-applications")).willReturn(
        aResponse().withStatus(200).withBody("[]")))

      await(connector.fetchApplicationAuthorities()) shouldBe Seq()
    }
  }

  "fetchApplicationAuthority" should {

    "retrieve a single third party delegated authority granted by a user to the given application" in new Setup {

      val authority = anApplicationAuthority()

      DelegatedAuthorityStub.stubSuccessfulFetchApplicationAuthority(authority)

      await(connector.fetchApplicationAuthority(authority.application.id)) shouldBe authority
    }
  }

  private def anApplicationAuthority() = {
    AppAuthorisation(
      application = ThirdPartyApplication(UUID.randomUUID(), "My App", trusted = true),
      scopes = Set(Scope("read:api-name", "Access personal info", "Access personal info")),
      earliestGrantDate = new DateTime(1460713641258L)
    )
  }
}
