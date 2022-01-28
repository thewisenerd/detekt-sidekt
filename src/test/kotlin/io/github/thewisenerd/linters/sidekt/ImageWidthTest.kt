package io.github.thewisenerd.linters.sidekt

import io.github.thewisenerd.linters.sidekt.rules.ImageWidth
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.Test

class ImageWidthTest {
    companion object {
        private val imageWidth = ImageWidth::class.java.simpleName

        private fun ensureImageWidthFindings(
            findings: List<Finding>,
            requiredFindings: List<SourceLocation>
        ) = TestUtils.ensureFindings(imageWidth, findings, requiredFindings)
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
    private val subject = ImageWidth(testConfig)

    @Test
    fun testImageWidth() {
        val code = TestUtils.readFile("ImageWidthChecks.kt")
        val findings = subject.compileAndLintWithContext(TestUtils.env, code)
        ensureImageWidthFindings(
            findings, listOf(
                SourceLocation(10, 17),
                SourceLocation(17, 17),
                SourceLocation(26, 22),
                SourceLocation(27, 22),
                SourceLocation(31, 23),
                SourceLocation(32, 23)
            )
        )
    }
}