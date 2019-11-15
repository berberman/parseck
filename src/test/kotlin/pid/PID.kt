package pid

import cn.berberman.parseck.*
import cn.berberman.parseck.dt.Left
import cn.berberman.parseck.parser.*
import org.mechdancer.remote.presets.RemoteHub
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.remote.resources.Command
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
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
val operation = listOf("kp", "ki", "kd", "ia", "da").sortedByDescending { it.length }.map { string(it) }.choice()
val arg = until(" ") bind { returnP { jsEngine.eval(it).toString().toDouble() } }
val expr = operation bind { op ->
    lexeme {
        arg bind { num ->
            returnP {
                pid = when (op) {
                    "kp"  -> pid.copy(k = num)
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

val remote = remoteHub("pwh").also {
    it.openAllNetworks()
}

fun RemoteHub.sendPID(pid: PID, id: Int, reset: Boolean = true) =
        broadcast(object : Command {
            override val id: Byte = 9
        }, ByteArrayOutputStream().let {
            DataOutputStream(it).run {
                with(pid) {
                    writeInt(id)
                    writeDouble(k)
                    writeDouble(ki)
                    writeDouble(kd)
                    writeDouble(integrateArea)
                    writeDouble(deadArea)
                    writeBoolean(reset)
                }
                it.toByteArray()
            }
        })

fun RemoteHub.sendDouble(double: Double, id: Int) =
        broadcast(object : Command {
            override val id: Byte = 32
        }, ByteArrayOutputStream().let {
            DataOutputStream(it).run {
                writeInt(id)
                writeDouble(double)
                it.toByteArray()
            }
        })
fun RemoteHub.sendPosition(x:Double, y:Double,w:Double) =
        broadcast(object : Command {
            override val id: Byte = 10
        }, ByteArrayOutputStream().let {
            DataOutputStream(it).run {
                writeDouble(x)
                writeDouble(y)
                writeDouble(w)
                it.toByteArray()
            }
        })

val parser = string("pid") * space * expr.some()

fun main() {
    while (true) {
        //pid.getPid ki ... k ... (at least one term)
        parser(readLine()!!)/*.let { if (it is Left) println(it.value) }*/
        println(pid)
        remote.sendPID(pid,0)
    }
}