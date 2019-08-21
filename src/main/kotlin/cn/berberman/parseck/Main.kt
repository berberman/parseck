package cn.berberman.parseck

import cn.berberman.parseck.parser.*
import kotlin.coroutines.*


fun main() {
//    val signWithReal = sign bind { s -> real bind { num -> Parser.returnM<String, Double>(s * num) } }
//    println(signWithReal("-+-233.3")

    val opParser = listOf('+', '-', '*', '/').map { char(it) }.choice()
    val exp =
        int bind { num1 ->
            space.many() bind {
                opParser bind { op ->
                    space.many() bind {
                        int bind { num2 ->
                            returnP {
                                when (op) {
                                    '+'  -> num1 + num2
                                    '-'  -> num1 - num2
                                    '*'  -> num1 * num2
                                    '/'  -> num1 / num2
                                    else -> throw Unknown
                                }
                            }
                        }
                    }
                }
            }
        }
    println(exp("1+1"))
    println(exp("1  -  1"))
    println(exp("1 *6"))
    println(exp("8 / 2"))
}

val sign = (char('+') + char('-')).some().map { it.map { s -> "${s}1".toInt() }.fold(1, Int::times).toDouble() }
val space = char(' ') map { Unit }
val digit = satisfy { it.isDigit() } map { it.toString().toInt() }
val int = digit.some() map { it.joinToString(separator = "").toInt() }
val real =
    int.bind { a -> char('.') bind { dot -> int bind { b -> Parser.returnM<String, Double>("$a$dot$b".toDouble()) } } } or int.map(Int::toDouble)

fun string(s: String): Parser<String, String> = when {
    s.isEmpty() -> Parser.returnM("")
    else        -> char(s.first()) bind { result -> string(s.substring(1)) bind { rest -> Parser.returnM<String, String>(result + rest) } }
}

fun <R> lexeme(p: ParserS<R>) = space bind { p bind { x -> space bind { Parser.returnM<String, R>(x) } } }

fun <R> returnP(f: () -> R) = Parser.returnM<String, R>(f())


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