package io.github.thewisenerd.linters.sidekt

import io.github.thewisenerd.linters.sidekt.rules.ImageQuality
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class ImageQualityTest {
    companion object {
        private val imageQuality = ImageQuality::class.java.simpleName

        private fun ensureImageQualityFindings(
            findings: List<Finding>,
            requiredFindings: List<SourceLocation>
        ) = TestUtils.ensureFindings(imageQuality, findings, requiredFindings)
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
    private val subject = ImageQuality(testConfig)

    @Test
    fun testImageQuality() {
        val code = TestUtils.readFile("ImageQualityChecks.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureImageQualityFindings(
            findings, listOf(
                SourceLocation(10, 17),
                SourceLocation(17, 17),
                SourceLocation(25, 22),
                SourceLocation(26, 22),
                SourceLocation(27, 22),
                SourceLocation(31, 25),
                SourceLocation(32, 25)
            )
        )
    }
}