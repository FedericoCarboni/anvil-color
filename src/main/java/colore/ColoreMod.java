package colore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class ColoreMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("colore");

    public static boolean booksAddLore = true;
    public static boolean consumeBookOnLoreAdded = true;
    public static boolean consumeBookOnLoreReset = false;
    public static boolean styledRenames = true;

    @Override
    public void onInitialize() {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("colore.properties").toFile();
        Properties properties = new Properties();
        properties.setProperty("booksAddLore", "true");
        properties.setProperty("consumeBookOnLoreAdded", "true");
        properties.setProperty("consumeBookOnLoreReset", "false");
        properties.setProperty("styledRenames", "true");
        try {
            if (configFile.isFile()) {
                properties.load(new FileInputStream(configFile));
            } else {
                properties.store(new FileOutputStream(configFile), "colore mod config file");
            }
        } catch (IOException ex) {
            LOGGER.error("cannot open configuration file " + configFile.getAbsolutePath());
        }
        booksAddLore = Boolean.parseBoolean(properties.getProperty("booksAddLore", "true"));
        consumeBookOnLoreAdded = Boolean.parseBoolean(properties.getProperty("consumeBookOnLoreAdded", "true"));
        consumeBookOnLoreReset = Boolean.parseBoolean(properties.getProperty("consumeBookOnLoreReset", "true"));
        styledRenames = Boolean.parseBoolean(properties.getProperty("styledRenames", "true"));
    }
}
