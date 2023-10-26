package colore;

import colore.server.Config;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColoreMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("colore");

    private static Config config;

    @Override
    public void onInitialize() {
    }

    public static void onServerLoad(@NotNull MinecraftServer server) {
        LOGGER.debug("Loading config file");
        config = Config.load(server.getWorldPath(LevelResource.ROOT).resolve("colore.properties").toFile());
    }

    public static Config getConfig() {
        return config;
    }
}
