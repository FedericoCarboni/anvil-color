package colore.server;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static colore.ColoreMod.LOGGER;

public class Config {
    public boolean booksSetLore = true;
    public boolean consumeBookOnLoreAdded = true;
    public boolean consumeBookOnLoreReset = false;
    public boolean ampersandFormattingInWritableBooks = false;
    public boolean ampersandFormattingRename = true;
//    public boolean clientAmpersandFormattingDefault = true;
//    public char ampersandCharOverride = '&';

    public static Config load(File file) {
        Config config = new Config();
        Properties props = getProperties(config);
        try {
            if (file.isFile()) {
                props.load(new FileInputStream(file));
            } else {
                props.store(new FileOutputStream(file), "colore mod config file");
            }
        } catch (IOException ex) {
            LOGGER.error("cannot read config file, using default configuration", ex);
        }
        config.booksSetLore = Boolean.parseBoolean(props.getProperty("booksSetLore"));
        config.consumeBookOnLoreAdded = Boolean.parseBoolean(props.getProperty("consumeBookOnLoreAdded"));
        config.consumeBookOnLoreReset = Boolean.parseBoolean(props.getProperty("consumeBookOnLoreReset"));
        config.ampersandFormattingInWritableBooks = Boolean.parseBoolean(props.getProperty("ampersandFormattingInWritableBooks"));
        config.ampersandFormattingRename = Boolean.parseBoolean(props.getProperty("ampersandFormattingRename"));
//        config.clientAmpersandFormattingDefault = Boolean.parseBoolean(props.getProperty("clientAmpersandFormattingDefault"));
//        String c = props.getProperty("ampersandCharOverride");
//        config.ampersandCharOverride = c.isEmpty() ? config.ampersandCharOverride : c.charAt(0);
        return config;
    }

    @NotNull
    private static Properties getProperties(Config config) {
        Properties props = new Properties();
        props.setProperty("booksSetLore", Boolean.toString(config.booksSetLore));
        props.setProperty("consumeBookOnLoreAdded", Boolean.toString(config.consumeBookOnLoreAdded));
        props.setProperty("consumeBookOnLoreReset", Boolean.toString(config.consumeBookOnLoreReset));
        props.setProperty("ampersandFormattingInWritableBooks", Boolean.toString(config.ampersandFormattingInWritableBooks));
        props.setProperty("ampersandFormattingRename", Boolean.toString(config.ampersandFormattingRename));
//        props.setProperty("clientAmpersandFormattingDefault", Boolean.toString(config.clientAmpersandFormattingDefault));
//        props.setProperty("ampersandCharOverride", Character.toString(config.ampersandCharOverride));
        return props;
    }
}
