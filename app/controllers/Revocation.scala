/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import java.util.UUID

import config.{FrontendAuthConnector, FrontendGlobal}
import connectors.AuthorityNotFound
import javax.inject.{Inject, Singleton}
import play.api.mvc.Action
import service.{RevocationService, TrustedAuthorityRetrievalException, TrustedAuthorityRevocationException}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

@Singleton
class Revocation @Inject()(override val authConnector: FrontendAuthConnector, val revocationService: RevocationService) extends FrontendController with Authentication {

  val start = Action.async { implicit request =>
    Future.successful(Ok(views.html.revocation.start()))
  }

  val loggedOut = Action.async { implicit request =>
    Future.successful(Ok(views.html.revocation.loggedOut()))
  }

  val listAuthorizedApplications = authenticated.async { implicit user => implicit request =>
    revocationService.fetchUntrustedApplicationAuthorities() map {
      applications => Ok(views.html.revocation.authorizedApplications(applications))
    }
  }

  def withdrawPage(id: UUID) = authenticated.async { implicit user => implicit request =>
    revocationService.fetchUntrustedApplicationAuthority(id) map {
      authority => Ok(views.html.revocation.withdrawPermission(authority))
    } recover {
      case _: AuthorityNotFound => NotFound(FrontendGlobal.notFoundTemplate)
      case _: TrustedAuthorityRetrievalException => NotFound(FrontendGlobal.notFoundTemplate)
    }
  }

  def withdrawAction(id: UUID) = authenticated.async { implicit user => implicit request =>
    revocationService.revokeApplicationAuthority(id) map {
      _ => Redirect(routes.Revocation.withdrawConfirmationPage())
    } recover {
      case _: AuthorityNotFound => NotFound(FrontendGlobal.notFoundTemplate)
      case _: TrustedAuthorityRevocationException => NotFound(FrontendGlobal.notFoundTemplate)
    }
  }

  val withdrawConfirmationPage = authenticated.async { implicit user => implicit request =>
    Future.successful(Ok(views.html.revocation.permissionWithdrawn()))
  }
}
