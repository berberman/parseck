package cn.berberman.parseck.token

import cn.berberman.fp.util.either.Either
import cn.berberman.fp.util.either.Left
import cn.berberman.fp.util.either.Right
import cn.berberman.parseck.parser.*
import cn.berberman.parseck.simple.*

val simpleSpace = satisfy(Char::isWhitespace).skipMany1()
fun <T> lexeme(p: ParserS<T>) = p flatMap { x -> simpleSpace flatMap { returnP { x } } }
fun symbol(name: String) = lexeme(string(name))

private val nat = digit.many1() map { it.joinToString("").toInt() }
private val sign =
    char('+') flatMap { returnP { { x: Int -> x } } } or char('-').flatMap { returnP { { x: Int -> -x } } } or returnP { { x: Int -> x } }
private val int = lexeme(sign) flatMap { f -> nat flatMap { n -> returnP { f(n) } } }

private val fraction = char('.') flatMap { digit.many1() flatMap { digits -> returnP { ".${digits.joinToString("")}" } } }
private val expoentP = oneOf(listOf('e', 'E')) flatMap { (oneOf(listOf('+', '-')).map { it.toString() } or returnP { "" }) flatMap { s -> nat flatMap { e -> returnP { "e$s$e" } } } }
private fun fractionExponent(n: Int) = fraction.flatMap { frac -> option("", expoentP) flatMap { expo -> returnP { "$n$frac$expo".toDouble() } } } or expoentP.flatMap { expo -> returnP { "$n$expo".toDouble() } }
private fun fractFloat(n: Int) = fractionExponent(n) flatMap { returnP { Right<Int, Double>(it) as Either<Int, Double> } }
private val decimalFloat = nat flatMap { n -> option(Left(n), fractFloat(n)) }
private val zeroNumFloat = (decimalFloat or fractFloat(0)) or returnP { Left<Int, Double>(0) as Either<Int, Double> }
private val natFloat = char('0').flatMap { zeroNumFloat } or decimalFloat
private val floating = nat flatMap { n -> fractionExponent(n) }

val float = lexeme(floating)
val integer = lexeme(int)
val natural = lexeme(nat)
val naturalOrFloat = lexeme(natFloat)

fun <T> ParserS<T>.parens() = between(symbol("("), this, symbol(")"))
fun <T> ParserS<T>.braces() = between(symbol("{"), this, symbol("}"))
fun <T> ParserS<T>.angles() = between(symbol("<"), this, symbol(">"))

val semi = symbol(";")
val comma = symbol(",")
val dot = symbol(".")
val colon = symbol(":")

fun <T> ParserS<T>.commaSep() = sepBy(comma)
fun <T> ParserS<T>.semiSep() = sepBy(semi)
fun <T> ParserS<T>.commaSep1() = sepBy1(comma)
fun <T> ParserS<T>.semiSep1() = sepBy1(semi)

