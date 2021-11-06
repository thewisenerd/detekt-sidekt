package io.github.thewisenerd.linters.sidekt
import io.github.thewisenerd.linters.sidekt.rules.ResourceApsecOnboarded
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class ResourceApsecOnboardedTest {

    companion object {
        private val blockingJerseyMethod = ResourceApsecOnboarded::class.java.simpleName

        private fun ensureJerseyMethodParameterDefaultValueFindings(
            findings: List<Finding>,
            requiredFindings: List<SourceLocation>
        ) = TestUtils.ensureFindings(blockingJerseyMethod, findings, requiredFindings)
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
    private val subject = ResourceApsecOnboarded(testConfig)


    @Test
    fun testApsecResource() {
        val code = TestUtils.readFile("TestApsecResource.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureJerseyMethodParameterDefaultValueFindings(
            findings, listOf(
                SourceLocation(14, 5),
                SourceLocation(19, 5)
            )
        )
    }
}
