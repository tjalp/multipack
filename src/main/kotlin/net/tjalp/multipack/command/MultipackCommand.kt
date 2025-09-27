package net.tjalp.multipack.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.tjalp.multipack.Multipack
import net.tjalp.multipack.PackService

object MultipackCommand {

    fun create(plugin: Multipack, packService: PackService): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("multipack")
            .then(Commands.literal("reload")
                .executes { context ->
                    context.source.sender.sendPlainMessage("Reloading Multipack configuration...")

                    CoroutineScope(Dispatchers.Default).launch {
                        plugin.reloadConfig()
                        packService.load(plugin.config)

                        context.source.sender.sendPlainMessage("Reloaded Multipack configuration!")
                    }

                    return@executes Command.SINGLE_SUCCESS
                })
            .build()
    }
}