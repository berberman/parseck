package cn.berberman.parseck1.error

import cn.berberman.parseck1.pos.SourcePos

data class ParserError(
    val pos: SourcePos,
    val messages: List<Message>
) {
    fun merge(other: ParserError) =
        when {
            other.messages.isEmpty() && messages.isNotEmpty() -> this
            messages.isEmpty() && other.messages.isNotEmpty() -> other
            else                                              -> when {
                pos == other.pos -> ParserError(pos, messages + other.messages)
                pos > other.pos  -> this
                else             -> other
            }
        }
}