package io.github.thewisenerd.linters.sidekt
import io.github.thewisenerd.linters.sidekt.rules.ResourceOnboardedOnApsec
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class ResourceOnboardedOnApsecTest {

    companion object {
        private val blockingJerseyMethod = ResourceOnboardedOnApsec::class.java.simpleName

        private fun ensureResourceOnboardedOnAPSECFindings(
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
    private val subject = ResourceOnboardedOnApsec(testConfig)


    @Test
    fun testApsecResource() {
        val code = TestUtils.readFile("TestApsecResource.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureResourceOnboardedOnAPSECFindings(
            findings, listOf(
                SourceLocation(14, 5),
                SourceLocation(19, 5)
            )
        )
    }
}
