package kgg.translator.translator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kgg.translator.exception.ErrorCodeException;
import kgg.translator.translator.Translator;
import kgg.translator.util.RequestUtil;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BingTranslator extends Translator {
    public static final String INFO_URL = "https://cn.bing.com/search?q=translate";
    public static final String TRANSLATE_URL = "https://cn.bing.com/ttranslatev3?&IG=%s&IID=%s";

    public static final Pattern IGPattern = Pattern.compile("_IG=\"(.*?)\"");
    public static final Pattern IIDPattern = Pattern.compile("_iid=\"(.*?)\"");
    public static final Pattern tokenDataPattern = Pattern.compile("params_AbusePreventionHelper = \\[(.*?)\\];");
    public HashMap<String, String> cookies = new HashMap<>();

    public static final String ignores = " \n\t\r";

    private String IG;
    private String IID;
    private String key;
    private String token;
    private int maxAge;
    private long updateTime;

    private static boolean updated = false;

    @Override
    public synchronized String translate(String text, String from, String to) throws IOException {
        return delay(0, ()->{
            if (isOverAge()) update();
            boolean ignore = true;
            for (char c : text.toCharArray()) {
                if (!ignores.contains(String.valueOf(c))){
                    ignore = false;
                    break;
                }
            }
            if (ignore) return text;
            HashMap<String, Object> translateData = new HashMap<>();
            translateData.put("fromLang", from);
            translateData.put("text", text);
            translateData.put("to", to);
            translateData.put("token", token);
            translateData.put("key", key);
            translateData.put("tryFetchingGenderDebiasedTranslations", "true");

            String requestBody = HttpAuthenticationService.buildQuery(translateData);
            URI uri = URI.create(TRANSLATE_URL.formatted(IG, IID));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .header("Cookie",
                    cookies.entrySet().stream().map(
                        entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("; "))
                )
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
            HttpRequest request = requestBuilder.build();

            String response = RequestUtil.send(request, HttpResponse.BodyHandlers.ofString()).body();
            JsonElement result = JsonParser.parseString(response);

            checkSuccess(result);

            return result.getAsJsonArray().get(0).getAsJsonObject()
                .getAsJsonArray("translations").get(0).getAsJsonObject()
                .get("text").getAsString();
        });
    }

    private void checkSuccess(JsonElement result) throws ErrorCodeException {
        JsonElement code;
        if (result.isJsonObject() && (code=result.getAsJsonObject().get("statusCode")).isJsonNull()){
            throw new ErrorCodeException("bing", code.getAsString());
        }
    }

    private boolean isOverAge() {
        if (!updated) return true;
        return System.currentTimeMillis() - updateTime > maxAge;
    }

    public void update() throws IOException {
        cookies.put("MUID", generateRandomToken());
        String infoPage = getInfoPage(cookies);

        Matcher IGMatcher = IGPattern.matcher(infoPage);
        Matcher IIDMatcher = IIDPattern.matcher(infoPage);
        Matcher tokenDataMatcher = tokenDataPattern.matcher(infoPage);

        if (IGMatcher.find() && IIDMatcher.find() && tokenDataMatcher.find()){
            IG = IGMatcher.group(0).substring(5, IGMatcher.group(0).length() - 2);
            IID = IIDMatcher.group(0).substring(6, IIDMatcher.group(0).length() - 2);

            String tokenDataString = tokenDataMatcher.group(0);
            tokenDataString = tokenDataString.substring(32, tokenDataString.length() - 2);
            String[] tokenData = tokenDataString.split(",");
            key = tokenData[0];
            token = tokenData[1].substring(1, tokenData[1].length() - 1);
            maxAge = Integer.parseInt(tokenData[2]);
            updateTime = System.currentTimeMillis();
            updated = true;
        }
        setConfigured();
    }

    private String getInfoPage(HashMap<String, String> cookies) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(INFO_URL)).header("Cookie",
            cookies.entrySet().stream().map(
                entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("; "))
        ).build();
        return RequestUtil.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String generateRandomToken() {
        Random random = new Random();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return "Bing翻译";
    }

    @Override
    public void read(JsonObject jsonObject) {
    }

    @Override
    public void write(JsonObject jsonObject) {
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> literalArgumentBuilder) {
    }
}
