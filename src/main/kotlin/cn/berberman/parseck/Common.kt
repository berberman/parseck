package cn.berberman.parseck

import cn.berberman.parseck.parser.Parser
import cn.berberman.parseck.parser.or
import cn.berberman.parseck.parser.some
import cn.berberman.parseck.simple.ParserS
import cn.berberman.parseck.simple.char
import cn.berberman.parseck.simple.satisfy

//
//val sign = (char('+') or char('-')).some() map {
//    it.map { s -> "${s}1".toInt() }
//        .fold(1, Int::times)
//}
//val space = char(' ') map { Unit }
//val digit = satisfy { it.isDigit() } map { it.toString().toInt() }
//val int = digit.some() map { it.joinToString(separator = "").toInt() }
//val real =
//    int flatMap { a ->
//        char('.') flatMap { dot ->
//            int flatMap { b ->
//                Parser.returnM<String, Double>("$a$dot$b".toDouble())
//            }
//        }
//    } or int.map(Int::toDouble)


//
//

//
//fun <R> lexeme(f: () -> ParserS<R>) =
//    space flatMap { f() flatMap { x -> space flatMap { Parser.returnM<String, R>(x) } } }
//
