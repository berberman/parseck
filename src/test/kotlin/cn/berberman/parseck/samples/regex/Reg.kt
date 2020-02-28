package cn.berberman.parseck.samples.regex

import cn.berberman.fp.util.either.valueOrNull
import cn.berberman.parseck.parser.*
import cn.berberman.parseck.simple.ParserS
import cn.berberman.parseck.simple.char
import cn.berberman.parseck.simple.noneOf
import cn.berberman.parseck.simple.returnP

abstract class RegExp

class Normal(val c: Char) : RegExp()
class Str(val es: List<RegExp>) : RegExp()
class Or(val e1: RegExp, val e2: RegExp) : RegExp()
class ZeroOrMore(val e: RegExp) : RegExp()
class Void : RegExp()
class Any : RegExp()

fun MyRegExp.fromMe(): RegExp =
    simplify().run {
        when (this) {
            is MyRegExp.Normal     -> Normal(word)
            is MyRegExp.Str        -> Str(words.map { it.fromMe() })
            is MyRegExp.Sum        -> Str(es.map { it.fromMe() })
            is MyRegExp.Or         -> if (es.size != 2) Void() else Or(es[0].fromMe(), es[1].fromMe())
            is MyRegExp.ZeroOrMore -> ZeroOrMore(e.fromMe())
            is MyRegExp.Any        -> Any()
            MyRegExp.Null          -> Void()
        }
    }

sealed class MyRegExp {

    class Normal(val word: Char) : MyRegExp()

    class Str(val words: List<MyRegExp>) : MyRegExp()

    class Sum(val es: List<MyRegExp>) : MyRegExp()

    class Or(val es: List<MyRegExp>) : MyRegExp()

    class ZeroOrMore(val e: MyRegExp) : MyRegExp()

    class Any : MyRegExp()

    object Null : MyRegExp()

    override fun toString(): String =
        when (this) {
            is Normal     -> "Normal('$word')"
            is Str        -> "Str([${words.joinToString(", ")}])"
            is Sum        -> "Sum([${es.joinToString(", ")}])"
            is Or         -> "Or(${es.joinToString(", ")})"
            is ZeroOrMore -> "ZeroOrMore($e)"
            is Any        -> "Any()"
            Null          -> ""
        }

}

typealias RegExpParser = ParserS<MyRegExp>

val keys = listOf('(', ')', '*', '.', '|')

val normal: RegExpParser = char('.').map { MyRegExp.Any() as MyRegExp } or noneOf(keys).map { MyRegExp.Normal(it) as MyRegExp }
val group: RegExpParser = char('(').lookAhead() flatMap { between(char('('), regex(), char(')')) }
fun starOr(e: MyRegExp): RegExpParser = char('*').map { MyRegExp.ZeroOrMore(e) as MyRegExp } or returnP { e }
val ele: RegExpParser = group or normal
val posted: RegExpParser = ele flatMap { starOr(it) }
val branch: RegExpParser = posted.many1() map { if (it.size != 1) MyRegExp.Sum(it) else it.first() }
fun regex(): RegExpParser = branch.sepBy1(char('|')) map { if (it.size != 1) MyRegExp.Or(it) else it.first() }

fun MyRegExp.flatten() =
    when (this) {
        is MyRegExp.Or  -> es.flatMap {
            if (it is MyRegExp.Or)
                it.es
            else listOf(it)
        }
        is MyRegExp.Sum -> es.flatMap {
            when (it) {
                is MyRegExp.Sum -> it.es
                is MyRegExp.Str -> it.words
                else            -> listOf(it)
            }

        }
        else            -> listOf()
    }

tailrec fun mergeNormalToStr(es: List<MyRegExp>): List<MyRegExp> {
    if (es.isEmpty()) return listOf()
    val first = es[0]
    if (es.size == 1) return listOf(first)
    val second = es[1]
    val rest = es.drop(2)

    if (first is MyRegExp.Normal && second is MyRegExp.Normal)
        return mergeNormalToStr(listOf(MyRegExp.Str(listOf(first, second))) + rest)
    if (first is MyRegExp.Normal && second is MyRegExp.Str)
        return mergeNormalToStr(listOf(MyRegExp.Str(listOf(first) + second.words)) + rest)
    if (first is MyRegExp.Normal && second is MyRegExp.Any)
        return mergeNormalToStr(listOf(MyRegExp.Str(listOf(first, second) + rest)))

    if (first is MyRegExp.Str && second is MyRegExp.Normal)
        return mergeNormalToStr(listOf(MyRegExp.Str(first.words + second)) + rest)
    if (first is MyRegExp.Str && second is MyRegExp.Str)
        return mergeNormalToStr(listOf(MyRegExp.Str(first.words + second.words)) + rest)
    if (first is MyRegExp.Str && second is MyRegExp.Any)
        return mergeNormalToStr(listOf(MyRegExp.Str(first.words + second)) + rest)

    if (first is MyRegExp.Any && second is MyRegExp.Normal)
        return mergeNormalToStr(listOf(MyRegExp.Str(listOf(first, second) + rest)))
    if (first is MyRegExp.Any && second is MyRegExp.Str)
        return mergeNormalToStr(listOf(MyRegExp.Str(listOf(first) + second.words + rest)))
    if (first is MyRegExp.Any && second is MyRegExp.Any)
        return mergeNormalToStr(listOf(MyRegExp.Str(listOf(first, second))) + rest)

    return listOf(first) + mergeNormalToStr(es.drop(1))
}

fun MyRegExp.simplify(): MyRegExp =
    when (this) {
        is MyRegExp.Or  -> {
            val s = flatten()
            when {
                s.isEmpty() -> MyRegExp.Null
                s.size == 1 -> s.first()
                else        -> MyRegExp.Or(s.map { it.simplify() })
            }
        }
        is MyRegExp.Sum -> {
            val s = mergeNormalToStr(flatten().filter { it !is MyRegExp.Null })
            when {
                s.isEmpty() -> MyRegExp.Null
                s.size == 1 -> s.first()
                else        -> MyRegExp.Sum(s.map { it.simplify() }.flatMap { if (it is MyRegExp.Str) it.words else listOf(it) })
            }
        }
        else            -> this
    }

fun main() {
    val parser = regex()
    val result = parser("((((mck*v(r|v)m)za*)j)|o)")
    val data = result.valueOrNull()!!.first
    println(data.simplify().simplify())
}
