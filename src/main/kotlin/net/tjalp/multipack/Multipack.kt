package net.tjalp.multipack

import kotlinx.coroutines.runBlocking
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

        PlayerListener(packService).also { server.pluginManager.registerEvents(it, this) }
    }
}
