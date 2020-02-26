package cn.berberman.parseck.token

import cn.berberman.fp.util.either.Either
import cn.berberman.parseck.parser.Parser

data class TokenParser<T>(
    val identifier: Parser<T, String>,
    val reserved: Parser<T, Unit>,
    val operator: Parser<T, String>,
    val reservedOp: Parser<T, Unit>,
    val charLiteral: Parser<T, Char>,
    val stringLiteral: Parser<T, Char>,
    val natural: Parser<T, Int>,
    val integer: Parser<T, Int>,
    val float: Parser<T, Int>,
    val naturalOrFloat: Parser<T, Either<Int, Double>>,
    val decimal: Parser<T, Int>,
    val hexDecimal: Parser<T, Int>,
    val octal: Parser<T, Int>,
    val symbol: (String) -> Parser<T, String>,
    val lexeme: IdentityWrap<T, *>,
    val whiteSpace: Parser<T, Unit>,
    val parens: IdentityWrap<T, *>,
    val angles: IdentityWrap<T, *>,
    val brackets: IdentityWrap<T, *>,
    val semi: Parser<T, String>,
    val comma: Parser<T, String>,
    val colon: Parser<T, String>,
    val dot: Parser<T, String>,
    val semiSep: ListWrap<T, *>,
    val semiSep1: ListWrap<T, *>,
    val commaSep: ListWrap<T, *>,
    val commaSep1: ListWrap<T, *>
)

// to express polymorphism
data class IdentityWrap<T, R>(val core: (Parser<T, R>) -> Parser<T, R>)

data class ListWrap<T, R>(val core: (Parser<T, R>) -> Parser<T, List<R>>)
