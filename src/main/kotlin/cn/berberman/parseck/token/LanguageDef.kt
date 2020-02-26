package cn.berberman.parseck.token

import cn.berberman.parseck.simple.ParserS

data class LanguageDef(
    val commentStart: String,
    val commentEnd: String,
    val commentLine: String,
    val nestedComments: Boolean,
    val identStart: ParserS<Char>,
    val identLetter: ParserS<Char>,
    val opStart: ParserS<Char>,
    val opLetter: ParserS<Char>,
    val reservedNames: List<String>,
    val reservedOpNames: List<String>,
    val caseSensitive: Boolean
)