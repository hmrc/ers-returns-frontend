
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._


object TestPhases {

  val allPhases = "tt->test;test->test;test->compile;compile->compile"
  val allItPhases = "tit->it;it->it;it->compile;compile->compile"

  lazy val TemplateTest = config("tt") extend Test
  lazy val TemplateItTest = config("tit") extend IntegrationTest

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => Group(test.name, Seq(test), SubProcess(config = ForkOptions().withRunJVMOptions(Vector("-Dtest.name=" + test.name))))

    }


}
