package cn.berberman.parseck.parser

import cn.berberman.parseck.dt.Either
import cn.berberman.parseck.dt.Left
import cn.berberman.parseck.dt.Right

typealias ParserS<R> = Parser<String, R>

operator fun <T, R> Parser<T, R>.invoke(a: T): Either<ParserException, Pair<R, T>> {
    val (e, rest) = runParser().runState(a)
    return when (e) {
        is Left  -> Left(e.value)
        is Right -> Right(e.value to rest)
    }
}

fun satisfy(f: (Char) -> Boolean): ParserS<Char> =
    Parser.get<String>() bind { s ->
        when {
            s.isEmpty()  -> Parser.throwError(UnexpectedEOF)
            f(s.first()) -> Parser.put(s.takeLast(s.length - 1)) bind { Parser.returnM<String, Char>(s.first()) }
            else         -> Parser.throwError(UnexpectedChar(s.first()))
        }
    }

fun char(c: Char) = satisfy { it == c }

infix fun <T, R> Parser<T, R>.or(p: Parser<T, R>): Parser<T, R> =
    Parser.catchError(this) { p }

operator fun <T, R> Parser<T, R>.plus(p: Parser<T, R>) = or(p)

operator fun <T, R1, R2> Parser<T, R1>.times(p: Parser<T, R2>) = bind { p }

operator fun <T, R> Parser<T, R>.times(n: Int): Parser<T, List<R>> =
    if (n <= 1) bind { Parser.returnM<T, List<R>>(listOf(it)) }
    else bind { result -> times(n - 1) bind { rest -> Parser.returnM<T, List<R>>(listOf(result) + rest) } }

fun <T, R> List<Parser<T, R>>.choice(): Parser<T, R> =
    foldRight(Parser.throwError(Unknown), Parser<T, R>::or)


fun <T, R> Parser<T, R>.many(): Parser<T, List<R>> = some() or Parser.returnM(listOf())

fun <T, R> Parser<T, R>.some(): Parser<T, List<R>> =
    bind { many() bind { rest -> Parser.returnM<T, List<R>>(listOf(it) + rest) } }


fun eof(): ParserS<Unit> =
    Parser.get<String>() bind {
        when {
            it.isEmpty() -> Parser.returnM<String, Unit>(Unit)
            else         -> Parser.throwError(UnexpectedEOF)
        }
    }