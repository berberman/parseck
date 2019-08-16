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
                is Left  ->
                    state { Left<ParserException, U>(e.value) to it }
            }
        }
    }


}

fun <T, R> parser(f: () -> State<T, Either<ParserException, R>>): Parser<T, R> = object : Parser<T, R> {
    override fun runParser(): State<T, Either<ParserException, R>> = f()
}

fun <T, R> Parser<T, Parser<T, R>>.join() = bind(::id)

