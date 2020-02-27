package cn.berberman.parseck.samples.pid

import cn.berberman.parseck.parser.*
import cn.berberman.parseck.simple.char
import cn.berberman.parseck.simple.returnP
import cn.berberman.parseck.simple.string
import cn.berberman.parseck.token.lexeme
import cn.berberman.parseck.token.natural
import cn.berberman.parseck.samples.consoleparser.until
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

var pidS = Array(3){PID(.0, .0, .0, .0, .0)}

var i = 0

fun currentPID()= pidS[i]

val jsEngine = ScriptEngineManager().getEngineByName("nashorn")
val operation = listOf("kp", "ki", "kd", "ia", "da").sortedByDescending { it.length }.map { string(it) }.choice()
val arg = until(" ") flatMap  { returnP { jsEngine.eval(it).toString().toDouble() } }
val expr = operation flatMap  { op ->
    lexeme (
        arg flatMap  { num ->
            returnP {
                pidS[i] = when (op) {
                    "kp" -> currentPID().copy(k = num)
                    "ki" -> currentPID().copy(ki = num)
                    "kd" -> currentPID().copy(kd = num)
                    "ia" -> currentPID().copy(integrateArea = num)
                    "da" -> currentPID().copy(deadArea = num)
                    else -> throw Unknown
                }
            }
        }
    )
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

fun RemoteHub.sendPosition(x: Double, y: Double, w: Double) =
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

val parser = natural flatMap  { i = it; char(' ') * expr.some() }

fun main() {
    while (true) {
        // ki ... k ... (at least one term)
        parser(readLine()!!)/*.let { if (it is Left) println(it.value) }*/
        println(currentPID())
        remote.sendPID(currentPID(), i)
    }
}