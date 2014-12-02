package lambdacalculus.parser

import lambdacalculus.ParseException

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
    override def letter = elem("letter", isValidLetter)
  }

  def lambdaSymbol = 'λ'

  def isValidLetter(c: Char) = (c.isLetter || c == '/') && c != lambdaSymbol
  def keywords = Set("let", "endlet", "if", "then", "else", "call")
  def unaryLiterals = UnaryOp.values.map(_.toString)
  def binaryLiterals: SortedSet[String] = BinaryOp.values.map(_.toString)


  def delimiters: Seq[String] = Seq(lambdaSymbol.toString, ".", "(", ")", "=", ";", "-")

  lexical.delimiters ++= delimiters
  lexical.reserved ++= keywords ++ binaryLiterals ++ unaryLiterals
  type P[+T] = PackratParser[T]

  val binaryLiteralsToParse = binaryLiterals.map(Parser[String](_)).reduce(_ | _ )
  val unaryLiteralsToParse = unaryLiterals.map(Parser[String](_)).reduce(_ | _ )

  lazy val expr:        P[Expr]       = let | application | notApp
  lazy val notApp:      P[Expr]       = ifthenelse | call | binary | unary | str | variable | number | lambda | parens
  lazy val lambda:      P[Clos]       = positioned(lambdaSymbol.toString  ~> ident ~ ("." ~> expr) ^^ { case name ~ body => Clos(name, body) })
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
  lazy val call:       P[Call]        = positioned(("call" ~> numericLit) ~ ident ~ rep(notApp) ^^ { case n ~ id ~ exprs =>
    if(exprs.size == Integer.parseInt(n) - 1)
      Call(id, exprs)
    else throw new ParseException(s"parsed call with wrong number of arguments (${Integer.parseInt(n) -1} instead of ${exprs.size} ($exprs)), to circumvent this issue put parantheses around calls, introduce an 'endcall' or improve parser")
  })

  // TODO take care of left/right evaluation order
  lazy val unary :      P[UnaryExpr]  = positioned( unaryLiteralsToParse ~ notApp ^^ { case lit ~ v => UnaryExpr(UnaryOp.withName(lit), v)})
  lazy val binary:      P[BinaryExpr] = positioned( binaryLiteralsToParse ~ notApp ~ notApp ^^ { case lit ~ v1 ~ v2 => BinaryExpr(BinaryOp.withName(lit), v1, v2)})


  override def parse(code: String):ParseResult[Expr] = {
    logger.info(s"Parsing: $code")
    val tokens = new lexical.Scanner(code.stripLineEnd)
    phrase(expr)(tokens)
  }
}



