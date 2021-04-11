package io.github.thewisenerd.linters.sidekt

import io.github.thewisenerd.linters.sidekt.rules.BlockingCallContext
import io.github.thewisenerd.linters.sidekt.rules.BlockingCallContextReclaimable
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class SideKtRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "sidekt"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            BlockingCallContext(config),
            BlockingCallContextReclaimable(config)
        )
    )
}