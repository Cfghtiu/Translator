package kgg.translator;

import kgg.translator.command.TranslateCommand;
import kgg.translator.command.TranslateConfigCommand;
import kgg.translator.handler.KeyBindingHandler;
import kgg.translator.translator.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TranslatorMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 创建config/translator/文件夹
        new File(FabricLoader.getInstance().getConfigDir().toFile(), "translator").mkdirs();
        // 命令
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            TranslateCommand.register(dispatcher);
            TranslateConfigCommand.register(dispatcher);
        });
        // 语言
        try {
            Language.load(load("language.json", false));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 保存配置
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            TranslatorConfig.readFile();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            TranslatorConfig.writeFile();
        });
        // 按键注册
        KeyBindingHandler.register();

        // 普通翻译器
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            TranslatorManager.addTranslator(new BaiduTranslatorModMenuImpl());
            TranslatorManager.addTranslator(new YouDaoTranslatorModMenuImpl());
        } else {
            TranslatorManager.addTranslator(new BaiduTranslatorImpl());
            TranslatorManager.addTranslator(new YouDaoTranslatorImpl());
        }
        // LLM翻译器
        try {
            String ai = load("ai-list.txt", false);
            for (String line : ai.split("\n")) {
                String[] split = line.split(",");
                String name = split[0];
                String url = split[1];
                String model = split[2];
                LLMTranslator translator = createLLMTranslator(name, url);
                translator.setConfig("", model);
                TranslatorManager.addTranslator(translator);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LLMTranslator createLLMTranslator(String name, String url) {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return new LLMTranslatorModMenuImpl(name, url);
        } else {
            return new LLMTranslatorImpl(name, url);
        }
    }

    private static String load(String name, boolean jar) throws IOException {
        if (!jar) {
            // 如果存在 config/translator/name，则读取，否则读取包内文件
            File file = new File("config/translator/" + name);
            if (file.exists()) {
                return IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
            }
        }
        try {
            return IOUtils.toString(TranslatorMod.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8);
        } catch (IOException e) {
            if (jar) {
                throw new RuntimeException(e);
            }
            throw e;
        }
    }
}
