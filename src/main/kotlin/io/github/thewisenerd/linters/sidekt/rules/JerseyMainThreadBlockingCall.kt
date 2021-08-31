package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.*

class JerseyMainThreadBlockingCall(config: Config) : Rule(config)  {

    companion object {
        const val RUN_BLOCKING_EXP_ID = "runBlocking"
        val httpMethodList = arrayOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
    }

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    override val issue: Issue = Issue(
        id = JerseyMainThreadBlockingCall::class.java.simpleName,
        severity = Severity.Performance,
        description = "main thread blocking call in resource method",
        debt = Debt.TEN_MINS
    )


    override fun visitNamedFunction(resourceMethod: KtNamedFunction) {
        super.visitNamedFunction(resourceMethod)
        val dbg = Debugger.make(JerseyMainThreadBlockingCall::class.java.simpleName, debugStream)

        val hasPathAnnotation = resourceMethod.hasAnnotation("Path")
        val hasAnyOfHttpMethodAnnotation = resourceMethod.hasAnnotation(*httpMethodList)

        if (!hasPathAnnotation && !hasAnyOfHttpMethodAnnotation) {
            dbg.i("  hasPathAnnotation=false or none of http method annotation available")
            return
        }

        var methodBlockingMainThread = false
        resourceMethod.children.forEach {
            if(it is KtCallExpression) {
                val isUsingRunBlockingExpression = (it.calleeExpression as KtNameReferenceExpression).getReferencedNameAsName().identifier == RUN_BLOCKING_EXP_ID
                if(isUsingRunBlockingExpression) {
                    methodBlockingMainThread = true
                }
            }

            if(it is KtBlockExpression) {
                it.children.map { blkExp ->
                    if(blkExp is KtCallExpression) {
                        val identifiedBlockingExp = (blkExp.calleeExpression as KtNameReferenceExpression).getReferencedNameAsName().identifier == RUN_BLOCKING_EXP_ID
                        if(identifiedBlockingExp){
                            methodBlockingMainThread = true
                        }
                    }

                    if(blkExp is KtReturnExpression) {
                       blkExp.children.filterIsInstance<KtCallExpression>().map { retExp ->
                           val identifiedBlockingExp = (retExp.calleeExpression as KtNameReferenceExpression).getReferencedNameAsName().identifier == RUN_BLOCKING_EXP_ID
                           if(identifiedBlockingExp){
                               methodBlockingMainThread = true
                           }
                       }
                    }
                }
            }
        }

        if(methodBlockingMainThread) {
            dbg.i("blocks main application thread")
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(resourceMethod),
                    message = "jersey resource method (${resourceMethod.name}) have main thread blocking call)"
                )
            )
        }
    }
}