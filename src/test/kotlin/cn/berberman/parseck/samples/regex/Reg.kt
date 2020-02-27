package cn.berberman.parseck.samples.regex

import cn.berberman.fp.util.either.valueOrNull
import cn.berberman.parseck.parser.*
import cn.berberman.parseck.simple.ParserS
import cn.berberman.parseck.simple.char
import cn.berberman.parseck.simple.noneOf
import cn.berberman.parseck.simple.returnP

sealed class RegExp {

    class Normal(val word: Char) : RegExp()

    class Str(val words: List<RegExp>) : RegExp()

    class Sum(val es: List<RegExp>) : RegExp()

    class Or(val es: List<RegExp>) : RegExp()

    class ZeroOrMore(val e: RegExp) : RegExp()

    class Any : RegExp()

    object Null : RegExp()

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

typealias RegExpParser = ParserS<RegExp>

val keys = listOf('(', ')', '*', '.', '|')

val normal: RegExpParser = char('.').map { RegExp.Any() as RegExp } or noneOf(keys).map { RegExp.Normal(it) as RegExp }
val group: RegExpParser = char('(').lookAhead() flatMap { between(char('('), regex(), char(')')) }
fun starOr(e: RegExp): RegExpParser = char('*').map { RegExp.ZeroOrMore(e) as RegExp } or returnP { e }
val ele: RegExpParser = group or normal
val posted: RegExpParser = ele flatMap { starOr(it) }
val branch: RegExpParser = posted.many1() map { if(it.size!=1) RegExp.Sum(it) else it.first() }
fun regex(): RegExpParser = branch.sepBy1(char('|')) map { if(it.size!=1) RegExp.Or(it) else it.first() }

fun RegExp.flatten() =
    when (this) {
        is RegExp.Or  -> es.flatMap {
            if (it is RegExp.Or)
                it.es
            else listOf(it)
        }
        is RegExp.Sum -> es.flatMap {
            if (it is RegExp.Sum)
                it.es
            else listOf(it)
        }
        else                                                                -> listOf()
    }

tailrec fun mergeNormalToStr(es: List<RegExp>): List<RegExp> {
    if (es.isEmpty()) return listOf()
    val first = es[0]
    if (es.size == 1) return listOf(first)
    val second = es[1]
    val rest = es.drop(2)

    if (first is RegExp.Normal && second is RegExp.Normal)
        return mergeNormalToStr(listOf(RegExp.Str(listOf(first, second))) + rest)
    if (first is RegExp.Normal && second is RegExp.Str)
        return mergeNormalToStr(listOf(RegExp.Str(listOf(first) + second.words)) + rest)
    if (first is RegExp.Normal && second is RegExp.Any)
        return mergeNormalToStr(listOf(RegExp.Str(listOf(first, second) + rest)))

    if (first is RegExp.Str && second is RegExp.Normal)
        return mergeNormalToStr(listOf(RegExp.Str(first.words + second)) + rest)
    if (first is RegExp.Str && second is RegExp.Str)
        return mergeNormalToStr(listOf(RegExp.Str(first.words + second.words)) + rest)
    if (first is RegExp.Str && second is RegExp.Any)
        return mergeNormalToStr(listOf(RegExp.Str(first.words + second)) + rest)

    if (first is RegExp.Any && second is RegExp.Normal)
        return mergeNormalToStr(listOf(RegExp.Str(listOf(first, second) + rest)))
    if (first is RegExp.Any && second is RegExp.Str)
        return mergeNormalToStr(listOf(RegExp.Str(listOf(first) + second.words + rest)))
    if (first is RegExp.Any && second is RegExp.Any)
        return mergeNormalToStr(listOf(RegExp.Str(listOf(first, second))) + rest)

    return listOf(first) + mergeNormalToStr(es.drop(1))
}

fun RegExp.simplify(): RegExp =
    when (this) {
        is RegExp.Or  -> {
            val s = flatten()
            when {
                s.isEmpty() -> RegExp.Null
                s.size == 1 -> s.first()
                else        -> RegExp.Or(s.map { it.simplify() })
            }
        }
        is RegExp.Sum -> {
            val s = mergeNormalToStr(flatten().filter { it !is RegExp.Null })
            when {
                s.isEmpty() -> RegExp.Null
                s.size == 1 -> s.first()
                else        -> RegExp.Sum(s.map { it.simplify() })
            }
        }
        else                                                                -> this
    }

fun main() {
    val parser = regex()
    val result = parser("(a.*)|(bb)")
    val data = result.valueOrNull()!!.first
    println(data.simplify().simplify().simplify())
}
