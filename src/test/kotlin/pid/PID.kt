package pid

import cn.berberman.parseck.*
import cn.berberman.parseck.dt.Left
import cn.berberman.parseck.parser.*
import javax.script.ScriptEngineManager

data class PID(
    val k: Double,
    val ki: Double,
    val kd: Double,
    val integrateArea: Double,
    val deadArea: Double
)

var pid = PID(.0, .0, .0, .0, .0)

val jsEngine = ScriptEngineManager().getEngineByName("nashorn")
val operation = listOf("k", "ki", "kd", "ia", "da").sortedByDescending { it.length }.map { string(it) }.choice()
val arg = until(" ") bind { returnP { jsEngine.eval(it).toString().toDouble() } }
val expr = operation bind { op ->
    lexeme {
        arg bind { num ->
            returnP {
                pid = when (op) {
                    "k"  -> pid.copy(k = num)
                    "ki" -> pid.copy(ki = num)
                    "kd" -> pid.copy(kd = num)
                    "ia" -> pid.copy(integrateArea = num)
                    "da" -> pid.copy(deadArea = num)
                    else -> throw Unknown
                }
            }
        }
    }
}
val parser = string("pid") * space * expr.some()

fun main() {
    while (true) {
        //pid.getPid ki ... k ... (at least one term)
        parser(readLine()!!).let { if (it is Left) println(it.value) }
        println(pid)
    }
}