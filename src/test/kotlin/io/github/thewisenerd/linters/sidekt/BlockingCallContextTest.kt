package io.github.thewisenerd.linters.sidekt

import io.github.detekt.test.utils.createEnvironment
import io.github.thewisenerd.linters.sidekt.rules.BlockingCallContext
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test
import java.io.InputStream

class BlockingCallContextTest {
    companion object {
        private fun ensureFindings(id: String, findings: List<Finding>, requiredFindings: List<SourceLocation>) {
            val allFindings =
                findings.filter { it.id == id }.map { it.location.source }

            val missing = requiredFindings.minus(allFindings)
            val extra = allFindings.minus(requiredFindings)

            assert(missing.isEmpty() && extra.isEmpty()) {
                "findings mismatch\n" +
                        "  missing = $missing\n" +
                        "  extra   = $extra"
            }

            assert(extra.isEmpty()) {
                "got extra findings $extra"
            }
        }

        private fun ensureBlockingCallContextFindings(findings: List<Finding>, requiredFindings: List<SourceLocation>) =
            ensureFindings("BlockingCallContext", findings, requiredFindings)

        private fun ensureReclaimableBlockingCallContextFindings(findings: List<Finding>, requiredFindings: List<SourceLocation>) =
            ensureFindings("ReclaimableBlockingCallContext", findings, requiredFindings)
    }

    private val wrapper = createEnvironment()
    private val env = wrapper.env
    val testConfig = object : Config {
        override fun subConfig(key: String): Config = this

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> valueOrNull(key: String): T? {
            return when (key) {
                "active" -> true as? T
                "debug" -> "stderr" as? T
                "blockingMethodAnnotations" -> arrayListOf("BlockingCall", "RegularBlockingCall", "ReclaimableBlockingCall") as? T
                "blockingMethodFqNames" -> arrayListOf("Test03.foo", "kotlinx.coroutines.runBlocking") as? T
                "reclaimableMethodAnnotations" -> arrayListOf("ReclaimableBlockingCall") as? T
                "ioDispatcherFqNames" -> arrayListOf("CustomDispatchers.DB") as? T
                else -> null
            }
        }
    }

    private val subject = BlockingCallContext(testConfig)
    private fun readFile(filename: String): String {
        val resource: InputStream? = this.javaClass.classLoader.getResourceAsStream(filename)
        return resource?.let { String(it.readBytes()) } ?: error("resource $filename not found")
    }

    @Test
    fun simple01() {
        val code = readFile("simple01.kt")
        val findings = subject.compileAndLintWithContext(env, code)
        ensureBlockingCallContextFindings(
            findings, listOf(
                SourceLocation(40, 21),
                SourceLocation(43, 15),
                SourceLocation(65, 15),
                SourceLocation(69, 15),
                SourceLocation(69, 15),
                SourceLocation(75, 15)
            ).minus(
                listOf( // kotlinx-coroutines-core dep in classpath
                    SourceLocation(43, 15),
                    SourceLocation(75, 15)
                )
            )
        )
    }

    @Test
    fun simple02() {
        val code = readFile("simple02.kt")
        val findings = subject.compileAndLintWithContext(env, code)
        ensureBlockingCallContextFindings(
            findings, listOf(
                SourceLocation(13, 23),
                SourceLocation(15, 23)
            )
        )
    }

    @Test
    fun simple03() {
        val code = readFile("simple03.kt")
        val findings = subject.compileAndLintWithContext(env, code)
        ensureBlockingCallContextFindings(
            findings, listOf(
                SourceLocation(8, 11),
                SourceLocation(12, 11),
                SourceLocation(16, 28)
            )
        )
    }

    @Test
    fun simple04() {
        val code = readFile("simple04.kt")
        val findings = subject.compileAndLintWithContext(env, code)
        ensureReclaimableBlockingCallContextFindings(
            findings, listOf(
                SourceLocation(37, 22),
                SourceLocation(43, 26)
            )
        )
    }
}