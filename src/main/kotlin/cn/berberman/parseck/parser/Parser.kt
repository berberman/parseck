package cn.berberman.parseck.parser

import cn.berberman.fp.util.curried
import cn.berberman.fp.util.either.Either
import cn.berberman.fp.util.either.Left
import cn.berberman.fp.util.either.Right
import cn.berberman.fp.util.state.State
import cn.berberman.fp.util.state.state


interface Parser<T, R> {
    fun runParser(): State<T, Either<ParserException, R>>

    infix fun <U> map(f: (R) -> U) = parser {
        runParser() map { it map { r -> f(r) } }
    }

    infix fun <U> ap(f: Parser<T, (R) -> U>) =
        map { r -> f.map { it(r) } }.join()


    infix fun <U> flatMap(f: (R) -> Parser<T, U>) = parser {
        runParser().flatMap { e ->
            when (e) {
                is Right ->
                    f(e.value).runParser()
                is Left  ->
                    state {
                        Left<ParserException, U>(
                            e.value
                        ) to it
                    }
            }
        }
    }

    companion object {

        private fun <R> err(e: ParserException) =
            Left<ParserException, R>(e)

        private fun <R> suc(a: R) = Right<ParserException, R>(a)

        fun <T> get(): Parser<T, T> =
            parser {
                state<T, Either<ParserException, T>> {
                    Right<ParserException, T>(
                        it
                    ) to it
                }
            }

        fun <T> put(a: T): Parser<T, Unit> =
            parser {
                state<T, Either<ParserException, Unit>> {
                    Right<ParserException, Unit>(
                        Unit
                    ) to a
                }
            }

        fun <T, R> returnM(a: R): Parser<T, R> = parser {
            state<T, Either<ParserException, R>> {
                suc(a) to it
            }
        }

        fun <T, R> throwError(a: ParserException): Parser<T, R> =
            parser {
                state<T, Either<ParserException, R>> {
                    err<R>(
                        a
                    ) to it
                }
            }

        fun <T, R> catchError(p: Parser<T, R>, f: (ParserException) -> Parser<T, R>): Parser<T, R> = parser {
            State.get<T>().flatMap<Either<ParserException, R>> { s ->
                val (e, s1) = p.runParser().runState(s)
                when (e) {
                    is Left  -> state { f(e.value).runParser().runState(s) }
                    is Right -> state { suc(e.value) to s1 }
                }
            }
        }

        fun <T, R1, R2, R> liftA2(t1: Parser<T, R1>, t2: Parser<T, R2>, f: ((R1, R2) -> R)): Parser<T, R> =
            t2 ap t1.map { a -> f.curried()(a) }

        fun <T, R1, R2, R3, R> liftA3(
            t1: Parser<T, R1>,
            t2: Parser<T, R2>,
            t3: Parser<T, R3>,
            f: ((R1, R2, R3) -> R)
        ): Parser<T, R> =
            t3 ap t2.ap(t1.map { a -> f.curried()(a) }
            )

        fun <T, R1, R2, R3, R4, R> liftA4(
            t1: Parser<T, R1>,
            t2: Parser<T, R2>,
            t3: Parser<T, R3>,
            t4: Parser<T, R4>,
            f: ((R1, R2, R3, R4) -> R)
        ): Parser<T, R> =
            t4 ap t3.ap(t2 ap t1.map { a -> f.curried()(a) })

    }

}

fun <T, R> parser(f: () -> State<T, Either<ParserException, R>>): Parser<T, R> = object :
    Parser<T, R> {
    override fun runParser(): State<T, Either<ParserException, R>> = f()
}

fun <T, R> Parser<T, Parser<T, R>>.join() = flatMap { it }


operator fun <T, R> Parser<T, R>.invoke(state: T): Either<ParserException, Pair<R, T>> {
    val (e, rest) = runParser().runState(state)
    return when (e) {
        is Left  -> Left(e.value)
        is Right -> Right(e.value to rest)
    }
}