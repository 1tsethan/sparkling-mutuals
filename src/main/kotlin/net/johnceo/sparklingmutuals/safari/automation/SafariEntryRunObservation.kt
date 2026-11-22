package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.safari.*
import net.minecraft.client.Minecraft

/** Tracks current run presence without registering or changing any client input. */
internal object SafariEntryRunObservation {
    private val runState = SafariEntryRunState()

    fun activeRun(client: Minecraft): Boolean {
        val player = client.player ?: return false
        return runState.active(client.level, SafariIsland.onIsland, SafariAssist.location,
            SafariAreaMap.areaAt(player.x, player.y, player.z))
    }
    fun reset() = runState.reset()
}
