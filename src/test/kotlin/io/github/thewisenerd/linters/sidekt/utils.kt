package io.github.thewisenerd.linters.sidekt

import io.github.detekt.test.utils.createEnvironment
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SourceLocation
import java.io.InputStream

object TestUtils {
    fun ensureFindings(id: String, findings: List<Finding>, requiredFindings: List<SourceLocation>) {
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

    private val wrapper = createEnvironment()
    val env = wrapper.env

    fun readFile(filename: String): String {
        val resource: InputStream? = this.javaClass.classLoader.getResourceAsStream(filename)
        return resource?.let { String(it.readBytes()) } ?: error("resource $filename not found")
    }
}