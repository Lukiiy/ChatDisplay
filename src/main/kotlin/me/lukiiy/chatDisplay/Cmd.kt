package me.lukiiy.chatDisplay

import com.mojang.brigadier.Command
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object Cmd {
    private val main = Commands.literal("chatdisplay")
        .requires { it.sender.hasPermission("chatdisplay.cmd") }
        .executes {
            it.source.sender.sendMessage(Component.text("ChatDisplay Reload complete!").color(NamedTextColor.GREEN))
            ChatDisplay.getInstance().reloadConfig()
            Command.SINGLE_SUCCESS
        }
        .then(Commands.literal("self")
            .executes {
                val sender = it.source.sender
                if (sender !is Player) throw SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(Component.text("This can only be used by players!").color(NamedTextColor.RED))).create()

                val result = ChatDisplay.getInstance().toggleSelfDisplay(sender)

                val bubble = ChatDisplay.getInstance().getBubble(sender)
                if (result && bubble != null) sender.showEntity(ChatDisplay.getInstance(), bubble)

                sender.sendMessage(Component.text("Toggled ${if (result) "on" else "off"} self chat display!").color(NamedTextColor.YELLOW))
                Command.SINGLE_SUCCESS
            }
        )

    fun register(): LiteralCommandNode<CommandSourceStack> = main.build()
}