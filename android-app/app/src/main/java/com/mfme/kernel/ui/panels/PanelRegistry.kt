package com.mfme.kernel.ui.panels

/** In-memory registry for panels. */
object PanelRegistry {
    private val panels = LinkedHashMap<String, PanelDescriptor>()

    fun register(descriptor: PanelDescriptor) {
        panels[descriptor.id] = descriptor
    }

    fun all(): List<PanelDescriptor> = panels.values.toList()
    fun byId(id: String): PanelDescriptor? = panels[id]
    fun clear() { panels.clear() }
}
