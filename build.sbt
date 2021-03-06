import play.core.PlayVersion
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.PlayImport._
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.{SbtAutoBuildPlugin, _}

lazy val appName = "api-revocation-frontend"

lazy val appDependencies: Seq[ModuleID] = compile ++ test

lazy val frontendBootstrapVersion = "8.22.0"
lazy val govukTemplateVersion = "5.3.0"
lazy val playPartialsVersion = "6.1.0"
lazy val hmrcTestVersion = "2.4.0"

lazy val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
  "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
  "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
  "org.apache.httpcomponents" % "httpclient" % "4.3.3",
  "org.apache.httpcomponents" % "httpcore" % "4.3.3"
)

lazy val test = Seq(
  "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test",
  "org.jsoup" % "jsoup" % "1.7.3" % "test",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "test",
  "com.github.tomakehurst" % "wiremock" % "2.11.0" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.0" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val microservice = (project in file("."))
  .enablePlugins(Seq(_root_.play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin) ++ plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    name := appName,
    targetJvm := "jvm-1.8",
    scalaVersion := "2.11.11",
    libraryDependencies ++= appDependencies,
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := StaticRoutesGenerator
  )
  .settings(testOptions in Test := Seq(Tests.Filter(unitFilter)),
    addTestReportOption(Test, "test-reports")
  )
  .configs(AcceptanceTest)
  .settings(inConfig(AcceptanceTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in AcceptanceTest := Seq(Tests.Filter(acceptanceFilter)),
    unmanagedSourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest) (base => Seq(base / "test")),
    addTestReportOption(AcceptanceTest, "int-test-reports"),
    testGrouping in AcceptanceTest := oneForkedJvmPerTest((definedTests in AcceptanceTest).value)
  )
  .settings(resolvers ++= Seq(Resolver.bintrayRepo("hmrc", "releases"), Resolver.jcenterRepo))

lazy val AcceptanceTest = config("acceptance") extend Test

def unitFilter(name: String): Boolean = name startsWith "unit"

def acceptanceFilter(name: String): Boolean = name startsWith "acceptance"

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

// Coverage configuration
coverageMinimum := 90
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;uk.gov.hmrc.BuildInfo"
