# parseck

使用 Kotlin 实现的简易 Parser Combinator，大部分 Parsec 的功能都没有实现。

## 简介

对于一个吃字符串的 Parser 在 Haskell 中可以写成这样（用了 mtl）：

```haskell
newtype Parser a = P
  { runP :: ExceptT ParseError (State String) a
  } deriving (Functor, Applicative, Monad, MonadError ParseError, MonadState String)
```

其实是一个经过 `ExceptT` 单子变换过的 State。解析过程中的字符串作为状态单子的上下文，`ExceptT` 则是在外面套了一个 `Either`，表示解析中遇到错误，而解析器真正运行的值是 `a`。在 Haskell 中有 `GeneralisedNewtypeDeriving` 这种作弊的东西，这样一个 Parser 数据类型就定义并实现好了，加上 `satisfy` 之类的东西就可以用了。在 Kotlin 中可以仿照这个思路实现这个解析器，并用 `sealed class` 来代替 `ADT`，`interface` 来代替 `newtype`。

## 实现

### Either

先来搞一个 Either 单子：

```kotlin
sealed class Either<L, R> {

    abstract infix fun <T> map(f: (R) -> T): Either<L, T>

    abstract infix fun <T> ap(f: (Either<L, (R) -> T>)): Either<L, T>

    abstract infix fun <T> bind(f: (R) -> Either<L, T>): Either<L, T>

    override fun toString(): String =
        when (this) {
            is Left -> "Left $value"
            is Right -> "Right $value"
        }
}

class Left<L, R>(val value: L) : Either<L, R>() {

    override fun <T> map(f: (R) -> T): Either<L, T> = Left(value)

    override fun <T> ap(f: Either<L, (R) -> T>): Either<L, T> =
        Left(value)

    override fun <T> bind(f: (R) -> Either<L, T>): Either<L, T> =
        Left(value)

}

class Right<L, R>(val value: R) : Either<L, R>() {

    override fun <T> map(f: (R) -> T): Either<L, T> = Right(f(value))

    override fun <T> ap(f: Either<L, (R) -> T>): Either<L, T> = f.map { it(value) }

    override fun <T> bind(f: (R) -> Either<L, T>): Either<L, T> = f(value)

}


fun <L, R> Either<L, Either<L, R>>.join(): Either<L, R> = bind(::id)
```

仅仅写了作为 `Functor`、`Applicative` 和 `Monad` 的三个函数。（甚至没有 `pure` & `return`，问题不大。

### State

```kotlin
interface State<T, R> {

    fun runState(state: T): Pair<R, T>

    infix fun <S> map(f: (R) -> S): State<T, S> = state {
        val (r, s) = runState(it)
        f(r) to s
    }

    infix fun <S> ap(f: State<T, (R) -> S>): State<T, S> =
        state {
            val (a, s) = runState(it)
            val (frs, s1) = f.runState(s)
            frs(a) to s1
        }

    infix fun <S> bind(f: (R) -> State<T, S>): State<T, S> =
        state {
            val (a, s) = runState(it)
            f(a).runState(s)
        }


    companion object {
        fun <T> get(): State<T, T> = state { it to it }

        fun <T> put(a: T): State<T, Unit> = state { Unit to a }
}

fun <T, R> State<T, State<T, R>>.join(): State<T, R> = bind(::id)


fun <T, R> state(f: (T) -> Pair<R, T>): State<T, R> = object :
    State<T, R> {
    override fun runState(state: T): Pair<R, T> = f(state)
}
```

State 里装的东西应该是 `s -> (a, s)`，一个函数，接收状态 `s` 返回变换过的状态及这个操作的值。这里保留了二元组的写法。除了 `interface` 这种写法外还可以用普通的类作 wrapper，其实没多大区别，由于不可变都要创建一大堆对象。BTW，9102 年了 Kotlin 还不支持自己接口的 **SAM 转换**，搞得自己还要写一个 `state` 函数。

### Parser

Kotlin 中没法实现高阶类型，像 `Functor` 这种类型类是写不出来的，并且单子变换也写不出来。所以干脆就不写 `ExceptT` 了，反正也没别处用。

```kotlin
interface Parser<T, R> {
    fun runParser(): State<T, Either<ParserException, R>>
}
```

`T` 是状态的类型，`R` 是单子中返回值的类型。

可以给它实现那三个东西：

```kotlin
infix fun <U> map(f: (R) -> U) = parser {
    runParser() map { it map { r -> f(r) } }
}

infix fun <U> ap(f: Parser<T, (R) -> U>) =
    map { r -> f.map { it(r) } }.join()


infix fun <U> bind(f: (R) -> Parser<T, U>) = parser {
    runParser().bind { e ->
        when (e) {
            is Right ->
                f(e.value).runParser()
            is Left  ->
                state {
                    Left<ParserException, U>(
                        e.value
                    ) to it
                }
        }
    }
}
```

