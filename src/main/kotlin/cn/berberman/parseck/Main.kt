package cn.berberman.parseck

import cn.berberman.parseck.fp.Either
import cn.berberman.parseck.fp.Left
import cn.berberman.parseck.fp.Right
import cn.berberman.parseck.fp.valueOrNull


fun main() {
    val a = satisfy { it == 'A' }
    val b = satisfy { it == 'B' }
    val c = a or b
    val e = c("BASAbc")
    println(e.valueOrNull())
}

operator fun <T, R> Parser<T, R>.invoke(a: T): Either<ParserException, Pair<R, T>> {
    val (e, rest) = runParser().runState(a)
    return when (e) {
        is Left -> Left(e.value)
        is Right -> Right(e.value to rest)
    }
}

fun satisfy(f: (Char) -> Boolean): ParserS<Char> =
    Parser.get<String>() bind { s ->
        when {
            s.isEmpty() -> Parser.throwError(UnexpectedEOF)
            f(s.first()) -> Parser.put(s.takeLast(s.length - 1)) bind { Parser.returnM<String, Char>(s.first()) }
            else -> Parser.throwError(UnexpectedChar(s.first()))
        }
    }


infix fun <T, R> Parser<T, R>.or(p: Parser<T, R>): Parser<T, R> =
    Parser.get<T>() bind {
        val (e, s) = runParser().runState(it)
        Parser.put(s) bind {
            when (e) {
                is Left -> p
                is Right -> Parser.returnM(e.value)
            }
        }
    }

fun <T, R> List<Parser<T, R>>.choice(): Parser<T, R> {
    TODO()
}

fun <T, R> Parser<T, R>.many(): Parser<T, List<R>> {
    fun f() = bind { result ->
        many().bind { rest ->
            Parser.returnM<T, List<R>>(listOf(result) + rest)
        }
    }
    TODO()
}

fun eof(): ParserS<Unit> =
    Parser.get<String>() bind {
        when {
            it.isEmpty() -> Parser.returnM<String, Unit>(Unit)
            else -> Parser.throwError(UnexpectedEOF)
        }
    }