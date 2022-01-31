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

class ImageQuality(config: Config) : Rule(config) {

    private val cloudinaryRefUrlForQuality = "https://cloudinary.com/documentation/image_optimization#automatic_quality_selection_q_auto."

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    override val issue: Issue = Issue(
        id = ImageQuality::class.java.simpleName,
        severity = Severity.Performance,
        description = "Wasteful parameters w_768/q_75 detected.",
        debt = Debt.TEN_MINS
    )

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        val dbg = Debugger.make(ImageQuality::class.java.simpleName, debugStream)
        val text = expression.text
        val patternQuality = Regex("https://ud-img.azureedge.net/.*q_75.*")
        if(patternQuality.containsMatchIn(text)) {
            dbg.i("Wasteful parameters detected q_75 in string $text")
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Wasteful parameters q_75 detected. Use q_auto instead. Refer to $cloudinaryRefUrlForQuality."
                )
            )
        }
    }
}