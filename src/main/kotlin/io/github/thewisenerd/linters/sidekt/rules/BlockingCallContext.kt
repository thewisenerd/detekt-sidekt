package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class BlockingCallContext(config: Config) : Rule(config) {
    companion object {
        private const val DEFAULT_IO_DISPATCHER_FQN = "kotlinx.coroutines.Dispatchers.IO"

        private val DEFAULT_BLOCKING_EXCEPTION_TYPES = listOf(
            "java.lang.InterruptedException",
            "java.io.IOException"
        )

        private val DEFAULT_BLOCKING_METHOD_FQ_NAMES = listOf(
            "java.util.concurrent.CompletableFuture.get",
            "java.lang.Thread.sleep"
        )

        // ideally also show a replaceWith
        private val DEFAULT_RECLAIMABLE_METHOD_FQ_NAMES = listOf(
            "java.util.concurrent.CompletableFuture.get",
            "java.lang.Thread.sleep"
        )
    }

    override val issue: Issue = Issue(
        javaClass.simpleName,
        Severity.Warning,
        "blocking call made in possibly non-blocking context",
        Debt.TWENTY_MINS
    )

    private val reclaimableIssue = Issue(
        javaClass.simpleName + "-Reclaimable",
        Severity.Performance,
        "un-necessary blocking call made in possibly non-blocking context",
        Debt.FIVE_MINS
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

    private val ioDispatcherFqNames by lazy {
        val fqNames = mutableListOf(DEFAULT_IO_DISPATCHER_FQN)
        val extraFqNames = valueOrNull<ArrayList<String>>("ioDispatcherFqNames")
        extraFqNames?.let { fqNames.addAll(it) }

        fqNames.toSet().map { FqName(it) }
    }

    private val reclaimableMethodAnnotations by lazy {
        valueOrNull<ArrayList<String>>("reclaimableMethodAnnotations")?.let { arrayList ->
            arrayList.toSet().map { FqName(it) }
        } ?: emptyList()
    }

    private val reclaimableMethodFqNames by lazy {
        val fqNames = DEFAULT_RECLAIMABLE_METHOD_FQ_NAMES.toMutableList()
        val extraFqNames = valueOrNull<ArrayList<String>>("reclaimableMethodAnnotations")
        extraFqNames?.let { fqNames.addAll(it) }
        fqNames.toSet().map { FqName(it) }
    }

    private fun getFqNameFromValueArgument(argument: KtValueArgument): FqName? {
        val resolved = argument.getArgumentExpression().getResolvedCall(bindingContext)
        return resolved?.resultingDescriptor?.fqNameOrNull()
    }

    // ref: https://github.com/JetBrains/kotlin/blob/0cb039eea7180df2a62bf8db00847c5502653312/idea/src/org/jetbrains/kotlin/idea/inspections/blockingCallsDetection/CoroutineNonBlockingContextChecker.kt
    private fun isContextNonBlockingFor(dbg: Debugger, element: PsiElement): ContextInfo {
        if (element !is KtCallExpression)
            return ContextInfo(false)

        val containingLambda = element.parents.find { it is KtLambdaExpression }
        val containingArgument = containingLambda?.let { PsiTreeUtil.getParentOfType(it, KtValueArgument::class.java) }
        if (containingArgument != null) {
            // why not just use containingArgument? idk i'm just blindly kanging ref
            val callExpression =
                PsiTreeUtil.getParentOfType(containingArgument, KtCallExpression::class.java) ?: return ContextInfo(
                    false
                )
            val matchingArgument =
                callExpression.valueArguments.find { it == containingArgument } ?: return ContextInfo(false)
            val type = matchingArgument.getArgumentExpression()?.getType(bindingContext)

            // dispatcher is usually the first, so
            val probableDispatcherArgument = callExpression.valueArguments.firstOrNull()?.takeIf {
                callExpression.valueArguments.size > 1
            }
            val ioDispatcher = probableDispatcherArgument?.let {
                getFqNameFromValueArgument(it)
            }?.let { it in ioDispatcherFqNames }

            dbg.i(
                "lambda-expr:\n" +
                        "  el=${element.getTextWithLocation()}\n" +
                        "  lambda=${containingLambda.getTextWithLocation()}\n" +
                        "  arg=${containingArgument.getTextWithLocation()}\n" +
                        "  callExpr=${callExpression.getTextWithLocation()}\n" +
                        "  matchingArgument=${matchingArgument.getTextWithLocation()}\n" +
                        "  ioDispatcher=$ioDispatcher\n" +
                        "  suspending=${type?.isSuspendFunctionTypeOrSubtype}"
            )

            return ContextInfo(type?.isSuspendFunctionTypeOrSubtype == true, ioDispatcher)
        }

        val callingMethod = PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java)
        if (callingMethod != null) {
            val isSuspendingMethod = callingMethod.hasModifier(KtTokens.SUSPEND_KEYWORD)
            if (isSuspendingMethod) {
                dbg.i("got suspending callingMethod ${callingMethod.name} ${element.getTextWithLocation()}")
            }
            return ContextInfo(isSuspendingMethod)
        }

        return ContextInfo(false)
    }

    // kang from org.jetbrains.kotlin.codegen.FunctionCodegen.getThrownExceptions(org.jetbrains.kotlin.descriptors.FunctionDescriptor)
    // at 1.3.72
    private fun getThrownExceptionsZ(descriptor: FunctionDescriptor): List<ClassDescriptor> {
        val throwsAnnotation = descriptor.annotations.findAnnotation(FqName("kotlin.throws"))
            ?: descriptor.annotations.findAnnotation(FqName("kotlin.jvm.Throws"))
            ?: return emptyList()

        val valueArgument = throwsAnnotation.allValueArguments.values.firstOrNull()
        return if (valueArgument != null && valueArgument is ArrayValue) {
            valueArgument.value.mapNotNull { current ->
                (current as? KClassValue)?.let { classValue ->
                    DescriptorUtils.getClassDescriptorForType(classValue.getArgumentType(descriptor.module))
                }
            }
        } else emptyList()
    }

    private fun isMethodBlocking(
        dbg: Debugger,
        method: ResolvedCall<out CallableDescriptor>,
        contextInfo: ContextInfo
    ): BlockingMethodInfo {
        val descriptor = method.resultingDescriptor

        fun fromFqName(fqName: FqName): BlockingMethodInfo {
            return BlockingMethodInfo(true, contextInfo.ioDispatcher == true && fqName in reclaimableMethodFqNames)
        }

        fun fromBlockingExceptionTypeFqName(fqName: FqName): BlockingMethodInfo {
            // ??
            return BlockingMethodInfo(true)
        }

        fun fromBlockingMethodAnnotationFqName(fqName: FqName): BlockingMethodInfo {
            return BlockingMethodInfo(true, contextInfo.ioDispatcher == true && fqName in reclaimableMethodAnnotations)
        }


        val fqName = descriptor.fqNameOrNull()
        dbg.i("  got fqName=$fqName")
        if (fqName != null && fqName in blockingMethodFqNames) {
            dbg.i("  got blocking method fqName $fqName")
            return fromFqName(fqName)
        }

        val thrownExceptions = if (descriptor is FunctionDescriptor) {
            getThrownExceptionsZ(descriptor)
        } else emptyList()

        val throwBlockingExceptionType =
            thrownExceptions.mapNotNull { it.fqNameOrNull() }.find { it in blockingExceptionTypes }

        if (throwBlockingExceptionType != null) {
            dbg.i("  throwBlockingExceptionType $throwBlockingExceptionType")
            return fromBlockingExceptionTypeFqName(throwBlockingExceptionType)
        }

        val annotations = descriptor.annotations
        annotations.forEach { annotation ->
            val annotationFqName = annotation.fqName
            if (annotationFqName != null && annotationFqName in blockingMethodAnnotations) {
                dbg.i("  hasBlockingAnnotation=true, ${annotation.fqName}")
                return fromBlockingMethodAnnotationFqName(annotationFqName)
            }
        }

        return BlockingMethodInfo(false)
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
        val contextInfo = isContextNonBlockingFor(dbg, element)
        if (!contextInfo.blocking) {
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

        val blockingMethodInfo = isMethodBlocking(dbg, method, contextInfo)
        if (!blockingMethodInfo.blocking) {
            dbg.i("  isMethodBlocking=false")
            return
        }

        dbg.i("reporting BlockingContext $blockingMethodInfo for element ${element.getTextWithLocation()}")
        val methodName = method.resultingDescriptor.fqNameOrNull()
        if (contextInfo.ioDispatcher == true) {
            if (blockingMethodInfo.reclaimable) {
                report(
                    CodeSmell(
                        issue = reclaimableIssue,
                        entity = Entity.from(element),
                        message = "method ($methodName) reclaimable in non-blocking context"
                    )
                )
            } else {
                // TODO: counter and exit logs
            }
        } else {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(element),
                    message = "method ($methodName) called in non-blocking context"
                )
            )
        }
    }
}

private data class ContextInfo(
    val blocking: Boolean,
    val ioDispatcher: Boolean? = null
)

private data class BlockingMethodInfo(
    val blocking: Boolean,
    val reclaimable: Boolean = false
)