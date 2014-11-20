package nfn.localAbstractMachine

import scala.util.parsing.combinator.syntactical.StdTokenParsers
import scala.util.parsing.combinator.PackratParsers
import com.typesafe.scalalogging.slf4j.Logging
import scala.util.parsing.combinator.lexical.StdLexical
import scala.collection.immutable.SortedSet

import lambdacalculus.parser.ast._
import lambdacalculus.parser._


class NFNLambdaParser extends LambdaParser with StdTokenParsers  with PackratParsers with Logging {
  type Tokens = StdLexical
  val lexical: StdLexical =  new StdLexical {
    override def letter = elem("letter", c => (c.isLetter || c == '/') && c != '@')

//    // We override the token and remove all " and ' parts from original implementation
//    override def token: Parser[Token] = {
//          (identChar ~ rep(identChar | digit) ^^ { case first ~ rest => processIdent(first :: rest mkString "")}
//        | digit ~ rep(digit) ^^ { case first ~ rest => NumericLit(first :: rest mkString "")}
//        | delim
//        | failure("illegal character")
//        )
//    }
  }

  val keywords = Set("let", "endlet", "ifelse", "call")
  val unaryLiterals = UnaryOp.values.map(_.toString)
  val binaryLiterals: SortedSet[String] = BinaryOp.values.map(_.toString)


  lexical.delimiters ++= Seq("@", "(", ")", "=", ";", "-")
  lexical.reserved ++= keywords ++ binaryLiterals ++ unaryLiterals
  type P[+T] = PackratParser[T]

  val binaryLiteralsToParse = binaryLiterals.map(Parser[String](_)).reduce(_ | _ )
  val unaryLiteralsToParse = unaryLiterals.map(Parser[String](_)).reduce(_ | _ )

  lazy val expr:        P[Expr]       = let | application | notApp
  lazy val notApp:      P[Expr]       = ifthenelse | call | binary | unary | variable | str | number | lambda | parens

  lazy val lambda:      P[Clos]       = positioned("@" ~> ident ~ expr ^^ { case name ~ body => Clos(name, body) })
  lazy val application: P[Application]= positioned(expr ~ notApp ^^ { case left ~ right => Application(left, right) })
  lazy val parens:      P[Expr]       = "(" ~> expr <~ ")"
  lazy val str:         P[Str]        = positioned(stringLit ^^ { case s => Str(s) })
  lazy val variable:    P[Variable]   = positioned(ident ^^ { case name => Variable(name) } )
  lazy val number:      P[Constant]   = negNumber | posNumber
  lazy val negNumber:   P[Constant]   = positioned(numericLit ^^ { case n => Constant(n.toInt) })
  lazy val posNumber:   P[Constant]   = positioned("-" ~> numericLit ^^ {case n => Constant(n.toInt * -1)})
  lazy val let:         P[Let]        = positioned(("let" ~> ident <~ "=") ~ expr ~ ("endlet" ~> expr) ^^
    { case name ~ fun ~ code => Let(name, fun, Some(code))})
  lazy val ifthenelse:  P[IfElse]     = positioned(("ifelse" ~> expr) ~ expr ~ expr ^^
    { case test ~ thenn ~ otherwise => IfElse(test, thenn, otherwise) })
  lazy val call:       P[Call]      = positioned(("call" ~> numericLit) ~ ident ~ rep(notApp) ^^ { case n ~ i ~ exprs=> Call(i, exprs)})

  // TODO take care of left/right evaluation order
  lazy val unary :      P[UnaryExpr]  = positioned( unaryLiteralsToParse ~ notApp ^^ { case lit ~ v => UnaryExpr(UnaryOp.withName(lit), v)})
  lazy val binary:      P[BinaryExpr] = positioned( binaryLiteralsToParse ~ notApp ~ notApp ^^ { case lit ~ v1 ~ v2 => BinaryExpr(BinaryOp.withName(lit), v1, v2)})


  override def parse(code: String):ParseResult[Expr] = {
    logger.info(s"Parsing: $code")
    val tokens = new lexical.Scanner(code.stripLineEnd)
    phrase(expr)(tokens)
  }
}

object StringTestApp extends App {
  val lp = new NFNLambdaParser()
  val r = lp.parse("call 4 /nfn_service_impl_Pandoc /node/node1/doc/tutorial/tutorial_md 'markdown_github' 'latex'")
  println(r)
}


