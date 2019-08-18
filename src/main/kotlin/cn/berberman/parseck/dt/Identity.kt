package cn.berberman.parseck.dt


interface Identity<T> {

    fun runIdentity(): T

    infix fun <S> map(f: (T) -> S): Identity<S> = identity {
        f(runIdentity())
    }

    infix fun <S> ap(f: Identity<(T) -> S>): Identity<S> =
        identity {
            f.runIdentity()(runIdentity())
        }

    infix fun <S> bind(f: (T) -> Identity<S>): Identity<S> = f(runIdentity())

}

fun <T> id(t: T): T = t

fun <T> identity(f: () -> T): Identity<T> = object : Identity<T> {
    override fun runIdentity(): T = f()
}