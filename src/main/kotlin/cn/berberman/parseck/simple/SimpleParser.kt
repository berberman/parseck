package cn.berberman.parseck.simple

import cn.berberman.parseck.parser.Parser
import cn.berberman.parseck.parser.UnexpectedChar
import cn.berberman.parseck.parser.UnexpectedEOF

typealias ParserS<R> = Parser<String, R>


fun satisfy(f: (Char) -> Boolean): ParserS<Char> =
    Parser.get<String>() flatMap { s ->
        when {
            s.isEmpty()  -> Parser.throwError(UnexpectedEOF)
            f(s.first()) -> Parser.put(s.takeLast(s.length - 1)) flatMap { Parser.returnM<String, Char>(s.first()) }
            else         -> Parser.throwError(UnexpectedChar(s.first()))
        }
    }

fun char(c: Char) = satisfy { it == c }

fun eof(): ParserS<Unit> =
    Parser.get<String>() flatMap {
        when {
            it.isEmpty() -> Parser.returnM<String, Unit>(Unit)
            else         -> Parser.throwError(UnexpectedEOF)
        }
    }

fun oneOf(chars: List<Char>) =
    satisfy { it in chars }

fun noneOf(chars: List<Char>) =
    satisfy { it !in chars }

fun string(s: String): ParserS<String> =
    when {
        s.isEmpty() -> Parser.returnM("")
        else        -> char(s.first()) flatMap { result ->
            string(s.substring(1)) flatMap { rest ->
                Parser.returnM<String, String>(result + rest)
            }
        }
    }

val digit = satisfy(Char::isDigit)

inline fun <R> returnP(f: () -> R) = Parser.returnM<String, R>(f())