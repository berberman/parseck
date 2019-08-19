package cn.berberman.parseck.dt

import cn.berberman.parseck.curried

sealed class Either<L, R> {

    abstract infix fun <T> map(f: (R) -> T): Either<L, T>

    abstract infix fun <T> ap(f: (Either<L, (R) -> T>)): Either<L, T>

    abstract infix fun <T> bind(f: (R) -> Either<L, T>): Either<L, T>

    override fun toString(): String =
        when (this) {
            is Left -> "Left $value"
            is Right -> "Right $value"
        }


    companion object {
        fun <L, R> right(value: R): Either<L, R> = Right(value)

        fun <L, R> left(value: L): Either<L, R> = Left(value)

        fun <L, R1, R2, R> liftA2(t1: Either<L, R1>, t2: Either<L, R2>, f: ((R1, R2) -> R)): Either<L, R> =
            t2 ap t1.map { a -> f.curried()(a) }

        fun <L, R1, R2, R3, R> liftA3(
            t1: Either<L, R1>,
            t2: Either<L, R2>,
            t3: Either<L, R3>,
            f: ((R1, R2, R3) -> R)
        ): Either<L, R> =
            t3 ap t2.ap(t1.map { a -> f.curried()(a) }
            )

        fun <L, R1, R2, R3, R4, R> liftA4(
            t1: Either<L, R1>,
            t2: Either<L, R2>,
            t3: Either<L, R3>,
            t4: Either<L, R4>,
            f: ((R1, R2, R3, R4) -> R)
        ): Either<L, R> =
            t4 ap t3.ap(t2 ap t1.map { a -> f.curried()(a) })

    }
}

class Left<L, R>(val value: L) : Either<L, R>() {

    override fun <T> map(f: (R) -> T): Either<L, T> = Left(value)

    override fun <T> ap(f: Either<L, (R) -> T>): Either<L, T> =
        Left(value)

    override fun <T> bind(f: (R) -> Either<L, T>): Either<L, T> =
        Left(value)

}

class Right<L, R>(val value: R) : Either<L, R>() {

    override fun <T> map(f: (R) -> T): Either<L, T> = Right(f(value))

    override fun <T> ap(f: Either<L, (R) -> T>): Either<L, T> = f.map { it(value) }

    override fun <T> bind(f: (R) -> Either<L, T>): Either<L, T> = f(value)

}

inline fun <L, R> Either<L, R>.then(block: (R) -> Unit): Either<L, R> = apply {
    if (this is Right)
        block(value)
}

inline fun <L, R> Either<L, R>.otherwise(block: (L) -> Unit): Either<L, R> = apply {
    if (this is Left)
        block(value)
}

fun <L, R> Either<L, R>.valueOrNull(): R? =
    when (this) {
        is Left -> null
        is Right -> value
    }

fun <L, R> Either<L, Either<L, R>>.join(): Either<L, R> = bind(::id)

fun <T> T.either(): Right<Any?, T> = Right(this)