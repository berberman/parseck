package cn.berberman.parseck

import cn.berberman.parseck.parser.*
import kotlin.coroutines.*


val sign = (char('+') + char('-')).some().map { it.map { s -> "${s}1".toInt() }.fold(1, Int::times) }
val space = char(' ') map { Unit }
val digit = satisfy { it.isDigit() } map { it.toString().toInt() }
val int = digit.some() map { it.joinToString(separator = "").toInt() }
val real =
    int.bind { a -> char('.') bind { dot -> int bind { b -> Parser.returnM<String, Double>("$a$dot$b".toDouble()) } } } or int.map(
        Int::toDouble
    )

fun string(s: String): ParserS<String> = when {
    s.isEmpty() -> Parser.returnM("")
    else -> char(s.first()) bind { result -> string(s.substring(1)) bind { rest -> Parser.returnM<String, String>(result + rest) } }
}

fun until(s: String, eat: Boolean = false): ParserS<String> = Parser.get<String>() bind {
    if (it.isEmpty())
        returnP { it }
    else it.split(s)[0].let { r ->
        Parser.put(
            it.removePrefix(r).run {
                if (eat)
                    removePrefix(s)
                else this
            }
        ) bind { returnP { r } }
    }
}

fun <R> lexeme(f: () -> ParserS<R>) =
    space bind { f() bind { x -> space bind { Parser.returnM<String, R>(x) } } }

inline fun <R> returnP(f: () -> R) = Parser.returnM<String, R>(f())


///////////////////////////////////////////////////////////////////
suspend fun <R> bind(p: ParserS<R>): R = suspendCoroutine { cont ->
    cont.context[ParserContext]!!.c bind {
        @Suppress("UNCHECKED_CAST")
        cont.resume(it as R)
        p
    }
}

fun `do`(f: suspend () -> ParserS<*>): ParserS<*> {
    var init: ParserS<*> = Parser.returnM<String, Any>(Unit)
    f.startCoroutine(object : Continuation<ParserS<*>> {
        override val context: CoroutineContext
            get() = ParserContext(init)

        override fun resumeWith(result: Result<ParserS<*>>) {
            result.onSuccess { p ->
                init = p
            }.onFailure {
                throw it
            }
        }
    })
    return init
}

class ParserContext<R>(val c: ParserS<R>) : AbstractCoroutineContextElement(ParserContext) {
    companion object : CoroutineContext.Key<ParserContext<*>>
}