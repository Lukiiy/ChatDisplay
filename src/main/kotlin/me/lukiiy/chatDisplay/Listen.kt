package me.lukiiy.chatDisplay

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent

class Listen : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun chat(e: AsyncChatEvent) {
        e.player.scheduler.run(ChatDisplay.getInstance(), { ChatDisplay.getInstance().doBubble(e.player, e.message()) }, null)
    }

    @EventHandler
    fun worldChange(e: PlayerChangedWorldEvent) { // why
        val p = e.player
        val inst = ChatDisplay.getInstance()
        val bubble = inst.getBubble(p)

        if (bubble != null && !inst.getSelfDisplay(p)) p.scheduler.execute(inst, { p.hideEntity(inst, bubble) }, null, 2L)
    }

    @EventHandler
    fun quit(e: PlayerQuitEvent) {
        ChatDisplay.getInstance().getBubble(e.player)?.remove()
    }
}