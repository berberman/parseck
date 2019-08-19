package cn.berberman.parseck.parser

abstract class ParserException : RuntimeException() {
    override fun toString(): String = javaClass.simpleName
}

class UnexpectedChar(val char: Char) : ParserException() {
    override fun toString(): String = "${super.toString()} $char"
}

object UnexpectedEOF : ParserException()
object Unknown : ParserException()