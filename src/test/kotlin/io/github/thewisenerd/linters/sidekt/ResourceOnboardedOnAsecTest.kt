package io.github.thewisenerd.linters.sidekt
import io.github.thewisenerd.linters.sidekt.rules.ResourceOnboardedOnAsec
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class ResourceOnboardedOnAsecTest {

    companion object {
        private val resourceOnboardedOnAsec = ResourceOnboardedOnAsec::class.java.simpleName

        private fun ensureResourceOnboardedOnASECFindings(
            findings: List<Finding>,
            requiredFindings: List<SourceLocation>
        ) = TestUtils.ensureFindings(resourceOnboardedOnAsec, findings, requiredFindings)
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
    private val subject = ResourceOnboardedOnAsec(testConfig)


    @Test
    fun testAsecResource() {
        val code = TestUtils.readFile("TestAsecResource.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureResourceOnboardedOnASECFindings(
            findings, listOf(
                SourceLocation(14, 5),
                SourceLocation(19, 5)
            )
        )
    }
}
