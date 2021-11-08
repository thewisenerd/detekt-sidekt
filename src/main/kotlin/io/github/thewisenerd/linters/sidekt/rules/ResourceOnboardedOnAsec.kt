package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.*

class ResourceOnboardedOnAsec(config: Config) : Rule(config) {

    companion object {
        val ASEC_ANNOTATION = arrayOf("UDErrorMonitoredApi")
        val httpMethodList = arrayOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
    }

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    override val issue: Issue = Issue(
        id = ResourceOnboardedOnAsec::class.java.simpleName,
        severity = Severity.Performance,
        description = "ASEC annotation is missed resource method",
        debt = Debt.TEN_MINS
    )


    override fun visitNamedFunction(resourceMethod: KtNamedFunction) {
        super.visitNamedFunction(resourceMethod)
        val dbg = Debugger.make(ResourceOnboardedOnAsec::class.java.simpleName, debugStream)

        val hasPathAnnotation = resourceMethod.hasAnnotation("Path")
        val hasAnyOfHttpMethodAnnotation = resourceMethod.hasAnnotation(*httpMethodList)

        if (hasPathAnnotation.not() && hasAnyOfHttpMethodAnnotation.not()) {
            dbg.i("  hasPathAnnotation=false or none of http method annotation available")
            return
        }

        // Check ASEC annotation is missed.
        var hasAsecAnnotation = resourceMethod.hasAnnotation(*ASEC_ANNOTATION)

        if (hasAsecAnnotation.not()) {
            dbg.i("ASEC annotation is missed for resource")
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(resourceMethod),
                    message = "Onboard Resources (${resourceMethod.name} on ASEC))"
                )
            )
        }
    }
}
