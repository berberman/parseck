package cn.berberman.parseck

import cn.berberman.parseck.fp.*


sealed class ParserException : RuntimeException()
class UnexpectedChar(val char: Char) : ParserException()
object UnexpectedEOF : ParserException()
object Unknown : ParserException()

typealias ParserS<R> = Parser<String, R>

interface Parser<T, R> {
    fun runParser(): State<T, Either<ParserException, R>>

    infix fun <U> map(f: (R) -> U) = parser {
        runParser() map { it map { r -> f(r) } }
    }

    infix fun <U> ap(f: Parser<T, (R) -> U>) =
        map { r -> f.map { it(r) } }.join()


    infix fun <U> bind(f: (R) -> Parser<T, U>) = parser {
        runParser().bind { e ->
            when (e) {
                is Right ->
                    f(e.value).runParser()
                is Left ->
                    state { Left<ParserException, U>(e.value) to it }
            }
        }
    }

    companion object {

        private fun <R> err(e: ParserException) = Left<ParserException, R>(e)
        private fun <R> suc(a: R) = Right<ParserException, R>(a)

        fun <T> get(): Parser<T, T> =
            parser { state<T, Either<ParserException, T>> { Right<ParserException, T>(it) to it } }

        fun <T> put(a: T): Parser<T, Unit> =
            parser { state<T, Either<ParserException, Unit>> { Right<ParserException, Unit>(Unit) to a } }

        fun <T, R> returnM(a: R): Parser<T, R> = parser { state<T, Either<ParserException, R>> { suc(a) to it } }

        fun <T, R> throwError(a: ParserException): Parser<T, R> =
            parser { state<T, Either<ParserException, R>> { err<R>(a) to it } }
    }

}

fun <T, R> parser(f: () -> State<T, Either<ParserException, R>>): Parser<T, R> = object : Parser<T, R> {
    override fun runParser(): State<T, Either<ParserException, R>> = f()
}

fun <T, R> Parser<T, Parser<T, R>>.join() = bind(::id)

