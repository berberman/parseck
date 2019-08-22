package consoleparser

import cn.berberman.parseck.*
import cn.berberman.parseck.dt.valueOrNull
import cn.berberman.parseck.parser.*

sealed class DefAST {
    class Str(val s: String) : DefAST()
    object Num : DefAST()
    object Word : DefAST()
    object Sign : DefAST()
    object NA : DefAST()

    override fun toString(): String = when (this) {
        is Str -> "Str(\"$s\")"
        else   -> javaClass.simpleName
    }

}

data class ResultContext(val num: List<Double>, val word: List<String>, val sign: List<Int>)

val pronouns = listOf("@num", "@word", "@sign")

val parseDef =
    ((Parser.catchError(pronouns.map { string(it) }.choice() bind {
        returnP {
            when (it) {
                "@num"  -> DefAST.Num
                "@word" -> DefAST.Word
                "@sign" -> DefAST.Sign
                else    -> throw Unknown
            }
        }
    }) {
        until("@") bind {
            returnP {
                if (it.isEmpty())
                    DefAST.NA
                else DefAST.Str(it.trim())
            }
        }
    }) * 30).map { it.filter { ele -> ele != DefAST.NA } }


fun List<DefAST>.toParser(): ParserS<ResultContext> {
    val nums = mutableListOf<Double>()
    val words = mutableListOf<String>()
    val signs = mutableListOf<Int>()

    fun <R1, R2> connect(p: ParserS<R2>, acc: ParserS<R1>) =
        p bind {
            when (it) {
                is Double -> nums.add(it)
                is String -> words.add(it)
                is Int    -> signs.add(it)
            }
            acc
        }

    return map {
        when (it) {
            is DefAST.Str -> string(it.s)
            DefAST.Num    -> real
            DefAST.Word   -> until("\n")
            DefAST.Sign   -> sign
            DefAST.NA     -> throw Unknown
        }.wrapWithSpace()
    }.foldRight<ParserS<*>, ParserS<*>>(returnP { Unit }) { p, acc ->
        connect(p, acc)
    }.bind {
        returnP {
            ResultContext(nums, words, signs)
        }
    }
}

fun <R> ParserS<R>.wrapWithSpace() =
    space.many() bind { this bind { result -> space.many() bind { returnP { result } } } }

fun main() {
    val a = "print @word"
    val b = "hello"
    val c = "@num / @num"
    val d = "A=@num"
    val e = "hello world"
    println(parseDef(a))
    println(parseDef(b))
    println(parseDef(c))
    println(parseDef(d))
    println(parseDef(e))
    println()
    println()
    println()
    println(parseDef(a).valueOrNull()!!.first.toParser()("print nya~"))
    println(parseDef(c).valueOrNull()!!.first.toParser()("2.0 / 3.0"))
    println(parseDef(d).valueOrNull()!!.first.toParser()("A=6666666"))

}