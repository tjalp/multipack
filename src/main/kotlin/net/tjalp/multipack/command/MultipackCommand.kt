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
import kotlin.time.measureTime

object MultipackCommand {

    fun create(plugin: Multipack, packService: PackService): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("multipack")
            .requires(Commands.restricted { source -> source.sender.hasPermission("multipack.reload") })
            .then(Commands.literal("reload")
                .executes { context ->
                    context.source.sender.sendPlainMessage("Reloading Multipack configuration...")

                    CoroutineScope(Dispatchers.Default).launch {
                        val duration = measureTime {
                            plugin.reloadConfig()
                            packService.load(plugin.config)
                        }

                        context.source.sender.sendPlainMessage("Reloaded Multipack configuration (${duration.inWholeMilliseconds} ms)!")
                    }

                    return@executes Command.SINGLE_SUCCESS
                })
            .build()
    }
}