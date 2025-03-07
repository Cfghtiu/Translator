package kgg.translator.translator;

import kgg.translator.modmenu.ModMenuConfigurable;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.text.Text;

public class LLMTranslatorModMenuImpl extends LLMTranslatorImpl implements ModMenuConfigurable {
    public LLMTranslatorModMenuImpl(String prompt, String name, String url) {
        super(prompt, name, url);
    }

    @Override
    public Runnable registerEntry(ConfigEntryBuilder entryBuilder, SubCategoryBuilder category) {
        StringListEntry apiKeyEntry = entryBuilder.startStrField(Text.literal("ApiKey"), this.apiKey).build();
        StringListEntry modelEntry = entryBuilder.startStrField(Text.literal("Model"), this.model).build();
        category.add(apiKeyEntry);
        category.add(modelEntry);
        return () -> {
            this.setConfig(apiKeyEntry.getValue(), modelEntry.getValue());
        };
    }
}
