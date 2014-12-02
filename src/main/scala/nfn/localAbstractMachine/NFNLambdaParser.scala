package nfn.localAbstractMachine

import com.typesafe.scalalogging.slf4j.Logging
import lambdacalculus.parser._
import lambdacalculus.parser.ast._


class NFNLambdaParser extends StandardLambdaParser with Logging {

  // remove "."
  override def delimiters = super.delimiters.filter(_ != ".")

  override def lambdaSymbol = '@'


  // @x (...) a instead of λx.(...) a
  override lazy val lambda:      P[Clos]       = positioned(lambdaSymbol.toString ~> ident ~ expr ^^ { case name ~ body => Clos(name, body) })


  // ifelse true 1 2 instead of if true then 1 else 2
  override lazy val ifthenelse:  P[IfElse]     = positioned(("ifelse" ~> expr) ~ expr ~ expr ^^
    { case test ~ thenn ~ otherwise => IfElse(test, thenn, otherwise) })

}

object StringTestApp extends App {
  val lp = new NFNLambdaParser()
  val r = lp.parse("@x (call 4 /nfn_service_impl_Pandoc /node/node1/doc/tutorial/tutorial_md 'markdown_github' 'latex' ) 1")
  println(r)
}


