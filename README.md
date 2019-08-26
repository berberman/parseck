# parseck

使用 Kotlin 实现的简易 Parser Combinator，大部分 Parsec 的功能都没有实现。

## 简介

对于一个吃字符串的 Parser 在 Haskell 中可以写成这样（用了 mtl）：

```haskell
newtype Parser a = P
  { runP :: ExceptT ParseError (State String) a
  } deriving (Functor, Applicative, Monad, MonadError ParseError, MonadState String)
```

其实是一个经过 `ExceptT` 单子变换过的 State。解析过程中的字符串作为状态单子的上下文，`ExceptT` 则是在外面套了一个 `Either`，表示解析中遇到错误，而解析器真正运行的值是 `a`。在 Haskell 中有 **GeneralisedNewtypeDeriving** 这种作弊的东西，这样一个 Parser 数据类型就定义并实现好了，加上 `satisfy` 之类的东西就可以用了。在 Kotlin 中可以仿照这个思路实现这个解析器，并用 *sealed class* 来代替 *ADT*，*functional interface* 来代替 *newtype*。

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

`suc` 和 `err` 代码没有贴在这里，它们分别是将异常装在 `Left` 里以及将返回值装在 `Right` 里。`returnM` 就是单子中的 `return`，把返回值升格为解析器，也就是在不改变单子上下文条件下直接返回这个值。`catchErorr` 为尝试运行解析器 `p`，如果遇到异常则不消耗状态，将运算转给返回另一个解析器的函数 `f`。

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

如果要重复多次匹配这个字符呢？那需要 `many` 和 `some`。在 Haskell 中它们是定义在 `Alternative` 中的，类型都是 `f a -> f [a]`，即重复这个单子运算任意次或至少一次，并将它们连接起来成为一个新的运算。在 Kotlin 中也可以写出类似实现：

```kotlin
fun <T, R> Parser<T, R>.many(): Parser<T, List<R>> = some() or Parser.returnM(listOf())

fun <T, R> Parser<T, R>.some(): Parser<T, List<R>> =
    bind { many() bind { rest -> Parser.returnM<T, List<R>>(listOf(it) + rest) } }
```

`or` 的代码也没贴，也就是 `<|>`。`p1 or p2` 意为先尝试使用 `p1` 解析，若失败则换 `p2` 继续下去。

再来一波运算符重载：

```kotlin
operator fun <T, R> Parser<T, R>.plus(p: Parser<T, R>) = or(p)

operator fun <T, R1, R2> Parser<T, R1>.times(p: Parser<T, R2>) = bind { p }
```

`p1 + p2` 为两个解析器进行类似于*或* 的组合，`p1 * p2` 则是类似于*与* 的组合。

### 符号

来看个例子——解析数的符号：

```kotlin
val sign = (char('+') + char('-')).some().map { it.map { s -> "${s}1".toInt() }.fold(1, Int::times) }
sign("+-")     // Right (-1, "")
sign("+--")    // Right (1, "")
sign("+--+-")  // Right (-1, "")
```

代码非常好理解：尝试吃掉 `-` 或 `+` 字符至少一次，后将它们转为正负 1 相乘得到最终结果。`(char('+') + char('-')).some()` 此表达式的返回值是 `ParserS<List<Char>>`，需要使用 `map` 来对函子中值进行映射，也就是先变成 `List<Int>` 再使用 `fold` 合并。

### 实数

实数的模样有两种情况：

* *digit\+*
* *digit+.digit\**

不管怎样，这里先需要有一个吃字符串得到数字的解析器：

```kotlin
val digit = satisfy { it.isDigit() } map { it.toString().toInt() }
```

异常简单，如果一个字符是数字，就把它吃了并映射为 `Int`。

再来一波解析整数：

```kotlin
val int = digit.some() map { it.joinToString(separator = "").toInt() }
```

整数就是许多数字。

有了它们，就可以写实数解析器了：

```kotlin
val real =
    int.bind { a ->
        char('.') bind { dot ->
            int bind { b ->
                Parser.returnM<String, Double>("$a$dot$b".toDouble())
            }
        }
    } or int.map(
        Int::toDouble
    )
```

