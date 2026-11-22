package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import net.johnceo.sparklingmutuals.safari.automation.SafariManagerGuard
import net.johnceo.sparklingmutuals.safari.automation.SafariTicketSelect
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

internal object CjsManagerGeometry {
    fun hit(eye: Vec3, view: Vec3, range: Double, box: AABB, block: Vec3?): Vec3? {
        if(!range.isFinite() || range !in .1..6.0 ||
            !listOf(eye.x,eye.y,eye.z,view.x,view.y,view.z).all { it.isFinite() }) return null
        val end=eye.add(view.normalize().scale(range))
        val hit=if(box.contains(eye)) eye else box.clip(eye,end).orElse(null) ?: return null
        if(block!=null && eye.distanceToSqr(block)<=eye.distanceToSqr(hit)) return null
        return hit
    }
}

/** Shared by native dispatch and controlled tests: the permit is consumed before any side effect. */
internal object CjsEntryDispatch {
    fun send(flow: CjsEntryFlow, effect: CjsEntryEffect, frame: () -> CjsEntryFrame, now: Long,
        boundary: (() -> Boolean) -> Boolean, manager: (UUID) -> Unit, ticket: (Int,Int) -> Unit): Boolean {
        if(!flow.matches(effect) || effect is CjsEntryEffect.Approach) return false
        val sent=try { boundary {
            val f=frame()
            when(effect) {
                is CjsEntryEffect.Manager -> if(flow.consumeManager(f,now)) { manager(effect.target);true } else false
                is CjsEntryEffect.Ticket -> if(flow.consumeTicket(f,now)) { ticket(effect.containerId,effect.slot);true } else false
            }
        } } catch(error: Exception) { flow.cancel("Input outcome uncertain: ${error.javaClass.simpleName}");return false }
        if(!sent) flow.cancel("Automatic input permission withdrawn")
        return sent
    }
}
