package kgg.translator.option;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class OptionRegistry {
    private static final Gson gson = new Gson();
    public static final Map<String, SimpleOption<?>> options = new HashMap<>();

    public static <T> void readJsonElement(SimpleOption<T> option, JsonElement element) {
        Type type = new TypeToken<T>(){}.getType();
        Object value = gson.fromJson(element, type);

        if (value instanceof Double && option.getValue() instanceof Integer) {  // 不然会报错，无奈之举
            value = ((Double) value).intValue();
        }

        option.setValue((T) value);
    }

    public static JsonElement createJsonElement(SimpleOption<?> option) {
        return gson.toJsonTree(option.getValue());
    }

    public static <T> SimpleOption<T> register(String name, SimpleOption<T> option){
        options.put(name, option);
        return option;
    }

    public static SimpleOption<Boolean> registerBool(String name, boolean defaultValue) {
        return register(name, SimpleOption.ofBoolean(
                "translator.option." + name,
                SimpleOption.emptyTooltip(),
                defaultValue,
                v -> {}
        ));
    }

    public static SimpleOption<Integer> registerInt(String name, int defaultValue, int minValue, int maxValue) {
        return register(name, new SimpleOption<>(
                "translator.option."+name,
                SimpleOption.emptyTooltip(),
                (p ,v) -> Text.translatable("options.percent_value", p, v),
                new SimpleOption.ValidatingIntSliderCallbacks(minValue, maxValue, false),
                defaultValue,
                v-> {}
        ));
    }

    public static SimpleOption<Boolean> registerBoolWithTooltip(String name, boolean defaultValue) {
        return register(name, SimpleOption.ofBoolean(
                "translator.option." + name,
                SimpleOption.constantTooltip(Text.translatable("translator.option." + name + ".desc")),
                defaultValue,
                v -> {}
        ));
    }

    public static SimpleOption<Integer> registerIntWithTooltip(String name, int defaultValue, int minValue, int maxValue, String textTranslationKey) {
        return register(name, new SimpleOption<>(
                "translator.option."+name,
                SimpleOption.constantTooltip(Text.translatable("translator.option." + name + ".desc")),
                (p ,v) -> Text.translatable(textTranslationKey, p, v),
                new SimpleOption.ValidatingIntSliderCallbacks(minValue, maxValue, false),
                defaultValue,
                v-> {}
        ));
    }


}
