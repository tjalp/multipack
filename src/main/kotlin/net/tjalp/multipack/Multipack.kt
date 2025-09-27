package net.tjalp.multipack

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.coroutines.runBlocking
import net.tjalp.multipack.command.MultipackCommand
import net.tjalp.multipack.listener.PlayerListener
import org.bukkit.plugin.java.JavaPlugin

class Multipack : JavaPlugin() {

    private val packService = PackService(this)

    override fun onEnable() {
        saveDefaultConfig()

        if (server.serverResourcePack != null) logger.warning("There is already a resource pack set on the server, this may cause issues with Multipack.")

        runBlocking {
            packService.load(config)
        }

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            commands.registrar().register(MultipackCommand.create(this, packService))
        }

        PlayerListener(packService).also { server.pluginManager.registerEvents(it, this) }
    }
}
