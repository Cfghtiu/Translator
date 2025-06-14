package kgg.translator;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Language {
    public static final Map<String, Predicate<String>> predicateMap = new HashMap<>();
    public static final Map<String, String> defaultMap = new HashMap<>();
    public static final Map<String, Map<String, String>> translatorMap = new HashMap<>();

    public static void load(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        Optional.of(object.get("default")).ifPresent(o -> o.getAsJsonObject().entrySet().forEach(entry -> {
            defaultMap.put(entry.getKey(), entry.getValue().getAsString());
        }));

        Optional.of(object.get("regex")).ifPresent(o -> o.getAsJsonObject().entrySet().forEach(entry -> {
            Pattern pattern = Pattern.compile(entry.getValue().getAsString());
            setPredicate(entry.getKey(), s -> pattern.matcher(s).find());
        }));
        Optional.of(object.get("translator")).ifPresent(o -> o.getAsJsonObject().entrySet().forEach(entry -> {
            Map<String, String> map = translatorMap.getOrDefault(entry.getKey(), new HashMap<>());
            entry.getValue().getAsJsonObject().entrySet().forEach(e1 -> {
                map.put(e1.getKey(), e1.getValue().getAsString());
            });
            translatorMap.put(entry.getKey(), map);
        }));
    }

    public static Set<String> getTranslatorSupport(String translator) {
        Map<String, String> m = translatorMap.get(translator);
        if (m == null) return defaultMap.keySet();

        Set<String> set = new HashSet<>();
        set.addAll(defaultMap.keySet());
        set.addAll(m.keySet());
        return set;
    }

    public static String getLeftLang(String translator, String rightLang) {
        Map<String, String> m;
        Map<String, String> m2 = translatorMap.get(translator);
        if (m2 == null) {
            m = defaultMap;
        } else {
            m = new HashMap<>(defaultMap);
            m.putAll(m2);
        }
        for (Map.Entry<String, String> entry : m.entrySet()) {
            if (entry.getValue().equals(rightLang)) return entry.getKey();
        }
        return null;
    }

    public static String getRightLang(String translator, String leftLang) {
        Map<String, String> m;
        Map<String, String> m2 = translatorMap.get(translator);
        if (m2 == null) {
            m = defaultMap;
        } else {
            m = new HashMap<>(defaultMap);
            m.putAll(m2);
        }
        return m.getOrDefault(leftLang, null);
    }

    public static void setPredicate(String lang, Predicate<String> predicate) {
        predicateMap.put(lang, predicate);
    }

    public static Predicate<String> getPredicate(String lang) {
        return predicateMap.getOrDefault(lang, s -> false);
    }
}
