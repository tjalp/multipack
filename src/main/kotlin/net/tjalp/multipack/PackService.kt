package net.tjalp.multipack

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.resource.ResourcePackCallback.onTerminal
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.configuration.file.FileConfiguration
import java.net.URI

class PackService(
    private val plugin: Multipack
) {

    private var request: ResourcePackRequest.Builder? = null
        get() = field?.let { ResourcePackRequest.resourcePackRequest(it.build()) }

    /**
     * Load the packs from the config.
     *
     * @param config The configuration to load the packs from.
     */
    suspend fun load(config: FileConfiguration) {
        val packUrls = config.getStringList("pack_urls").reversed()

        if (packUrls.isEmpty()) {
            plugin.logger.warning("No pack URLs found in config, please add some to use Multipack.")
        }

        val packs = coroutineScope {
            packUrls.map { url ->
                async {
                    plugin.logger.info("Loading resource pack from URL: '$url'")

                    val packInfo = try {
                        ResourcePackInfo.resourcePackInfo()
                            .uri(URI.create(url))
                            .computeHashAndBuild()
                            .await()
                    } catch (e: Exception) {
                        plugin.logger.warning("Failed to load resource pack from URL: '$url'")
                        plugin.logger.warning("Error: ${e.message}")
                        null
                    }

                    if (packInfo != null) {
                        plugin.logger.info("Successfully loaded resource pack from URL: '$url'")
                    }

                    packInfo
                }
            }.awaitAll().filterNotNull()
        }

        request = ResourcePackRequest.resourcePackRequest()
            .packs(packs)
            .replace(true)
            .required(true)

        send(plugin.server, await = false)
    }

    /**
     * Send the packs to the given audience and waits until done sending.
     *
     * @param audience The audience to send the packs to.
     * @param await Whether to wait for the audience to load the packs before returning. Defaults to true.
     */
    suspend fun send(audience: Audience, await: Boolean = true) {
        val audienceRequest = request

        if (audienceRequest == null) {
            plugin.logger.warning("Attempted to send resource packs before they were loaded.")
            return
        }

        var loadedPackCount = 0
        val requiredPackCount = audienceRequest.asResourcePackRequest().packs().size

        if (requiredPackCount <= 0) {
            plugin.logger.info("No resource packs to send to ${audience.get(Identity.NAME)}, clearing any existing packs...")
            audience.clearResourcePacks()
            return
        }

        val deferred = CompletableDeferred<Boolean>()

        audienceRequest.callback(
            onTerminal(
                { packId, audience ->
                    plugin.slF4JLogger.debug("{} has successfully loaded pack {}, {} of {}", audience.get(Identity.NAME), packId, loadedPackCount + 1, requiredPackCount)
                    loadedPackCount++

                    if (loadedPackCount >= requiredPackCount) deferred.complete(true)
                },
                { packId, audience ->
                    plugin.slF4JLogger.debug("{} failed to load pack {}, {} of {}", audience.get(Identity.NAME), packId, loadedPackCount + 1, requiredPackCount)
                    loadedPackCount++

                    if (loadedPackCount >= requiredPackCount) deferred.complete(false)
                }
            )
        )

        audience.sendResourcePacks(audienceRequest)

        if (await) deferred.await()

        plugin.logger.info("Sent resource packs to ${audience.get(Identity.DISPLAY_NAME)}")
    }
}