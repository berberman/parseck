package cn.berberman.parseck.parser

abstract class ParserException : RuntimeException()
class UnexpectedChar(val char: Char) : ParserException()
object UnexpectedEOF : ParserException()
object Unknown : ParserException()