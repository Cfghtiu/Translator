package kgg.translator;

import com.google.gson.*;
import kgg.translator.option.OptionRegistry;
import kgg.translator.translator.LLMTranslator;
import kgg.translator.util.ConfigUtil;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TranslatorConfig {
    private static final File file = new File("config", "translator.json");
    private static final File optionFile = new File("config", "translator_option.json");
    private static final Logger LOGGER = LogManager.getLogger(TranslatorConfig.class);
    private static boolean init = false;

    public static String read(String name) {
        try {
            File file = new File("config/translator/" + name);
            if (!file.exists()) {
                IOUtils.copy(Objects.requireNonNull(TranslatorMod.class.getClassLoader().getResourceAsStream(name)), new FileOutputStream(file));
            }
            return IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(String name, String content) throws IOException {
        File file = new File("config/translator/" + name);
        IOUtils.write(content, new FileOutputStream(file), StandardCharsets.UTF_8);
    }

    public static boolean readFile() {
        JsonObject config;
        JsonObject options;
        try {
            Language.load(TranslatorConfig.read("language.json"));

            config = ConfigUtil.load(file);
            boolean b = readConfig(config);
            assert b;
            options = ConfigUtil.load(optionFile);
        } catch (Exception e) {
            LOGGER.error("Failed to read config file", e);
            return false;
        }
        return readOptions(options);
    }

    public static boolean writeFile() {
        JsonObject config = new JsonObject();
        if (writeConfig(config)) {
            try {
                ConfigUtil.save(file, config);
                JsonObject options = new JsonObject();
                boolean b = writeOptions(options);
                assert b;
                ConfigUtil.save(optionFile, options);
                LOGGER.info("Config written successfully");
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to write config file", e);
                return false;
            }
        }
        return false;
    }

    public static boolean writeConfig(JsonObject config) {
        config.addProperty("from", TranslatorManager.getFrom());
        config.addProperty("to", TranslatorManager.getTo());
        LLMManager.writeConfig(config);
        config.addProperty("current", TranslatorManager.getCurrent().getName());
        TranslatorManager.getTranslators().forEach(translator -> {
            if (translator.isConfigured()) {
                JsonObject object = new JsonObject();
                translator.write(object);
                config.add(translator.getName(), object);
            }
        });
        return true;
    }

    public static boolean readConfig(JsonObject config) {
        init = true;
        try {
            TranslatorManager.setFrom(config.get("from").getAsString());
            TranslatorManager.setTo(config.get("to").getAsString());
            LLMManager.readConfig(config);
            String currentTranslator = config.get("current").getAsString();
            TranslatorManager.getTranslators().forEach(translator -> {
                JsonElement element = config.get(translator.getName());
                if (element != null) {
                    JsonObject translatorConfig = element.getAsJsonObject();
                    try {
                        translator.read(translatorConfig);
                    } catch (Exception e) {
                        LOGGER.error("{} failed to read config", translator.getName(), e);
                    }
                }

                if (translator.getName().equals(currentTranslator)) {
                    TranslatorManager.setTranslator(translator);
                }
            });
            LOGGER.info("Config read successfully");
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to read config", e);
            return false;
        } finally {
            init = false;
        }
    }

    private static boolean readOptions(JsonObject config) {
        OptionRegistry.options.forEach((key, value) -> {
            if (config.has(key)) {
                try {
                    OptionRegistry.readJsonElement(value, config.get(key));
                } catch (Exception e) {
                    LOGGER.error("{} failed to read option", key, e);
                }
            }
        });
        LOGGER.info("Options read successfully");
        return true;
    }

    private static boolean writeOptions(JsonObject config) {
        OptionRegistry.options.forEach((key, value) -> {
            config.add(key, OptionRegistry.createJsonElement(value));
        });
        return true;
    }

    public static boolean isInit() {
        return init;
    }
}
