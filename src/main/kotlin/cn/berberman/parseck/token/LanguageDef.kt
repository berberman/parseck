package cn.berberman.parseck.token

import cn.berberman.parseck.parser.Parser

data class LanguageDef<T>(
    val commentStart: String,
    val commentEnd: String,
    val commentLine: String,
    val nestedComments: Boolean,
    val identStart: Parser<T, Char>,
    val identLetter: Parser<T, Char>,
    val opStart: Parser<T, Char>,
    val opLetter: Parser<T, Char>,
    val reservedNames: List<String>,
    val reservedOpNames: List<String>,
    val caseSensitive: Boolean
)