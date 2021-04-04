package io.github.thewisenerd.linters.sidekt

import io.github.detekt.test.utils.createEnvironment
import io.github.thewisenerd.linters.sidekt.rules.BlockingCallContext
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test
import java.io.InputStream

class BlockingCallContextTest {
    private val wrapper = createEnvironment()
    private val env = wrapper.env
    private val subject = BlockingCallContext(Config.empty)
    private fun readFile(filename: String): String {
        val resource: InputStream? = this.javaClass.classLoader.getResourceAsStream(filename)
        return resource?.let { String(it.readBytes()) } ?: error("resource $filename not found")
    }

    @Test
    fun simple01() {
        val code = readFile("simple01.kt")
        val findings = subject.compileAndLintWithContext(env, code)
        findings.forEach {
            println("$it")
        }
    }
}