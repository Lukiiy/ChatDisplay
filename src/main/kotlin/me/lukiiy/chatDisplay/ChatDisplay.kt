package me.lukiiy.chatDisplay

import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffectType
import java.util.function.Consumer

class ChatDisplay : JavaPlugin(), Listener {
    private val bubbles: MutableMap<Player?, TextDisplay?> = mutableMapOf()
    private val selfDisplays: MutableSet<Player?> = mutableSetOf()

    override fun onEnable() {
        setupConfig()
        server.pluginManager.registerEvents(this, this)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(Cmd.register(), "ChatDisplay's main command!")
        }
    }

    companion object {
        fun getInstance(): ChatDisplay = getPlugin(ChatDisplay::class.java)
    }

    // Config
    fun setupConfig() {
        saveDefaultConfig()
        config.options().copyDefaults(true)
        saveConfig()
    }

    // Listener
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun chat(e: AsyncChatEvent) = Bukkit.getGlobalRegionScheduler().execute(this) { doBubble(e.player, e.message()) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun worldChange(e: PlayerChangedWorldEvent) { // why
        val p = e.player
        val bubble = getBubble(p)

        if (bubble != null && !selfDisplays.contains(p)) {
            p.scheduler.execute(this, { p.hideEntity(this, bubble) }, null, 2L)
        }
    }

    private fun doBubble(p: Player, msg: Component) {
        if (p.isInvis()) return

        if (bubbles.containsKey(p)) {
            bubbles[p]!!.apply {
                ticksLived = 1
                text(msg)
            }
            return
        }

        val billboard = try {
            Display.Billboard.valueOf(config.getString("billBoard")!!)
        } catch (_: Exception) {
            Display.Billboard.CENTER
        }

        fun loc(): Location = p.location.add(0.0, p.boundingBox.height + config.getDouble("yPos"), 0.0).apply {
            yaw = 0f
            pitch = 0f
        }

        val display = p.world.spawn(loc(), TextDisplay::class.java) {
            it.isPersistent = false
            it.text(msg)
            it.isSeeThrough = false
            it.isShadowed = config.getBoolean("shadow")
            it.viewRange = config.getDouble("viewRange", 16.0).toFloat()
            it.teleportDuration = config.getInt("tick.movement", 4)
            it.textOpacity = funByte("opacity.default")
            it.billboard = billboard

            it.isDefaultBackground = config.getInt("bg.red") < 0
            if (!it.isDefaultBackground) it.backgroundColor = Color.fromARGB(config.getInt("bg.alpha").coerceIn(0, 255), config.getInt("bg.red").coerceIn(0, 255), config.getInt("bg.green").coerceIn(0, 255), config.getInt("bg.blue").coerceIn(0, 255))

            if (!selfDisplays.contains(p)) p.hideEntity(this, it)
            bubbles.put(p, it)
        }

        p.scheduler.runAtFixedRate(this, Consumer {
            if (!p.isValid || !display.isValid || p.isInvis() || display.ticksLived > config.getInt("timeSpan", 100)) {
                it.cancel()
                display.remove()
                bubbles.remove(p)
                return@Consumer
            }

            display.textOpacity = if (p.isSneaking) funByte("opacity.sneaking") else funByte("opacity.default")
            display.teleport(loc())

            p.location.getNearbyPlayers(display.viewRange.toDouble()).forEach { pRad ->
                if (pRad == p) return@forEach
                if (!pRad.canSee(p)) pRad.hideEntity(this, display) else pRad.showEntity(this, display)
            }
        }, null, 1L, config.getLong("tick.update", 2))
    }

    private fun funByte(path: String): Byte = (config.getDouble(path, 1.0).coerceIn(0.0, 1.0) * 255).toInt().toByte()

    private fun Player.isInvis(): Boolean = (this.gameMode == GameMode.SPECTATOR || this.hasPotionEffect(PotionEffectType.INVISIBILITY))

    // API?
    fun getBubble(p: Player): TextDisplay? = bubbles[p]

    fun toggleSelfDisplay(p: Player): Boolean {
        return when {
            selfDisplays.add(p) -> true
            selfDisplays.remove(p) -> false
            else -> false
        }
    }
}
