package cn.berberman.parseck

import cn.berberman.parseck.fp.Either
import cn.berberman.parseck.fp.Left
import cn.berberman.parseck.fp.Right


fun main() {
}

fun <R> ParserS<R>.invoke(s: String): Either<ParserException, Pair<R, String>> {
    val (e, rest) = runParser().runState(s)
    return when (e) {
        is Left -> Left(e.value)
        is Right -> Right(e.value to rest)
    }
}

fun satisfy(f: (Char) -> Boolean): ParserS<Char> =
    Parser.get<String>().bind { s ->
        when {
            s.isEmpty() -> Parser.returnM(UnexpectedEOF)
            f(s.first()) -> Parser.put(s.takeLast(s.length - 1)) bind { Parser.returnM<String, Char>(s.first()) }
            else -> Parser.returnM(UnexpectedChar(s.first()))
        }
    }


infix fun <T, R> Parser<T, R>.or(p: Parser<T, R>): Parser<T, R> {
    TODO()
}

fun <T, R> List<Parser<T, R>>.choice(): Parser<T, R> {
    TODO()
}

fun <T, R> Parser<T, R>.many(): Parser<T, List<R>> {
    TODO()
}

fun eof(): ParserS<Unit> {
    TODO()
}