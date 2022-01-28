package io.github.thewisenerd.linters.sidekt.rules

import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class ImageWidth (config: Config) : Rule(config) {

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    override val issue: Issue = Issue(
        id = ImageWidth::class.java.simpleName,
        severity = Severity.Performance,
        description = "Wasteful parameters w_768 detected. Please use precise width as per design/UI to avoid high bandwidth usage.",
        debt = Debt.TEN_MINS
    )

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        val dbg = Debugger.make(ImageWidth::class.java.simpleName, debugStream)
        val text = expression.text
        val patternWidth = Regex("https://ud-img.azureedge.net.*w_768.*")
        if(patternWidth.containsMatchIn(text)) {
            dbg.i("Usage of w_768 detected in string $text")
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Usage of w_768 detected. Please use precise width as per design/UI to avoid high bandwidth usage.."
                )
            )
        }
    }
}