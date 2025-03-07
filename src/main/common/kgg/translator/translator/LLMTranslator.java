package kgg.translator.translator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kgg.translator.translator.Translator;
import kgg.translator.util.RequestUtil;
import net.minecraft.client.MinecraftClient;
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
    private String prompt;

    public LLMTranslator(String prompt, String name, String url) {
        this.prompt = prompt;
        this.name = name;
        this.url = url;
    }


    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
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
        String msg = strSubstitutor.replace(prompt);

        HttpClient client = RequestUtil.getClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(buildBody(msg))).build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        String body = response.body();
        return readBody(body);
    }

    private String readBody(String body) {
        JsonObject object = JsonParser.parseString(body).getAsJsonObject();
        // {"choices":[{"message":{"content":"","role":"assistant","tool_calls":[{"function":{"name":"result","arguments":"{\"result\":\"25\"}"},"index":0,"id":"","type":"function"}]}...
        String json = object
            .getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .getAsJsonArray("tool_calls")
            .get(0).getAsJsonObject()
            .getAsJsonObject("function")
            .get("arguments").getAsString();
        return JsonParser.parseString(json).getAsJsonObject().get("result").getAsString();
    }

    private String buildBody(String msg) {
        return """
            {
              "model": "%s",
              "messages": [
                {
                  "role": "user",
                  "content": "%s"
                }
              ],
              "tools": [
                {
                  "type": "function",
                  "function": {
                    "name": "result",
                    "description": "set result",
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
            """.formatted(model, msg.replace("\\", "\\\\").replace("\"", "\\\""));
    }

    @Override
    public String getName() {
        return name;
    }

    public void setConfig(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
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
}
