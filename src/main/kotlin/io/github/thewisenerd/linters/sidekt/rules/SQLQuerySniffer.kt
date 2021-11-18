package io.github.thewisenerd.linters.sidekt.rules

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

class SQLQuerySniffer(config: Config): Rule(config) {

    private val sniffSQLAnnotation = listOf("SqlUpdate", "SqlCreate")
    private val stringTemplateMarker = "@UseStringTemplate3StatementLocator"

    private var scanResourceFiles = false
    // TODO: Handle scanning resource files for detecting the queries

    override val issue = Issue(
        SQLQuerySniffer::class.java.simpleName,
        Severity.Performance,
        "SQL Query sniffed",
        Debt.TWENTY_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (klass.isInterface()) {
            klass.annotationEntries.any {
                it.originalElement.text == stringTemplateMarker
            }.takeIf {
                it
            }?.let {
                scanResourceFiles = true
            }
        } else {
            // Dont process non-interfaces
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        function.annotationEntries.forEach {
            val annotationValue = it.shortName.toString()
            if (sniffSQLAnnotation.contains(annotationValue)) {
                val sqlQuery = it.originalElement.text.preProcessAnnotation(annotationValue)
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.Companion.from(function),
                        message = "SQL Query Sniffed $annotationValue with value $sqlQuery"
                    )
                )

            }
        }
    }

    private fun String.preProcessAnnotation(queryType: String): String {
        val queryTypeExtractor = """@$queryType""".toRegex()
        return queryTypeExtractor.split(this)
            .last()
            .trim()
            .split("+").joinToString(" ") {
                it
                    .trim()
                    .replace("\"", "")
                    .replace("\\n", "")
            }

    }
}
