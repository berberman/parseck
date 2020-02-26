package cn.berberman.parseck

import cn.berberman.parseck.parser.*


val sign = (char('+') + char('-')).some() map {
    it.map { s -> "${s}1".toInt() }
        .fold(1, Int::times)
}
val space = char(' ') map { Unit }
val digit = satisfy { it.isDigit() } map { it.toString().toInt() }
val int = digit.some() map { it.joinToString(separator = "").toInt() }
val real =
    int flatMap { a ->
        char('.') flatMap { dot ->
            int flatMap { b ->
                Parser.returnM<String, Double>("$a$dot$b".toDouble())
            }
        }
    } or int.map(Int::toDouble)



fun until(s: String, eat: Boolean = false): ParserS<String> = Parser.get<String>() flatMap {
    if (it.isEmpty())
        returnP { it }
    else it.split(s)[0].let { r ->
        Parser.put(
            it.removePrefix(r).run {
                if (eat)
                    removePrefix(s)
                else this
            }
        ) flatMap { returnP { r } }
    }
}

fun <R> lexeme(f: () -> ParserS<R>) =
    space flatMap { f() flatMap { x -> space flatMap { Parser.returnM<String, R>(x) } } }

inline fun <R> returnP(f: () -> R) = Parser.returnM<String, R>(f())