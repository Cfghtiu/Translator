package kgg.translator;

import kgg.translator.command.TranslateCommand;
import kgg.translator.command.TranslateConfigCommand;
import kgg.translator.handler.KeyBindingHandler;
import kgg.translator.translator.BaiduTranslatorImpl;
import kgg.translator.translator.BaiduTranslatorModMenuImpl;
import kgg.translator.translator.YouDaoTranslatorImpl;
import kgg.translator.translator.YouDaoTranslatorModMenuImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public class TranslatorMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            TranslateCommand.register(dispatcher);
            TranslateConfigCommand.register(dispatcher);
        });

        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            TranslatorManager.addTranslator(new BaiduTranslatorModMenuImpl());
            TranslatorManager.addTranslator(new YouDaoTranslatorModMenuImpl());
        } else {
            TranslatorManager.addTranslator(new BaiduTranslatorImpl());
            TranslatorManager.addTranslator(new YouDaoTranslatorImpl());
        }

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            TranslatorConfig.readFile();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            TranslatorConfig.writeFile();
        });

        KeyBindingHandler.register();
    }
}
