package cn.berberman.parseck

import cn.berberman.parseck.parser.*


fun main() {
    val signWithReal = sign bind { s -> real bind { num -> Parser.returnM<String, Double>(s * num) } }
    println(signWithReal("-+-233.3"))
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
