package kgg.translator.translator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kgg.translator.LLMManager;
import kgg.translator.exception.TranslateException;
import kgg.translator.util.RequestUtil;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class LLMTranslator extends Translator {
    private static final Logger LOGGER = LogManager.getLogger(LLMTranslator.class);
    private static final Gson gson = new Gson();
    
    private final LLMManager.Model model;
    private long lastRequestTime = 0;
    private static final long REQUEST_INTERVAL = 500; // 限制请求频率，避免过快

    public LLMTranslator(LLMManager.Model model) {
        this.model = model;
    }

    @Override
    public boolean isConfigured() {
        return !Strings.isEmpty(model.apiKey) && 
               !Strings.isEmpty(model.model) && 
               !Strings.isEmpty(model.url) && 
               !Strings.isEmpty(model.name);
    }

    @Override
    public String translate(String text, String from, String to, String source) throws IOException {
        // 限制请求频率
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        if (timeSinceLastRequest < REQUEST_INTERVAL) {
            try {
                TimeUnit.MILLISECONDS.sleep(REQUEST_INTERVAL - timeSinceLastRequest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        try {
            // 构建提示词
            Map<String, String> map = new HashMap<>();
            map.put("version", MinecraftClient.getInstance().getGameVersion());
            map.put("source", source);
            map.put("to", to);
            map.put("from", from);
            map.put("text", text);
            
            StrSubstitutor strSubstitutor = new StrSubstitutor(map);
            String msg = strSubstitutor.replace(LLMManager.getPrompt());

            // 发送请求
            HttpClient client = RequestUtil.getClient();
            String body = buildBody(msg);
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(model.url))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body));
            
            // 添加认证头
            if (!model.apiKey.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + model.apiKey);
            }
            
            HttpRequest request = requestBuilder.build();
            
            LOGGER.debug("发送LLM翻译请求: {} -> {}", from, to);
            
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                throw new IOException("请求被中断", e);
            }
            
            // 检查响应状态
            if (response.statusCode() != 200) {
                StringBuilder sb = new StringBuilder();
                sb.append("LLM API请求失败！\n");
                sb.append("状态码: ").append(response.statusCode()).append("\n");
                sb.append("响应头:\n");
                response.headers().map().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
                sb.append("响应体:\n").append(response.body());
            
                LOGGER.error(sb.toString());
                throw new TranslateException("API请求失败: HTTP " + response.statusCode() + response.body());
            }

            
            String resp = response.body();
            String translatedText = parseResponse(resp);
            
            if (translatedText.isEmpty()) {
                LOGGER.warn("LLM返回空翻译结果，返回原文");
                return text;
            }
            
            LOGGER.debug("LLM翻译成功: {} -> {}", text, translatedText);
            return translatedText;
            
        } finally {
            lastRequestTime = System.currentTimeMillis();
        }
    }

    /**
     * 解析API响应
     */
    private String parseResponse(String body) throws TranslateException {
        if (!body.trim().startsWith("{")) {
            throw new TranslateException("返回内容不是JSON: " + body);
        }
        try {
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            
            // 检查是否有错误
            if (object.has("error")) {
                JsonObject error = object.getAsJsonObject("error");
                String errorMessage = error.has("message") ? 
                    error.get("message").getAsString() : 
                    "未知错误";
                throw new TranslateException("API错误: " + errorMessage);
            }
            
            // 获取choices数组
            if (!object.has("choices")) {
                throw new TranslateException("响应格式错误: 缺少choices字段");
            }
            
            JsonArray choices = object.getAsJsonArray("choices");
            if (choices.size() == 0) {
                throw new TranslateException("响应格式错误: choices数组为空");
            }
            
            // 获取第一个choice的message
            JsonObject choice = choices.get(0).getAsJsonObject();
            if (!choice.has("message")) {
                throw new TranslateException("响应格式错误: 缺少message字段");
            }
            
            JsonObject message = choice.getAsJsonObject("message");
            
            // 首先尝试获取content字段（标准响应）
            if (message.has("content") && !message.get("content").isJsonNull()) {
                String content = message.get("content").getAsString().trim();
                if (!content.isEmpty()) {
                    return content;
                }
            }
            
            // 如果content为空，尝试解析tool_calls（某些模型可能使用工具调用）
            if (message.has("tool_calls") && message.get("tool_calls").isJsonArray()) {
                JsonArray toolCalls = message.getAsJsonArray("tool_calls");
                if (toolCalls.size() > 0) {
                    JsonObject toolCall = toolCalls.get(0).getAsJsonObject();
                    if (toolCall.has("function")) {
                        JsonObject function = toolCall.getAsJsonObject("function");
                        if (function.has("arguments")) {
                            String arguments = function.get("arguments").getAsString();
                            try {
                                JsonObject argsJson = JsonParser.parseString(arguments).getAsJsonObject();
                                if (argsJson.has("result")) {
                                    return argsJson.get("result").getAsString();
                                }
                            } catch (Exception e) {
                                LOGGER.warn("解析tool_calls参数失败: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
            
            throw new TranslateException("无法从响应中提取翻译结果");
            
        } catch (Exception e) {
            if (e instanceof TranslateException) {
                throw (TranslateException) e;
            }
            LOGGER.error("解析LLM响应失败: {}", body, e);
            throw new TranslateException("解析响应失败: " + e.getMessage());
        }
    }

    /**
     * 构建请求体
     */
    private String buildBody(String msg) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model.model);
        
        // 添加消息
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", msg);
        messages.add(userMessage);
        requestBody.add("messages", messages);
        
        // 添加一些常用参数
        requestBody.addProperty("temperature", 0.3); // 随机性
        requestBody.addProperty("max_tokens", 1000); // 响应长度
        
        // 某些API可能需要stream参数
        requestBody.addProperty("stream", false);
        
        return gson.toJson(requestBody);
    }

    @Override
    public String getName() {
        return model.name;
    }

    @Override
    public String getLanguageType() {
        return "OpenAI";
    }

    @Override
    public void read(JsonObject object) {
        // LLM配置通过LLMManager管理，这里不需要额外读取
    }

    @Override
    public void write(JsonObject object) {
        // LLM配置通过LLMManager管理，这里不需要额外写入
    }
}