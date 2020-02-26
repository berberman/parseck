package cn.berberman.parseck.char

import cn.berberman.parseck.legacy.ParserS
import cn.berberman.parseck.legacy.char
import cn.berberman.parseck.legacy.satisfy
import cn.berberman.parseck.parser.Parser

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