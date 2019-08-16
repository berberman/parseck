package cn.berberman.parseck

import cn.berberman.parseck.fp.Either
import cn.berberman.parseck.fp.Left
import cn.berberman.parseck.fp.Right
import cn.berberman.parseck.fp.state


fun main() {
}

fun <R> ParserS<R>.invoke(s: String): Either<ParserException, Pair<R, String>> {
    val (e, rest) = runParser().runState(s)
    return when (e) {
        is Left  -> Left(e.value)
        is Right -> Right(e.value to rest)
    }
}

fun satisfy(f: (Char) -> Boolean): ParserS<Char> {
    return get<String>().bind {s ->
        if (s.length==0) mreturnLeft<String,Char>(UnexpectedEOF)
        else if(f(s.first()))
            put(s.takeLast(s.length-1)) bind {mreturnRight<String,Char>(s.first())}
        else
            mreturnLeft<String,Char>(UnexpectedChar(s.first()))
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