可以看到 `or`，意味着分了上述的两种情况。第一种情况是存在小数部分的，第二种则是整数。先尝试吃掉一个整数，再吃掉一个小数点，再尝试吃掉一个整数，很容易理解。如果是整数的话直接用之前的整数解析器就好了，不过要把它的类型从 `ParserS<Int>` 映射为 `ParserS<Double>` 来和上一种情况匹配。在第一种情况下仅仅匹配三段就已经两层 lambda 嵌套了，有点像回调地狱。在 Haskell 等语言中有 **do notation** 这类东西来改善情况：

```haskell
parseReal = do
	num1 <- int
	dot  <- char "."
	num2 <- int
	return . read $ (show num1) ++ dot ++ (show num2)
```

而 Kotlin 中的**协程**所模拟的控制流也可以。但由于类型的限制，写不出一些灵活、可以泛化使用的函数，所以会非常僵硬并且效果不好。 

## 用例

### Console Parser

之前我们在 JVM 平台实现了一个组播通信协议 [remote](https://github.com/MechDancer/framework/tree/master/remote) 用于机器人参数网络调试。当然，要调试的参数都是运行期受实时系统影响的，所以需要一个前端界面来发送参数，为了简便就选择了控制台。解析控制台有许多现成的框架，自己造一个简易版符合自己的使用习惯并没有很复杂。先前的版本在[这里](https://github.com/MechDancer/consoleparser)，是一个完整的项目，核心部分使用了 [确定型有穷自动机（DFA）](https://www.mechdancer.org/2018/11/12/programming/scanning-digit-comment-DFA/)。而使用组合子也可以简便地实现这个解析任务。先来看看效果：

```kotlin
val rules = Rules()
rules
    .addRule("print @word") {
        println(word[0])
    }
    .addRule("@num / @num") {
        println(num[0] / num[1])
    }

val parser = rules.buildParser()

parser("print hi") // 打印出 "hi"
parser("9/3")	   // 打印出 "3.0"
```

和没有绑定名字的**模式匹配**很像。规则集接收规则和符合规则时的操作（此时能访问 `ResultContext`），进行一些处理后合成一个解析器 `ParserS<Unit>`。这里的思路是先解析 Rule，再利用其生成 Parser。先定义规则语法树 ADT：

```kotlin
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

}

typealias Rule = List<RuleAST>
```

即便叫了 `AST`，但这根本不是一颗树。。总之，现在的任务是造出这样一个 Parser：接受诸如 `"print @word"`、`"@num / @num"` 的规则字符串返回 `Rule` 的解析器。规则中只有两种类别的元素：

* `Num` => `@num`、`Word` => `@word` 、`Sign` => `@sign` 这三类表示要从输入中解析并取出相应元素的标志
* `Str(...)` => `.*` 在规则中写死的字符串，对应要在输入中必须匹配但不作为结果的

```kotlin
val ruleParser = ...
ruleParser("print @word") // Right ([Str("print"), Word], "")
ruleParser("@num / @num") // Right ([Num, Str("/"), Num], "")
```

就是一个解析第一中情况要取出东西的解析器和第二种情况解析任意字符直到 `"\n"` 的解析器组合，代码在仓库里，就不贴在这了。所有空格都去掉了，并且匹配输入时也忽略空格。下一步任务是将单条规则 `Rule` 转化为 `ParserS<ResultContext>`：

```kotlin
data class ResultContext(val num: List<Double>, val word: List<String>, val sign:List<Int>)
```

`Rule.toParser()` 的代码也请见仓库，只要把规则中的每项映射为对应的解析器，再连接起来即可。在连接的过程中构建 `ResultContext` 并放入结果。现在有了 `Rule -> ParserS<ResultContext>`，还缺少 `[Rule] -> ParserS<ResultContext>`。规则与规则间是可能产生联系的，若不是这样，就可以直接将 `[Rule]` 映射为 `[ParserS<ResultContext>]` 再进行 `choice`，也就是 `asum`（定义在 `Data.Foldable`），换句话说即把它们用 `or`（`<|>`） 连接。考虑以下情况：

```kotlin
val ruleA = "print @num"
val ruleB = "print @word"
```

`@word` 是任意字符串，`@num` 是数，显然数此时也是字符串，也就是说 `@word` **覆盖** 了 `@num`。 

