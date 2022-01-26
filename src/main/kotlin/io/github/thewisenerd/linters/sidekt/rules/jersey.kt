package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.KtNamedFunction

private object JerseyIssueHolder {
    val jerseyMethodParameterDefaultValue = Issue(
        JerseyMethodParameterDefaultValue::class.java.simpleName,
        Severity.Defect,
        "jersey resource method has default parameter value",
        Debt.FIVE_MINS
    )
}

/**
 * TODO:
 *  - infer missing @Produces / @Consumes if @Path annotation exists
 *  - infer if @Path annotation exists without a value (???)
 *
 */
class JerseyMethodParameterDefaultValue(config: Config) : Rule(config) {
    companion object {
        val standardHttpMethodList = arrayOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
    }

    override val issue: Issue = JerseyIssueHolder.jerseyMethodParameterDefaultValue

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        val dbg = Debugger.make(JerseyMethodParameterDefaultValue::class.java.simpleName, debugStream)

        // TODO: ideally use fqName
        val hasPathAnnotation = function.hasAnnotation("Path")

        if (!hasPathAnnotation) {
            dbg.i("  hasPathAnnotation=false")
            return
        }

        function.valueParameters.forEach { parameter ->
            dbg.i("  parameter=${parameter.name}")
            if (parameter.hasDefaultValue()) {
                dbg.i("    hasDefaultValue=true")
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(parameter),
                        message = "probable jersey resource method (${function.name}) has parameter with defaultValue (${parameter.name})"
                    )
                )
            }
        }
    }
}
