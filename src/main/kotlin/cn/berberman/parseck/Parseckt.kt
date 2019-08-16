package cn.berberman.parseck

import cn.berberman.parseck.fp.*


class ParserException : RuntimeException()

interface Parser<T, R> {
    fun runParser(): State<T, Either<ParserException, R>>

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

