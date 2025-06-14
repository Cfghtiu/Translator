package kgg.translator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import kgg.translator.translator.LLMTranslator;
import kgg.translator.translator.LLMTranslatorImpl;
import kgg.translator.translator.LLMTranslatorModMenuImpl;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LLMManager {
    private static String prompt;
    public record Model(String name, String url, String defaultModel) { }
    private static final Gson gson = new Gson();
    private static final Map<String, Model> models = new HashMap<>();

    public static Map<String, Model> getModels() {
        return models;
    }

    public static String getPrompt() {
        return prompt;
    }

    public static void writeConfig(JsonObject object) {
        object.add("models", gson.toJsonTree(models, new TypeToken<Map<String, Model>>(){}.getType()));
    }

    public static void readConfig(JsonObject object) {
        Map<String, Model> load = gson.fromJson(object.getAsJsonObject("models"), new TypeToken<Map<String, Model>>(){}.getType());
        if (load == null) {
            addBuiltInModels();
        } else {
            models.putAll(load);
        }
        models.forEach((name, model) -> addLLMTranslator(model));
    }

    private static void addBuiltInModels() {
        models.put("KIMI", new Model("KIMI", "https://api.moonshot.cn/v1", "moonshot-v1-8k"));
        models.put("质谱", new Model("质谱", "https://open.bigmodel.cn/api/paas/v4", "GLM-4-Flash"));
        models.put("ChatGPT", new Model("ChatGPT", "https://api.openai.com/v1/completions", "gpt-3.5-turbo"));
    }

    public static void init() {
        try {
            prompt = TranslatorConfig.read("prompt.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addModel(Model model) {
        // 去除结尾的/
        Model newModel = new Model(model.name, model.url.endsWith("/") ? model.url.substring(0, model.url.length() - 1) : model.url, model.defaultModel);

        Model old = models.put(model.name, newModel);  // 替换
        if (old != null) {
            TranslatorManager.getTranslators().removeIf(translator -> translator.getName().equals(old.name));
        }
        addLLMTranslator(newModel);
    }

    public static boolean removeModel(String name) {
        if (models.remove(name) != null) {
            TranslatorManager.getTranslators().removeIf(translator -> translator.getName().equals(name));
            return true;
        } else {
            return false;
        }
    }

    private static void addLLMTranslator(Model model) {
        LLMTranslator translator;
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            translator = new LLMTranslatorModMenuImpl(model.name, model.url);
        } else {
            translator = new LLMTranslatorImpl(model.name, model.url);
        }
        translator.setConfig("", model.defaultModel());
        TranslatorManager.addTranslator(translator);
    }
}
