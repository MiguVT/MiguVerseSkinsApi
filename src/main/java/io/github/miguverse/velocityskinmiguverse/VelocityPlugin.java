package io.github.miguverse.velocityskinmiguverse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.net.URL;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.slf4j.Logger;
import static spark.Spark.get;
import static spark.Spark.port;

@Plugin(
    id = "miguverseplugin",
    name = "MiguVerseSkins",
    description = "A skin hook with skinsrestorer for Velocity.",
    version = "1.0.0",
    authors = { "MiguVT" }
)
public final class VelocityPlugin {
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(final ProxyInitializeEvent event) {
        // Inicialización del plugin...
		port(2420);
        get("/:player", (req, res) -> {
            String playerName = req.params(":player");
            try {
                SkinProperty skin = getSkin(playerName);
                if (skin != null) {
                    // Parsear la información de la skin a un objeto JSON
                    JsonObject skinData = new Gson().fromJson(new String(Base64.getDecoder().decode(skin.getValue())), JsonObject.class);
                    // Obtener la URL de la textura de la skin
                    String textureUrl = skinData.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

                    // Descargar la imagen de la textura de la skin
                    res.type("image/png");
                    URL url = new URL(textureUrl);
                    try (InputStream in = url.openStream()) {
                        byte[] imageBytes = in.readAllBytes();
                        res.raw().getOutputStream().write(imageBytes);
                        res.raw().getOutputStream().flush();
                        res.raw().getOutputStream().close();
                    }
                    return res.raw();
                } else {
                    res.status(404);
                    return "No skin found for player " + playerName;
                }
            } catch (Exception e) {
                logger.error("Error retrieving skin for player " + playerName, e);
                res.status(500);
                return "Internal server error";
            }
        });
    }

    private SkinProperty getSkin(String playerName) {
        try {
            Optional<Player> playerOptional = server.getPlayer(playerName);
            if (!playerOptional.isPresent()) {
                logger.info("Player not found or not online: " + playerName);
                return null;
            }

            Player player = playerOptional.get();
            UUID playerUUID = player.getUniqueId();

            SkinsRestorer skinsRestorerApi = SkinsRestorerProvider.get();
            PlayerStorage playerStorage = skinsRestorerApi.getPlayerStorage();

            // Here, the skin is retrieved from SkinsRestorer's own storage.
            Optional<SkinProperty> skinPropertyOptional = playerStorage.getSkinForPlayer(playerUUID, playerName);
            return skinPropertyOptional.orElse(null);
        } catch (Exception e) {
            logger.error("Failed to get skin for player " + playerName, e);
            return null;
        }
    }
}
