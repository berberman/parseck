package cn.berberman.parseck.legacy

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