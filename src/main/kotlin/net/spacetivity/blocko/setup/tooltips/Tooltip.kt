package net.spacetivity.blocko.setup.tooltips

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.translation.Translation

data class Tooltip(val setupStepId: Int) {

    private val formatKey = "blocko.setup.explanation_tooltips.format"
    private val tooltipKey = "blocko.setup.explanation_tooltips.tip_${this.setupStepId}"

    init {
        Blocko.instance.tooltipHandler.registerTooltip(this)
    }

    fun getToolTipComponent(translation: Translation): Component {
        return translation.line(this.formatKey,
            Placeholder.parsed("step", setupStepId.inc().toString()),
            Placeholder.component("tool_tip", translation.line(tooltipKey)))
    }

}