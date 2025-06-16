package kgg.translator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormat {
    public static final Map<String, Pattern> formatMap = new HashMap<>();
    private static String currentFormat = "none";

    public static void clear() {
        formatMap.clear();
    }

    public static void load(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        object.entrySet().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            formatMap.put(key, Pattern.compile(value));
        });
    }

    public static Pattern getPattern(String format) {
        return formatMap.getOrDefault(format, Pattern.compile(""));
    }

    public static Set<String> listFormats() {
        return formatMap.keySet();
    }

    public static String match(String text) {
        Pattern pattern = getPattern(currentFormat);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : text;
    }

    public static String getCurrentFormat() {
        return currentFormat;
    }

    public static void setCurrentFormat(String currentFormat) {
        ChatFormat.currentFormat = currentFormat;
    }
}