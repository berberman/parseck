package cn.berberman.parseck.pos

data class SourcePos(val name: String, val line: Int, val column: Int) : Comparable<SourcePos> {

    fun incSourceLine(n: Int) = copy(line = line + n)
    fun incSourceColumn(n: Int) = copy(column = column + n)

    fun updatePosChar(char: Char) =
        when (char) {
            '\n' -> SourcePos(name, line + 1, 1)
            '\t' -> SourcePos(name, line, column + 8 - ((column - 1).rem(8)))
            else -> SourcePos(name, line, column + 1)
        }

    fun updatePosString(s: String) =
        s.fold(this) { acc, c -> acc.updatePosChar(c) }

    override fun compareTo(other: SourcePos): Int = line.compareTo(other.line) + column.compareTo(other.column)

    companion object {
        fun initialPos(name: String) = SourcePos(name, 1, 1)
    }
}