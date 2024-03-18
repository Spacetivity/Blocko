package net.spacetivity.blocko.scoreboard

import java.util.*

class SidebarHandler {

    private val cachedSidebars: MutableSet<Sidebar> = mutableSetOf()

    fun getSidebar(uuid: UUID): Sidebar? = this.cachedSidebars.find { it.viewer.uniqueId == uuid }

    fun registerSidebar(sidebar: Sidebar) {
        this.cachedSidebars.add(sidebar)
    }

    fun unregisterSidebar(uuid: UUID) {
        val sidebar: Sidebar = getSidebar(uuid) ?: return
        sidebar.reset()
        this.cachedSidebars.remove(sidebar)
    }

}