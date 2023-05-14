package io.github.thewisenerd.linters.sidekt

import io.github.thewisenerd.linters.sidekt.rules.BlockingCallContext
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class BlockingCallContextTest {
    companion object {
        private val blockingCallContextClassName = BlockingCallContext::class.java.simpleName

        private fun ensureBlockingCallContextFindings(findings: List<Finding>, requiredFindings: List<SourceLocation>) =
            TestUtils.ensureFindings(blockingCallContextClassName, findings, requiredFindings)

        private fun ensureReclaimableBlockingCallContextFindings(
            findings: List<Finding>,
            requiredFindings: List<SourceLocation>
        ) =
            TestUtils.ensureFindings("${blockingCallContextClassName}-Reclaimable", findings, requiredFindings)
    }

    private val testConfig = object : Config {
        override fun subConfig(key: String): Config = this

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> valueOrNull(key: String): T? {
            return when (key) {
                "active" -> true as? T
                "debug" -> "stderr" as? T
                "blockingMethodAnnotations" -> arrayListOf(
                    "BlockingCall",
                    "RegularBlockingCall",
                    "ReclaimableBlockingCall"
                ) as? T

                "blockingClassAnnotations" -> arrayListOf(
                    "BlockingClass"
                ) as? T
                "blockingClassFqNames" -> arrayListOf(
                    "SomeRandomDao"
                ) as? T

                "blockingMethodFqNames" -> arrayListOf("Test03.foo", "kotlinx.coroutines.runBlocking") as? T
                "reclaimableMethodAnnotations" -> arrayListOf("ReclaimableBlockingCall") as? T
                "ioDispatcherFqNames" -> arrayListOf("CustomDispatchers.DB") as? T
                else -> null
            }
        }
    }

    private val subject = BlockingCallContext(testConfig)

    @Test
    fun simple01() {
        val code = TestUtils.readFile("simple01.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
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
        val code = TestUtils.readFile("simple02.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureBlockingCallContextFindings(
            findings, listOf(
                SourceLocation(13, 23),
                SourceLocation(15, 23)
            )
        )
    }

    @Test
    fun simple03() {
        val code = TestUtils.readFile("simple03.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureBlockingCallContextFindings(
            findings, listOf(
                SourceLocation(8, 11),
                SourceLocation(12, 11),
                SourceLocation(16, 28),
                SourceLocation(20, 16)
            )
        )
    }

    @Test
    fun simple04() {
        val code = TestUtils.readFile("simple04.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureReclaimableBlockingCallContextFindings(
            findings, listOf(
                SourceLocation(37, 22),
                SourceLocation(43, 26)
            )
        )
    }

    @Test
    fun simple06() {
        val code = TestUtils.readFile("simple06.kt")
        val base01 = 146
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureBlockingCallContextFindings(
            findings, listOf(
                SourceLocation(base01 + 0, 20),
                // SourceLocation(base01+1, 20),
                SourceLocation(base01 + 2, 20),
                SourceLocation(base01 + 3, 20),

                SourceLocation(base01 + 5, 14),
                SourceLocation(base01 + 6, 14),
                SourceLocation(base01 + 7, 14),
                SourceLocation(base01 + 8, 14),

                SourceLocation(base01 + 10, 20),
                SourceLocation(base01 + 11, 20),
                SourceLocation(base01 + 12, 20),

                SourceLocation(base01 + 14, 26),

                // SourceLocation(base01 + 16, 14)

                SourceLocation(base01 + 18, 14),
                // SourceLocation(base01 + 19, 14),
                SourceLocation(base01 + 20, 14),
                SourceLocation(base01 + 21, 14)
            )
        )
    }

    @Test
    fun simple07() {
        val code = TestUtils.readFile("simple07.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureBlockingCallContextFindings(findings, listOf(
            SourceLocation(11, 13)
        ))
    }
}
