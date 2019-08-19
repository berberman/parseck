package cn.berberman.parseck

import cn.berberman.parseck.parser.*


fun main() {
    println(real("233.3"))
}

val sign = (char('+') + char('-')).some().map { it.map { s -> "${s}1".toInt() }.fold(1, Int::times).toDouble() }
val space = char(' ') map { Unit }
val digit = satisfy { it.isDigit() } map { it.toString().toInt() }
val int = digit.some() map { it.joinToString(separator = "").toInt() }
val real =
    int.map(Int::toDouble) or int.bind { a -> char('.') bind { dot -> int bind { b -> Parser.returnM<String, Double>("$a$dot$b".toDouble()) } } }


fun <R> lexeme(p: ParserS<R>) = space bind { p bind { x -> space bind { Parser.returnM<String, R>(x) } } }
