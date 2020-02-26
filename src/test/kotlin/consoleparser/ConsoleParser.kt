package consoleparser

import cn.berberman.fp.util.either.valueOrNull
import cn.berberman.parseck.parser.*
import cn.berberman.parseck.simple.ParserS
import cn.berberman.parseck.simple.char
import cn.berberman.parseck.simple.returnP
import cn.berberman.parseck.simple.string
import cn.berberman.parseck.token.float

sealed class RuleAST {

    data class Str(val s: String) : RuleAST()
    object Num : RuleAST()
    object Word : RuleAST()
    object Sign : RuleAST()
    object NA : RuleAST()

    override fun toString(): String = when (this) {
        is Str -> "Str(\"$s\")"
        else -> javaClass.simpleName
    }

    fun build(): String = when (this) {
        is Str -> s
        is Num -> "@num"
        is Word -> "@word"
        is Sign -> "@sign"
        NA -> ""
    }

}

typealias Rule = List<RuleAST>

data class ResultContext(val num: List<Double>, val word: List<String>, val sign: List<Int>)

fun until(s: String, eat: Boolean = false): ParserS<String> = Parser.get<String>() flatMap {
    if (it.isEmpty())
        returnP { it }
    else it.split(s)[0].let { r ->
        Parser.put(
            it.removePrefix(r).run {
                if (eat)
                    removePrefix(s)
                else this
            }
        ) flatMap { returnP { r } }
    }
}

val ruleParser =
    ((Parser.catchError(listOf("@num", "@word", "@sign").map { string(it) }.choice() flatMap  {
        returnP {
            when (it) {
                "@num" -> RuleAST.Num
                "@word" -> RuleAST.Word
                "@sign" -> RuleAST.Sign
                else -> throw Unknown
            }
        }
    }) {
        until("@") flatMap  {
            returnP {
                if (it.isEmpty())
                    RuleAST.NA
                else RuleAST.Str(it.trim())
            }
        }
    }) * 60).map { it.filter { ele -> ele != RuleAST.NA } }


fun Rule.toParser(): ParserS<ResultContext> {
    val nums = mutableListOf<Double>()
    val words = mutableListOf<String>()
    val signs = mutableListOf<Int>()
    val ruleStrings = mapNotNull { it as? RuleAST.Str }.map { it.s }

    fun <R1, R2> connect(p: ParserS<R2>, acc: ParserS<R1>) =
        p flatMap  {
            when (it) {
                is Double -> nums.add(it)
                is String -> if (it !in ruleStrings) words.add(it)
                is Int -> signs.add(it)
            }
            acc
        }
    return map {
        when (it) {
            is RuleAST.Str -> string(it.s)
            RuleAST.Num -> float
            RuleAST.Word -> until("\n") flatMap  { s -> if (s.isEmpty()) Parser.throwError(UnexpectedEOF) else returnP { s } }
            RuleAST.Sign -> char('+') or char('-')
            RuleAST.NA -> throw Unknown
        }.wrapWithSpace()
    }.foldRight<ParserS<*>, ParserS<*>>(returnP { Unit }) { p, acc ->
        connect(p, acc)
    }.flatMap {
        returnP {
            ResultContext(nums, words, signs)
        }
    }
}

fun Rule.build() = joinToString(separator = " ") { it.build() }

fun <R> ParserS<R>.wrapWithSpace() =
    char(' ').many() flatMap  { this flatMap  { result -> char(' ').many() flatMap  { returnP { result } } } }

fun main() {
    val rules = Rules()
    rules
        .addRule("print @word") {
            println(word.first())
        }
        .addRule("@num / @num") {
            println(num[0] / num[1])
        }
        .addRule("A=@num") {
            println("A=${num.first()}")
        }
        .addRule("hello world") {
            println("hello!")
        }

    val parser = rules.buildParser()

    while (true)
        parser(readLine()!!)
}


class Rules {
    private val core = mutableMapOf<Rule, ResultContext.() -> Unit>()

    fun addRule(r: String, block: ResultContext.() -> Unit): Rules = apply {
        //print @num @word
        //print @word @num
        val rule = ruleParser(r).valueOrNull()?.first
            ?: throw IllegalArgumentException("Unable to resolve rule: \"$r\"")
        val matches = core.filter { it.key.size == rule.size }
        if (rule in matches)
            throw IllegalArgumentException("Rule: \"$r\" is existed")
        core[rule] = block

        //TODO merge rules
        //        rule.indices.forEach {
        //            val
        //        }
    }

    fun buildParser(): ParserS<Unit> {
        val map = core.mapKeys { (k, _) -> k.toParser() }
        fun connect(p: ParserS<ResultContext>, acc: ParserS<ResultContext>) =
            Parser.catchError(p flatMap  {
                map.getValue(p).invoke(it)
                returnP { it }
            }) {
                acc
            }

        //TODO should sort
        return map.keys.toList()
            .foldRight(Parser.throwError(Unknown), ::connect) map { Unit }
    }
}