如果有 `ExceptT` 的话这里的 `bind` 也是不用拆开写的。还需要 `MonadState`、`MonadError` 的一些函数：

```kotlin
fun <T> get(): Parser<T, T> =
    parser {
        state<T, Either<ParserException, T>> {
            Right<ParserException, T>(
                it
            ) to it
        }
    }

fun <T> put(a: T): Parser<T, Unit> =
    parser {
        state<T, Either<ParserException, Unit>> {
            Right<ParserException, Unit>(
                Unit
            ) to a
        }
    }

fun <T, R> returnM(a: R): Parser<T, R> = parser {
    state<T, Either<ParserException, R>> {
        suc(a) to it
    }
}

fun <T, R> throwError(a: ParserException): Parser<T, R> =
    parser {
        state<T, Either<ParserException, R>> {
            err<R>(
                a
            ) to it
        }
    }

fun <T, R> catchError(p: Parser<T, R>, f: (ParserException) -> Parser<T, R>): Parser<T, R> = parser {
    State.get<T>().bind<Either<ParserException, R>> { s ->
        val (e, s1) = p.runParser().runState(s)
        when (e) {
            is Left  -> state { f(e.value).runParser().runState(s) }
            is Right -> state { suc(e.value) to s1 }
        }
    }
}
```

`returnM` 就是单子中的 `return`，把返回值升格为返回这个值的解析器。`suc` 和 `err` 分别是装在 `Left` 的异常以及装在 `Right` 里的返回值。

`ParserException`：

```kotlin
abstract class ParserException : RuntimeException() {
    override fun toString(): String = javaClass.simpleName
}

class UnexpectedChar(val char: Char) : ParserException() {
    override fun toString(): String = "${super.toString()} \"$char\""
}

object UnexpectedEOF : ParserException()
object Unknown : ParserException()
```

为了保证扩展性，用了抽象类表示常见的两个错误以及其他错误。

### 组合子

我们制作的都是以 `String` 作为输入即状态量的解析器，为了方便，可以先定义个类型别名：

```kotlin
typealias ParserS<R> = Parser<String, R>
```

还可以为 Parser 重载一个 `invoke` 运算符：

```kotlin
operator fun <T, R> Parser<T, R>.invoke(a: T): Either<ParserException, Pair<R, T>> {
    val (e, rest) = runParser().runState(a)
    return when (e) {
        is Left  -> Left(e.value)
        is Right -> Right(e.value to rest)
    }
}
```

当解析成功时将剩余的字符串和解析结果合成一个二元组，失败则返回错误。

万事都从 `satisfy` 开始（`satisfy :: (Char -> Boolean) -> Parser Char`）：

```kotlin
fun satisfy(f: (Char) -> Boolean): ParserS<Char> =
    Parser.get<String>() bind { s ->
        when {
            s.isEmpty()  -> Parser.throwError(UnexpectedEOF)
            f(s.first()) -> Parser.put(s.takeLast(s.length - 1)) bind { Parser.returnM<String, Char>(s.first()) }
            else         -> Parser.throwError(UnexpectedChar(s.first()))
        }
    }
```

该函数接收一个接受 `Char` 返回布尔的函数，表示是这个字符否匹配。如果现在 State 单子中的字符串已经空了，说明先前已经它已经被吃完了，而现在还有人想吃肯定是不行的。如果匹配的话就把它（字符）拿出来作为返回值，并且把去掉它字符串装回 State 单子向下传递。至此，解析单字符已经实现了，可以试一试：

```kotlin
val parser = satisfy { it=='C' }
parser("CDE") //Right ('C', "DE")
parser("DDE") //Left UnexpectedChar('D')
```

如果要重复多次匹配这个字符呢？那需要 `many` 和 `some`。在 Haskell 中它们是定义在 `Alternative` 中的，类型都是 `f a -> f [a]`。即重复这个单子运算任意次或至少一次，并将它们连接起来成为一个新的运算。在 Kotlin 中也可以写出类似实现：

```kotlin
fun <T, R> Parser<T, R>.many(): Parser<T, List<R>> = some() or Parser.returnM(listOf())

fun <T, R> Parser<T, R>.some(): Parser<T, List<R>> =
    bind { many() bind { rest -> Parser.returnM<T, List<R>>(listOf(it) + rest) } }
```

可以写一个解析符号的：

```kotlin
val sign = (char('+') + char('-')).some().map { it.map { s -> "${s}1".toInt() }.fold(1, Int::times) }
sign("+-") // Right (-1, "")
sign("+--") // Right (1, "")
sign("+--+-") // Right (-1, "")
```

