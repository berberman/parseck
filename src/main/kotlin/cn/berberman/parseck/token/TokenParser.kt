package cn.berberman.parseck.token

import cn.berberman.fp.util.either.Either
import cn.berberman.parseck.simple.ParserS

data class TokenParser(
    val identifier: ParserS<String>,
    val reserved: ParserS<Unit>,
    val operator: ParserS<String>,
    val reservedOp: ParserS<Unit>,
    val charLiteral: ParserS<Char>,
    val stringLiteral: ParserS<Char>,
    val natural: ParserS<Int>,
    val integer: ParserS<Int>,
    val float: ParserS<Int>,
    val naturalOrFloat: ParserS<Either<Int, Double>>,
    val decimal: ParserS<Int>,
    val hexDecimal: ParserS<Int>,
    val octal: ParserS<Int>,
    val symbol: (String) -> ParserS<String>,
    val lexeme: IdentityWrap<*>,
    val whiteSpace: ParserS<Unit>,
    val parens: IdentityWrap<*>,
    val angles: IdentityWrap<*>,
    val brackets: IdentityWrap<*>,
    val semi: ParserS<String>,
    val comma: ParserS<String>,
    val colon: ParserS<String>,
    val dot: ParserS<String>,
    val semiSep: ListWrap<*>,
    val semiSep1: ListWrap<*>,
    val commaSep: ListWrap<*>,
    val commaSep1: ListWrap<*>
)

// to express polymorphism
data class IdentityWrap<R>(val core: (ParserS<R>) -> ParserS<R>)

data class ListWrap<R>(val core: (ParserS<R>) -> ParserS<List<R>>)
