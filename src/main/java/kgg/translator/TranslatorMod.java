package kgg.translator;

import kgg.translator.command.LLMConfigCommand;
import kgg.translator.command.TranslateCommand;
import kgg.translator.command.TranslateConfigCommand;
import kgg.translator.handler.KeyBindingHandler;
import kgg.translator.option.Options;
import kgg.translator.translator.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TranslatorMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 创建config/translator/文件夹
        new File("config/translator").mkdirs();
        // 命令
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            TranslateCommand.register(dispatcher);
            TranslateConfigCommand.register(dispatcher);
            LLMConfigCommand.register(dispatcher);  // 注册LLM配置命令
        });

        Options.init();

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
        TranslatorManager.addTranslator(new BaiduTranslatorModMenuImpl());
        TranslatorManager.addTranslator(new YouDaoTranslatorModMenuImpl());
    }
}