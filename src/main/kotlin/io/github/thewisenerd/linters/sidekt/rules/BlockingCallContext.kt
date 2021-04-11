package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.codegen.FunctionCodegen.getThrownExceptions
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
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

class BlockingCallContext(config: Config) : Rule(config) {
    companion object {
        private val DEFAULT_BLOCKING_EXCEPTION_TYPES = listOf(
            "java.lang.InterruptedException",
            "java.io.IOException"
        )

        private val DEFAULT_BLOCKING_METHOD_FQ_NAMES = listOf(
            "java.util.concurrent.CompletableFuture.get"
        )
    }

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

    private val blockingMethodAnnotations by lazy {
        val annotations = mutableListOf<String>()
        val extraAnnotations = valueOrNull<ArrayList<String>>("blockingMethodAnnotations")
        extraAnnotations?.let { annotations.addAll(it) }
        annotations.toSet().map { FqName(it) }
    }

    private val blockingMethodFqNames by lazy {
        val fqNames = DEFAULT_BLOCKING_METHOD_FQ_NAMES.toMutableList()
        val extraFqNames = valueOrNull<ArrayList<String>>("blockingMethodFqNames")
        extraFqNames?.let { fqNames.addAll(it) }
        fqNames.toSet().map { FqName(it) }
    }

    private val blockingExceptionTypes by lazy {
        val types = DEFAULT_BLOCKING_EXCEPTION_TYPES.toMutableList()

        val extraExceptionTypes = valueOrNull<ArrayList<String>>("blockingExceptionTypes")
        extraExceptionTypes?.let { types.addAll(it) }

        types.toSet().map { FqName(it) }
    }

    // ref: https://github.com/JetBrains/kotlin/blob/0cb039eea7180df2a62bf8db00847c5502653312/idea/src/org/jetbrains/kotlin/idea/inspections/blockingCallsDetection/CoroutineNonBlockingContextChecker.kt
    private fun isContextNonBlockingFor(dbg: Debugger, element: PsiElement): Boolean {
        if (element !is KtCallExpression)
            return false

        val containingLambda = element.parents.find { it is KtLambdaExpression }
        val containingArgument = containingLambda?.let { PsiTreeUtil.getParentOfType(it, KtValueArgument::class.java) }
        if (containingArgument != null) {
            // why not just use containingArgument? idk i'm just blindly kanging ref
            val callExpression =
                PsiTreeUtil.getParentOfType(containingArgument, KtCallExpression::class.java) ?: return false
            val matchingArgument = callExpression.valueArguments.find { it == containingArgument } ?: return false
            val type = matchingArgument.getArgumentExpression()?.getType(bindingContext)

            dbg.i(
                "lambda-expr:\n" +
                        "  el=${element.getTextWithLocation()}\n" +
                        "  lambda=${containingLambda.getTextWithLocation()}\n" +
                        "  arg=${containingArgument.getTextWithLocation()}\n" +
                        "  callExpr=${callExpression.getTextWithLocation()}\n" +
                        "  matchingArgument=${matchingArgument.getTextWithLocation()}\n" +
                        "  suspending=${type?.isSuspendFunctionTypeOrSubtype}"
            )

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

    private fun isMethodBlocking(
        dbg: Debugger,
        method: ResolvedCall<out CallableDescriptor>
    ): Boolean {
        val descriptor = method.resultingDescriptor

        val fqName = descriptor.fqNameOrNull()
        dbg.i("  got fqName=$fqName")
        if (fqName in blockingMethodFqNames) {
            dbg.i("  got blocking method fqName $fqName")
            return true
        }

        val thrownExceptions = if (descriptor is FunctionDescriptor) {
            getThrownExceptions(descriptor)
        } else emptyList()

        val throwBlockingExceptionType =
            thrownExceptions.mapNotNull { it.fqNameOrNull() }.find { it in blockingExceptionTypes }

        if (throwBlockingExceptionType != null) {
            dbg.i("  throwBlockingExceptionType $throwBlockingExceptionType")
            return true
        }

        val annotations = descriptor.annotations
        var hasBlockingAnnotation = false
        annotations.forEach { annotation ->
            if (annotation.fqName in blockingMethodAnnotations) {
                dbg.i("  hasBlockingAnnotation ${annotation.fqName}")
                hasBlockingAnnotation = true
            }
        }

        if (hasBlockingAnnotation) {
            dbg.i("  hasBlockingAnnotation=true")
            return true
        }

        return false
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

        dbg.i("reporting BlockingContext for element ${element.getTextWithLocation()}")
        val methodName = method.resultingDescriptor.fqNameOrNull()
        report(CodeSmell(issue, Entity.from(element), message = "method ($methodName) called in non-blocking context"))
    }
}