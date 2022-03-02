package io.github.thewisenerd.linters.sidekt

import io.github.thewisenerd.linters.sidekt.rules.JerseyMethodParameterDefaultValue
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class JerseyTest {
    companion object {
        private val jerseyMethodParameterDefaultValueClassName = JerseyMethodParameterDefaultValue::class.java.simpleName

        private fun ensureJerseyMethodParameterDefaultValueFindings(findings: List<Finding>, requiredFindings: List<SourceLocation>) =
            TestUtils.ensureFindings(jerseyMethodParameterDefaultValueClassName, findings, requiredFindings)
    }

    private val testConfig = object : Config {
        override fun subConfig(key: String): Config = this

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> valueOrNull(key: String): T? {
            return when (key) {
                "active" -> true as? T
                "debug" -> "stderr" as? T
                else -> null
            }
        }
    }
    private val subject = JerseyMethodParameterDefaultValue(testConfig)

    @Test
    fun simple05() {
        val code = TestUtils.readFile("simple05.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureJerseyMethodParameterDefaultValueFindings(findings, listOf(
            SourceLocation(15, 10),
            SourceLocation(21, 10)
        ))
    }

}