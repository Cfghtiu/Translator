package kgg.translator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import kgg.translator.translator.LLMTranslator;
import kgg.translator.translator.LLMTranslatorImpl;

import java.util.HashMap;
import java.util.Map;

public class LLMManager {
    private static String prompt;
    private static final Gson gson = new Gson();
    private static final Map<String, Model> models = new HashMap<>();

    public static void writeConfig(JsonObject object) {
        object.add("models", gson.toJsonTree(models, new TypeToken<Map<String, Model>>(){}.getType()));
    }

    public static void readConfig(JsonObject object) {
        models.clear();
        TranslatorManager.getTranslators().removeIf(translator -> translator instanceof LLMTranslator);
        Map<String, Model> load = gson.fromJson(object.getAsJsonObject("models"), new TypeToken<Map<String, Model>>(){}.getType());
        if (load == null) {
            addBuiltInModels();
        } else {
            load.forEach((name, model) -> addModel(model));
        }
        prompt = TranslatorConfig.read("prompt.txt");
    }

    public static String getPrompt() {
        return prompt;
    }

    public static Map<String, Model> getModels() {
        return models;
    }

    private static void addBuiltInModels() {
        for (Model model : geBuiltInModels()) {
            addModel(model);
        }
    }

    public static Model[] geBuiltInModels() {
        return new Model[] {
            new Model("OpenAI兼容接口", "https://api.openai.com/v1/chat/completions", "", "")
        };
    }

    public static void addModel(Model model) {
        // 去除结尾的/
        Model newModel = new Model(model.name, model.url.endsWith("/") ? model.url.substring(0, model.url.length() - 1) : model.url, model.model, model.apiKey);

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
        translator = new LLMTranslatorImpl(model);
        TranslatorManager.addTranslator(translator);
    }

    public static class Model {
        public String name;
        public String url;
        public String model;
        public String apiKey;

        public Model(String name, String url, String model, String apiKey) {
            this.name = name == null ? "" : name;
            this.url = url == null ? "" : url;
            this.model = model == null ? "" : model;
            this.apiKey = apiKey == null ? "" : apiKey;
        }
    }
    public static void setModels(Map<String, Model> newModels) {
        models.clear();
        models.putAll(newModels);
    }
}
