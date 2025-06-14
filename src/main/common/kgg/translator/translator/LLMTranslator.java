package kgg.translator.translator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kgg.translator.LLMManager;
import kgg.translator.exception.TranslateException;
import kgg.translator.util.RequestUtil;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public abstract class LLMTranslator extends Translator {
    protected final String name;
    protected final String url;

    protected String apiKey = "";
    protected String model = "";

    public String getModel() {
        return model;
    }

    public LLMTranslator(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public String translate(String text, String from, String to, String source) throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("version", MinecraftClient.getInstance().getGameVersion());
        map.put("source", source);
        map.put("to", to);
        map.put("from", from);
        map.put("text", text);
        StrSubstitutor strSubstitutor = new StrSubstitutor(map);
        String msg = strSubstitutor.replace(LLMManager.getPrompt());

        HttpClient client = RequestUtil.getClient();
        String body = buildBody(msg);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        String resp = response.body();
        return readBody(resp);
    }

    private String readBody(String body) throws TranslateException {
        JsonObject object = JsonParser.parseString(body).getAsJsonObject();
        // {'error': {'message': 'The model `qwen2.5-1.5b-instrct` does not exist or you do not have access to it.', 'type': 'invalid_request_error', 'param': None, '...
        // {"choices":[{"message":{"content":"","role":"assistant","tool_calls":[{"function":{"name":"result","arguments":"{\"result\":\"25\"}"},"index":0,"id":"","type":"function"}]}...
        if (object.has("error")) {
            throw new TranslateException(object.getAsJsonObject("error").get("message").getAsString());
        }
        JsonArray array = object
            .getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .getAsJsonArray("tool_calls");
        if (array.isEmpty()) {

            throw new TranslateException("翻译失败", new TranslateException(body));
        }

        String json = array
            .get(0).getAsJsonObject()
            .getAsJsonObject("function")
            .get("arguments").getAsString();
        return JsonParser.parseString(json).getAsJsonObject().get("result").getAsString();
    }

    private static final Gson gson = new Gson();
    private String buildBody(String msg) {
        return """
            {
              "model": "%s",
              "messages": [
                {
                  "role": "user",
                  "content": %s
                }
              ],
              "tools": [
                {
                  "type": "function",
                  "function": {
                    "name": "result",
                    "description": "set translate result",
                    "parameters": {
                      "type": "object",
                      "properties": {
                        "result": {
                          "type": "string",
                          "description": ""
                        }
                      },
                      "required": ["result"]
                    }
                  }
                }
              ],
              "tool_choice": {
                "type": "function",
                "function": {
                  "name": "result"
                }
              }
            }
            """.formatted(model, gson.toJson(msg));
    }

    @Override
    public String getName() {
        return name;
    }

    public void setConfig(String apiKey, String model) {
        if (!StringUtils.isBlank(apiKey)) {
            this.apiKey = apiKey;
        }
        if (!StringUtils.isBlank(model)) {
            this.model = model;
        }
        if (!StringUtils.isBlank(apiKey) && !StringUtils.isBlank(model)) {
            setConfigured();
        }
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void read(JsonObject jsonObject) {
        setConfig(jsonObject.get("apiKey").getAsString(), jsonObject.get("model").getAsString());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty("apiKey", apiKey);
        jsonObject.addProperty("model", model);
    }

    @Override
    public String getLanguageType() {
        return "AI翻译";
    }
}
