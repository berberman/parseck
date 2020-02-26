package cn.berberman.parseck.parser

import cn.berberman.parseck1.pos.SourcePos

data class ParserState(val input: String, val pos: SourcePos)