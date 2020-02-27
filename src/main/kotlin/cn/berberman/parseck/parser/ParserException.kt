package cn.berberman.parseck.parser

abstract class ParserException : RuntimeException() {
    override fun toString(): String = javaClass.simpleName
}

class Unexpected(val msg: String) : ParserException() {
    override fun toString(): String = "${super.toString()} \"$msg\""
}

object UnexpectedEOF : ParserException()
object Unknown : ParserException()