package cn.berberman.parseck.parser

import cn.berberman.parseck.pos.SourcePos

data class ParserState(val input: String, val pos: SourcePos)