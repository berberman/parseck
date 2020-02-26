package reg

import cn.berberman.parseck.parser.*
import cn.berberman.parseck.simple.*

sealed class RegExp {

    data class Normal(val word: Char) : RegExp()

    data class Str(val words: List<Normal>) : RegExp()

    data class Or(val e1: RegExp, val e2: RegExp) : RegExp()

    data class ZeroOrMore(val e: RegExp) : RegExp()

    object Any : RegExp()

}

typealias RegExpParser = ParserS<RegExp>

val keys = listOf('(', ')', '*', '.', '|')

val normal: RegExpParser = satisfy { it !in keys } map { RegExp.Normal(it) }
val str: RegExpParser = normal.moreThanOne() map { RegExp.Str(it as List<RegExp.Normal>) }
val any: RegExpParser = char('.') map { RegExp.Any }

fun <T, R> Parser<T, R>.moreThanOne() = flatMap { x -> some() flatMap { xs -> ParserS.returnM<T, List<R>>(xs.toMutableList().apply { add(0, x) }) } }
fun <T, R> Parser<T, R>.repeat(times: Int = 1): Parser<T, List<R>> = map { listOf(it) } flatMap { l1 -> if (times == 1) Parser.returnM(l1) else repeat(times - 1) flatMap { l2 -> Parser.returnM<T, List<R>>(l1 + l2) } }

val leftBracket = char('(')
val rightBracket = char(')')
val pLine = char('|')
val star = char('*')

val singleExp: RegExpParser = str or normal or any

val orExp: RegExpParser = (singleExp) flatMap { e1 -> pLine flatMap { zeroOrMoreExp or pSingleExp or singleExp flatMap { e2 -> returnP { RegExp.Or(e1, e2) as RegExp } } } }
val pSingleExp: RegExpParser = leftBracket flatMap { orExp or singleExp } flatMap { e -> rightBracket flatMap { returnP { e } } }

val zeroOrMoreExp: RegExpParser = listOf(orExp, pSingleExp, singleExp).choice() flatMap { e -> star flatMap { returnP { RegExp.ZeroOrMore(e) as RegExp } } }

val regExp = listOf(orExp, zeroOrMoreExp, pSingleExp, singleExp).choice()
val regExps = regExp.many()


fun main() {
    val let = string("let").lookAhead()
    println(let("let"))
}



