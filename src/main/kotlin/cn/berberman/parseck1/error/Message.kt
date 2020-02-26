package cn.berberman.parseck1.error

sealed class Message(val msg: String) : Comparable<Message> {

    class SysUnExpect(msg: String) : Message(msg)

    class UnExpect(msg: String) : Message(msg)

    class Expect(msg: String) : Message(msg)

    class Raw(msg: String) : Message(msg)

    override fun compareTo(other: Message): Int =
        when (this) {
            is SysUnExpect -> 0
            is UnExpect    -> 1
            is Expect      -> 2
            is Raw         -> 3
        }

}