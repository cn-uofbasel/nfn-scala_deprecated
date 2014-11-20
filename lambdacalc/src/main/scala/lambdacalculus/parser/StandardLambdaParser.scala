package lambdacalculus.parser

import scala.util.parsing.combinator.{Parsers, PackratParsers}
import scala.util.parsing.combinator.syntactical.StdTokenParsers
import scala.util.parsing.combinator.lexical.StdLexical
import scala.collection.immutable.SortedSet
import com.typesafe.scalalogging.slf4j.Logging

import lambdacalculus.parser.ast._

trait LambdaParser extends Parsers {
  def apply(code: String) = parse(code)

  def parse(code: String):ParseResult[Expr]
}

class StandardLambdaParser extends LambdaParser with StdTokenParsers  with PackratParsers with Logging {
  type Tokens = StdLexical
  val lexical: StdLexical =  new StdLexical {
    override def letter = elem("letter", c => (c.isLetter || c == '/') && c != 'λ' && c != '?')
  }
  val keywords = Set("let", "endlet", "if", "then", "else", "call")
  val unaryLiterals = UnaryOp.values.map(_.toString)
  val binaryLiterals: SortedSet[String] = BinaryOp.values.map(_.toString)

  lexical.delimiters ++= Seq("\\", "λ", "?", ".", "(", ")", "=", ";", "-")
  lexical.reserved ++= keywords ++ binaryLiterals ++ unaryLiterals
  type P[+T] = PackratParser[T]

  val binaryLiteralsToParse = binaryLiterals.map(Parser[String](_)).reduce(_ | _ )
  val unaryLiteralsToParse = unaryLiterals.map(Parser[String](_)).reduce(_ | _ )

  lazy val expr:        P[Expr]       = let | application | notApp
  lazy val notApp:      P[Expr]       = ifthenelse | call | binary | unary | str | variable | number | lambda | parens

  lazy val lambda:      P[Clos]       = positioned(("λ" | "\\")  ~> ident ~ ("." ~> expr) ^^ { case name ~ body => Clos(name, body) })
  lazy val application: P[Application]= positioned(expr ~ notApp ^^ { case left ~ right => Application(left, right) })
  lazy val parens:      P[Expr]       = "(" ~> expr <~ ")"
  lazy val str:         P[Str]        = positioned(stringLit ^^ { case s => Str(s) })
  lazy val variable:    P[Variable]   = positioned(ident ^^ { case name => Variable(name) } )
  lazy val number:      P[Constant]   = negNumber | posNumber
  lazy val negNumber:   P[Constant]   = positioned(numericLit ^^ { case n => Constant(n.toInt) })
  lazy val posNumber:   P[Constant]   = positioned("-" ~> numericLit ^^ {case n => Constant(n.toInt * -1)})
  lazy val let:         P[Let]        = positioned(("let" ~> ident <~ "=") ~ expr ~ ("endlet" ~> expr) ^^
    { case name ~ fun ~ code => Let(name, fun, Some(code))})
  lazy val ifthenelse:  P[IfElse]     = positioned(("if" ~> expr) ~ ("then" ~> expr) ~ ("else" ~> expr) ^^
    { case test ~ thenn ~ otherwise => IfElse(test, thenn, otherwise) })
  lazy val call:       P[Call]        = positioned(("call" ~> numericLit) ~ ident ~ rep(notApp) ^^ { case n ~ i ~ exprs=> Call(i, exprs)})

  // TODO take care of left/right evaluation order
  lazy val unary :      P[UnaryExpr]  = positioned( unaryLiteralsToParse ~ notApp ^^ { case lit ~ v => UnaryExpr(UnaryOp.withName(lit), v)})
  lazy val binary:      P[BinaryExpr] = positioned( binaryLiteralsToParse ~ notApp ~ notApp ^^ { case lit ~ v1 ~ v2 => BinaryExpr(BinaryOp.withName(lit), v1, v2)})


  override def parse(code: String):ParseResult[Expr] = {
    logger.info(s"Parsing: $code")
    val tokens = new lexical.Scanner(code.stripLineEnd)
    phrase(expr)(tokens)
  }
}



