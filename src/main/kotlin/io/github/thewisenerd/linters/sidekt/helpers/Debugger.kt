package io.github.thewisenerd.linters.sidekt.helpers

import java.io.File
import java.io.OutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class Debugger(private val ref: Int, private val outputStream: OutputStream?) {
    companion object {
        private val ctr = AtomicInteger()

        fun getOutputStreamForDebugger(debug: String): OutputStream? {
            val fallback = when (debug) {
                "stdout" -> System.out
                else -> null
            }
            if (fallback != null) return fallback

            val dir = File(debug)
            return if (dir.canWrite() && dir.isDirectory) {
                val uuid: UUID = UUID.randomUUID()
                val name = "${System.currentTimeMillis()}-$uuid"

                // TODO: add shutdown hook if needed
                val file = File(debug + File.separator + name)
                file.outputStream()
            } else null
        }

        fun make(outputStream: OutputStream?): Debugger {
            val ref = ctr.incrementAndGet()
            return Debugger(ref, outputStream)
        }
    }

    fun i(str: String) {
        val msg = "[$ref] $str\n"

        when {
            outputStream == System.out -> {
                print(msg)
            }
            outputStream != null -> {
                outputStream.write(msg.toByteArray())
                outputStream.flush()
            }
            else -> {
                // nothing to do ,_,
            }
        }
    }

    fun x(str: String) {
        // do nothing
    }
}