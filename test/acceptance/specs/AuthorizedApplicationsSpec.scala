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

package acceptance.specs

import java.util.UUID

import acceptance.pages.AuthorizedApplicationsPage.{applicationNameLink, applicationScopeElement, applicationsMessageText, authorityGrantDateElement}
import acceptance.pages.{AuthorizedApplicationsPage, LoginPage}
import acceptance.stubs.{DelegatedAuthorityStub, LoginStub}
import acceptance.{BaseSpec, NavigationSugar}
import models.{AppAuthorisation, Scope, ThirdPartyApplication}
import org.joda.time.DateTime

class AuthorizedApplicationsSpec extends BaseSpec with NavigationSugar {

  feature("Viewing authorised applications for user") {

    scenario("User is redirected to the sign in page when not logged in") {

      go(AuthorizedApplicationsPage)
      redirectedTo(LoginPage)
    }

    scenario("User sees his authorized applications when logged in") {

      val applications = Seq(
        applicationAuthority(UUID.randomUUID(), "First Application",
          Set(Scope("read:api-1", "scope name", "Access personal information"),
              Scope("read:api-3", "scope name", "Access tax information")), DateTime.now),

        applicationAuthority(UUID.randomUUID(), "Second Application",
          Set(Scope("read:api-2", "scope name", "Access confidential information")), DateTime.now.minusDays(2)),

        applicationAuthority(UUID.randomUUID(), "Third Application",
          Set(), DateTime.now.minusMonths(2))
      )

      LoginStub.stubSuccessfulLogin()
      DelegatedAuthorityStub.stubSuccessfulFetchApplicationAuthorities(applications)

      go(AuthorizedApplicationsPage)
      on(AuthorizedApplicationsPage)

      verifyText(applicationsMessageText, "You have granted permission to the following software applications to access HMRC data. You can with withdraw this permission at any time.")
      applications.foreach(assertApplication)
    }

    scenario("User sees correct message if there are not authorized applications") {
      LoginStub.stubSuccessfulLogin()
      DelegatedAuthorityStub.stubSuccessfulFetchApplicationAuthorities(Seq.empty)

      go(AuthorizedApplicationsPage)
      on(AuthorizedApplicationsPage)

      verifyText(applicationsMessageText, "There are currently no applications which have access to HMRC data. If you want to grant permission to an application, you must do so in the application itself.")
    }
  }

  private def assertApplication(app: AppAuthorisation) = {
    verifyText(applicationNameLink(app.application.id), app.application.name)
    clickOnElement(applicationNameLink(app.application.id))
    app.scopes.foreach(scope => verifyText(applicationScopeElement(app.application.id, scope.key), scope.description))
    verifyText(authorityGrantDateElement(app.application.id), app.earliestGrantDate.toString("dd MMMM yyyy"))
  }

  private def applicationAuthority(appId: UUID, appName: String, scopes: Set[Scope], earliestGrantDate: DateTime) = {
    AppAuthorisation(ThirdPartyApplication(appId, appName), scopes, earliestGrantDate)
  }
}
