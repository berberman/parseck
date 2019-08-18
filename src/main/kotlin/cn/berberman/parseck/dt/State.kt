package cn.berberman.parseck.dt

interface State<T, R> {

    fun runState(state: T): Pair<R, T>

    infix fun <S> map(f: (R) -> S): State<T, S> = state {
        val (r, s) = runState(it)
        f(r) to s
    }

    infix fun <S> ap(f: State<T, (R) -> S>): State<T, S> =
        state {
            val (a, s) = runState(it)
            val (frs, s1) = f.runState(s)
            frs(a) to s1
        }

    infix fun <S> bind(f: (R) -> State<T, S>): State<T, S> =
        state {
            val (a, s) = runState(it)
            f(a).runState(s)
        }

    companion object {
        fun <T> get(): State<T, T> = state { it to it }

        fun <T> put(a: T): State<T, Unit> = state { Unit to a }
    }

}

fun <T, R> State<T, State<T, R>>.join(): State<T, R> = bind(::id)


fun <T, R> state(f: (T) -> Pair<R, T>): State<T, R> = object :
    State<T, R> {
    override fun runState(state: T): Pair<R, T> = f(state)
}