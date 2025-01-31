package kgg.translator;

import kgg.translator.event.SetTranslatorEvent;
import kgg.translator.translator.Translator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

public class TranslatorManager {
    private static final Logger LOGGER = LogManager.getLogger(TranslatorManager.class);
    private static Translator current;
    private static final List<Translator> translators = new LinkedList<>();

    private static String from = "auto";
    private static String to = "zh-cn";

    public static Translator getCurrent() {
        return current;
    }

    public static boolean setTranslator(Translator translator) {
        LOGGER.info("Set current translator to {}", translator);
        if (current != translator) {
            SetTranslatorEvent.invoke(translator);
            if (current != null && Language.translatorMap.containsKey(current.getName()) && Language.translatorMap.containsKey(translator.getName())) {
                String leftFrom = Language.getLeftLang(current.getName(), from);
                String leftTo = Language.getLeftLang(current.getName(), to);
                if (leftFrom != null && leftTo != null) {
                    String rightFrom = Language.translatorMap.get(translator.getName()).get(leftFrom);
                    String rightTo = Language.translatorMap.get(translator.getName()).get(leftTo);
                    if (rightFrom != null && rightTo != null) {
                        setFrom(rightFrom);
                        setTo(rightTo);
                        TranslatorManager.current = translator;
                        return true;
                    }
                }
            }
            TranslatorManager.current = translator;
            return false;
        }
        return true;
    }


    public static void addTranslator(Translator translator) {
        LOGGER.info("Add translator {}", translator);
        if (translators.isEmpty()) {
            setTranslator(translator);
        } else {
            if (!getCurrent().isConfigured() && translator.isConfigured()) {
                setTranslator(translator);
            }
        }
        translators.add(translator);
    }

    public static String getTo() {
        return to;
    }

    public static void setTo(String defaultTo) {
        TranslatorManager.to = defaultTo;
    }

    public static String getFrom() {
        return from;
    }

    public static void setFrom(String defaultFrom) {
        TranslatorManager.from = defaultFrom;
    }

    public static List<Translator> getTranslators() {
        return translators;
    }
}
