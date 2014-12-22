package nfn

import ccn.packet._
import config.StaticConfig
import lambdacalculus.parser.ast.Expr
import node.LocalNode
import org.scalatest.time.{Millis, Span}
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ExpressionTester extends FlatSpec
                       with ScalaFutures
                       with Matchers {
  def testExpr(expr: Expr, descr: String, test: String => Unit, useThunks:Boolean = false)(implicit node: LocalNode) = {
    s"expr: $expr" should s"$descr" in {
      doExp(expr, test)
    }
  }

  def testExprExpected(expr: Expr, expected: String, useThunks: Boolean = false)(implicit node: LocalNode) = {
    testExpr(expr,
             s"result in $expected",
             testExpected(expected)
    )
  }

  def testExpected(expected: String) = (res: String) => res shouldBe expected

  def doExp(exprToDo: Expr, test: String => Unit, useThunks: Boolean = false)(implicit node: LocalNode) = {
    implicit val us = useThunks
    import LambdaNFNImplicits._
    val f: Future[Content] = node ? exprToDo
    implicit val patienceConfig = PatienceConfig(Span(StaticConfig.defaultTimeoutDuration.toMillis, Millis), Span(100, Millis))
    whenReady(f) { content =>
      val res = new String(content.data)
      test(res)
    }
  }

}
