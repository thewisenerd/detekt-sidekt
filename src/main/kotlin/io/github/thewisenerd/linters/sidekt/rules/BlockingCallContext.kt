package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getTextWithLocation
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class BlockingCallContext(config: Config): Rule(config) {
    override val issue: Issue = Issue(
        javaClass.simpleName,
        Severity.Warning,
        "blocking call made in possibly non-blocking context",
        Debt.TWENTY_MINS
    )

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    private val blockingAnnotations by lazy {
        val annotations = mutableListOf("BlockingCall")
        val extraAnnotations = valueOrNull<ArrayList<String>>("blockingAnnotations")
        extraAnnotations?.let { annotations.addAll(it) }
        annotations.toSet().map { FqName(it) }
    }

    // ref: https://github.com/JetBrains/kotlin/blob/0cb039eea7180df2a62bf8db00847c5502653312/idea/src/org/jetbrains/kotlin/idea/inspections/blockingCallsDetection/CoroutineNonBlockingContextChecker.kt
    private fun isContextNonBlockingFor(dbg: Debugger, element: PsiElement): Boolean {
        if (element !is KtCallExpression)
            return false

        val containingLambda = element.parents.find { it is KtLambdaExpression }
        val containingArgument = containingLambda?.let { PsiTreeUtil.getParentOfType(it, KtValueArgument::class.java) }
        if (containingArgument != null) {
            // why not just use containingArgument? idk i'm just blindly kanging ref
            val callExpression = PsiTreeUtil.getParentOfType(containingArgument, KtCallExpression::class.java) ?: return false
            val matchingArgument = callExpression.valueArguments.find { it == containingArgument } ?: return false
            val type = matchingArgument.getArgumentExpression()?.getType(bindingContext)

            dbg.i("lambda-expr:\n" +
                    "  el=${element.getTextWithLocation()}\n" +
                    "  lambda=${containingLambda.getTextWithLocation()}\n" +
                    "  arg=${containingArgument.getTextWithLocation()}\n" +
                    "  callExpr=${callExpression.getTextWithLocation()}\n" +
                    "  matchingArgument=${matchingArgument.getTextWithLocation()}\n" +
                    "  suspending=${type?.isSuspendFunctionTypeOrSubtype}")

            return type?.isSuspendFunctionTypeOrSubtype == true
        }

        val callingMethod = PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java)
        if (callingMethod != null) {
            val isSuspendingMethod = callingMethod.hasModifier(KtTokens.SUSPEND_KEYWORD)
            if (isSuspendingMethod) {
                dbg.i("got suspending callingMethod ${callingMethod.name} ${element.getTextWithLocation()}")
            }
            return isSuspendingMethod
        }

        return false
    }

    private fun isMethodBlocking(dbg: Debugger, method: ResolvedCall<out CallableDescriptor>): Boolean {
        val annotations = method.resultingDescriptor.annotations

        var hasBlockingAnnotation = false
        annotations.forEach { annotation ->
            val result = annotation.fqName in blockingAnnotations
            dbg.i("  hasBlockingAnnotation ${annotation.fqName} = $result")
            if (result) hasBlockingAnnotation = true
        }

        return hasBlockingAnnotation
    }


    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        val dbg = Debugger.make(debugStream)
        dbg.i("visiting element $element type=${element.javaClass.simpleName} ctx=${bindingContext != BindingContext.EMPTY} text=${element.text}")
        if (bindingContext == BindingContext.EMPTY) return

        try {
            visitElementSafe(dbg, element)
        } catch (e: Exception) {
            dbg.i("exception on visitElement, e=${element.text}, m=${e.message}")
        }
    }

    private fun visitElementSafe(dbg: Debugger, element: PsiElement) {
        if (!isContextNonBlockingFor(dbg, element)) {
            dbg.i("  isContextNonBlocking=false")
            return
        }

        if (element !is KtCallExpression) {
            dbg.i("  element !is KtCallExpression")
            return
        }

        val method = element.getResolvedCall(bindingContext)
        if (method == null) {
            dbg.i("  resolvedCall == null")
            return
        }

        if (!isMethodBlocking(dbg, method)) {
            dbg.i("  isMethodBlocking=false")
            return
        }

        val methodName = method.resultingDescriptor.fqNameOrNull()
        report(CodeSmell(issue, Entity.from(element), message = "method ($methodName) called in non-blocking context"))
    }
}