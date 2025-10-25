package net.tjalp.multipack.listener

import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.tjalp.multipack.PackService
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import kotlin.time.Duration.Companion.minutes

class PlayerListener(
    private val packService: PackService
) : Listener {

    @Suppress("UnstableApiUsage")
    @EventHandler
    fun on(event: AsyncPlayerConnectionConfigureEvent) {
        val audience = event.connection.audience

        runBlocking {
            try {
                withTimeout(5.minutes) {
                    packService.send(audience)
                }
            } catch (e: TimeoutCancellationException) {
                Bukkit.getServer().logger.warning("Timed out while trying to load resource packs for player ${event.connection.audience.get(
                    Identity.NAME)}")
                event.connection.disconnect(text("Timed out while trying to load resource packs. Please try again.", RED))
            }
        }
    }
}