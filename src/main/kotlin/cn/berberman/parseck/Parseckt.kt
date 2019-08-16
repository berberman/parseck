package cn.berberman.parseck

import cn.berberman.parseck.fp.*


class ParserException : RuntimeException()

interface Parser<T, R> {
    fun runParser(): State<T, Either<ParserException, R>>

    infix fun <U> bind(f: (R) -> Parser<T, U>) = parser {
        this.runParser().bind<Either<ParserException, U>> { a: Either<ParserException, R> ->
            when (a) {
                is Right<ParserException, R> ->
                {

                    f(a.value).runParser()
                }
                is Left<ParserException, R> -> {
                    mreturn<T, Either<ParserException, U>>(Left<ParserException, U>(a.value))
                }
                else
                    -> throw IllegalAccessException("hh")


            }

        }
    }


}

fun <T, R> parser(f: () -> State<T, Either<ParserException, R>>) = object : Parser<T, R> {
    override fun runParser(): State<T, Either<ParserException, R>> = f()
}

