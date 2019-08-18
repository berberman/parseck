package cn.berberman.parseck

import cn.berberman.parseck.dt.valueOrNull
import cn.berberman.parseck.parser.char
import cn.berberman.parseck.parser.invoke
import cn.berberman.parseck.parser.many
import cn.berberman.parseck.parser.or


fun main() {
    println(sign("+1").valueOrNull()?.first)
    println(sign("+-1").valueOrNull()?.first)
    println(sign("+-+1").valueOrNull()?.first)
    println(sign("+-+-1").valueOrNull()?.first)
}

val sign = (char('+') or char('-')).many().map { it.map { s -> "${s}1".toInt() }.fold(1) { acc, i -> acc * i } }


//val digit = satisfy { it.isDigit() } map { it.toInt() }
//val int = digit.some().map { it.joinToString(separator = "").toInt() }
