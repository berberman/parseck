package cn.berberman.parseck.parser

import cn.berberman.fp.util.maybe.Maybe
import cn.berberman.fp.util.state.State


infix fun <T, R> Parser<T, R>.or(p: Parser<T, R>): Parser<T, R> = Parser.catchError(this) { p }

operator fun <T, R> Parser<T, R>.plus(p: Parser<T, R>) = or(p)

operator fun <T, R1, R2> Parser<T, R1>.times(p: Parser<T, R2>) = flatMap { p }

fun <T, R> Parser<T, R>.count(n: Int): Parser<T, List<R>> =
    if (n <= 1) flatMap { Parser.returnM<T, List<R>>(listOf(it)) }
    else flatMap { result ->
        this@count.count(n - 1) flatMap { rest ->
            Parser.returnM<T, List<R>>(listOf(result) + rest)
        }
    }

operator fun <T, R> Parser<T, R>.times(n: Int): Parser<T, List<R>> = count(n)

fun <T, R> List<Parser<T, R>>.choice(): Parser<T, R> =
    foldRight(Parser.throwError(Unknown), Parser<T, R>::or)

fun <T, R> chainl1(p: Parser<T, R>, op: Parser<T, ((R, R) -> R)>): Parser<T, R> {
    fun rest(x: R): Parser<T, R> = (op flatMap { f ->
        p flatMap { y -> rest(f(x, y)) }
    }) or Parser.returnM(x)
    return p flatMap { x -> rest(x) }
}

fun <T, R> chainl(p: Parser<T, R>, op: Parser<T, (R, R) -> R>, x: R) = chainl1(p, op) or Parser.returnM(x)

private object Chainr1Helper {
    fun <T, R> scan(p: Parser<T, R>, op: Parser<T, (R, R) -> R>): Parser<T, R> =
        p flatMap { x -> rest(p, op, x) }

    fun <T, R> rest(p: Parser<T, R>, op: Parser<T, ((R, R) -> R)>, x: R) =
        (op flatMap { f -> scan(p, op) flatMap { y -> Parser.returnM<T, R>(f(x, y)) } }) or Parser.returnM(x)
}

fun <T, R> chainr1(p: Parser<T, R>, op: Parser<T, ((R, R) -> R)>): Parser<T, R> = Chainr1Helper.scan(p, op)

fun <T, R> chainr(p: Parser<T, R>, op: Parser<T, (R, R) -> R>, x: R) = chainr1(p, op) or Parser.returnM(x)

fun <T, R> Parser<T, R>.many(): Parser<T, List<R>> = some() or Parser.returnM(listOf())

fun <T, R> Parser<T, R>.many1() = some()

fun <T, R> Parser<T, R>.some(): Parser<T, List<R>> =
    flatMap { many() flatMap { rest -> Parser.returnM<T, List<R>>(listOf(it) + rest) } }

fun <T, End, R> Parser<T, R>.manyTill(end: Parser<T, End>): Parser<T, List<R>> =
    end.flatMap { Parser.returnM<T, List<R>>(listOf()) } or flatMap { x -> manyTill(end) flatMap { xs -> Parser.returnM<T, List<R>>(listOf(x) + xs) } }


fun <T, R> option(x: R, p: Parser<T, R>) = p or Parser.returnM(x)

fun <T, R> Parser<T, R>.optionMaybe() = option(Maybe.empty(), map(Maybe.Companion::from))

fun <T, R> Parser<T, R>.optional() = (flatMap { Parser.returnM<T, Unit>(Unit) }) or Parser.returnM(Unit)

fun <T, Open, R, Close> between(open: Parser<T, Open>, p: Parser<T, R>, close: Parser<T, Close>) =
    open flatMap { p flatMap { x -> close flatMap { Parser.returnM<T, R>(x) } } }

fun <T, R> Parser<T, R>.skipMany(): Parser<T, Unit> =
    (flatMap { skipMany() }) or Parser.returnM(Unit)

fun <T, R> Parser<T, R>.skipMany1(): Parser<T, Unit> =
    flatMap { skipMany() }

fun <T, Sep, R> Parser<T, R>.sepBy1(sep: Parser<T, Sep>) =
    flatMap { x -> (sep flatMap { this }).many() flatMap { xs -> Parser.returnM<T, List<R>>(listOf(x) + xs) } }

fun <T, Sep, R> Parser<T, R>.sepBy(sep: Parser<T, Sep>) = sepBy1(sep) or Parser.returnM(listOf())

fun <T, Sep, R> Parser<T, R>.sepEndBy1(sep: Parser<T, Sep>) =
    flatMap { x -> sep flatMap { (sepEndBy(sep) flatMap { xs -> Parser.returnM<T, List<R>>(listOf(x) + xs) }) or Parser.returnM(listOf(x)) } }

fun <T, Sep, R> Parser<T, R>.sepEndBy(sep: Parser<T, Sep>): Parser<T, List<R>> = sepEndBy1(sep) or Parser.returnM(listOf())

fun <T, R> Parser<T, R>.lookAhead() = Parser.get<T>() flatMap { before ->
    val (e, _) = runParser().runState(before)
    parser { State.put(before) map { e } }
}

fun <T, R> Parser<T, R>.notFollowedBy() =
    (flatMap { c -> Parser.throwError<T, Unit>(Unexpected(c.toString())) }) or Parser.returnM(Unit)

// TODO
//fun <T, R> Parser<T, R>.manyF(): Parser<T, List<R>> = parser {
//    state {
//        var ch:Pair<Either<ParserException, R>, T>;
//        var results= listOf<R>();
//        ch=this.runParser().runState(it)
//
//        while (ch.first is Right){
//            results+= listOf<R>(ch.first.value);
//
//        }
//        return@state Parser.returnM<T, List<R>>(results);
//
//    }
//}