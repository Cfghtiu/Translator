package kgg.translator.modmenu;

import com.google.common.collect.Lists;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import kgg.translator.LLMManager;
import kgg.translator.TranslatorConfig;
import kgg.translator.TranslatorManager;
import kgg.translator.translator.LLMTranslator;
import kgg.translator.translator.Translator;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuApiImpl::createScreen;
    }

    public static Screen createScreen(Screen p) {
        ConfigBuilder builder = ConfigBuilder.create().setTitle(Text.translatable("translator.modmenu.title")).setParentScreen(p);
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory category = builder.getOrCreateCategory(Text.translatable("translator.modmenu.title"));
        // 当前翻译器
        DropdownBoxEntry<Translator> listEntry = entryBuilder.startDropdownMenu(Text.translatable("translator.modmenu.current"),
                TranslatorManager.getCurrent(),
                s -> TranslatorManager.getTranslators().stream().filter(t -> t.getName().equals(s)).findFirst().orElse(null),
                t -> Text.of(t.getName())
            ).setSelections(TranslatorManager.getTranslators())
            .setSaveConsumer(TranslatorManager::setTranslator)
            .build();
        category.addEntry(listEntry);
        // From To
        category.addEntry(entryBuilder.startStrField(Text.translatable("translator.modmenu.from"), TranslatorManager.getFrom())
            .setSaveConsumer(TranslatorManager::setFrom)
            .setTooltip(Text.translatable("translator.modmenu.suggestion"))
            .build());
        category.addEntry(entryBuilder.startStrField(Text.translatable("translator.modmenu.to"), TranslatorManager.getTo())
            .setSaveConsumer(TranslatorManager::setTo)
            .setTooltip(Text.translatable("translator.modmenu.suggestion"))
            .build());
        // 翻译器配置

        // 普通翻译器
        List<Runnable> onSave = new ArrayList<>(TranslatorManager.getTranslators().size());
        for (Translator translator : TranslatorManager.getTranslators()) {
            if (translator instanceof ModMenuConfigurable configurable) {
                if (!(translator instanceof LLMTranslator)) {
                    // 为每个翻译器创建一个类别
                    SubCategoryBuilder tranCategory = entryBuilder.startSubCategory(Text.literal(translator.getName()));
                    onSave.add(configurable.registerEntry(entryBuilder, tranCategory));
                    category.addEntry(tranCategory.build());
                }
            }
        }
        // 他源码读的是真累啊
        category.addEntry(new NestedListListEntry<LLMManager.Model, MultiElementListEntry<LLMManager.Model>>(
            Text.literal("AI翻译"),
            Lists.newArrayList(LLMManager.getModels().values()),
            true,
            Optional::empty,
            ModMenuApiImpl::updateModels,
            () -> Arrays.stream(LLMManager.geBuiltInModels()).toList(),  // 默认值
            entryBuilder.getResetButtonKey(),
            true,
            true,
            (model, nestedListListEntry) -> {  // 创建子组件
                if (model == null) {
                    model = new LLMManager.Model("?", "?", "?", "?");
                }
                LLMManager.Model finalModel = model;
                MultiElementListEntry<LLMManager.Model> entry = new MultiElementListEntry<>(Text.literal(model.name), model,
                    Lists.newArrayList(
                        entryBuilder.startStrField(Text.literal("Name"), model.name).setSaveConsumer(s -> finalModel.name = s).build(),
                        entryBuilder.startStrField(Text.literal("Url"), model.url).setSaveConsumer(s -> finalModel.url = s).build(),
                        entryBuilder.startStrField(Text.literal("Model"), model.model).setSaveConsumer(s -> finalModel.model = s).build(),
                        entryBuilder.startStrField(Text.literal("APIKEY"), model.apiKey).setSaveConsumer(s -> finalModel.apiKey = s).build()
                    ),
                    true);
                return entry;
            }
        ));

        builder.setSavingRunnable(() -> {
            onSave.forEach(Runnable::run);
            TranslatorConfig.writeFile();
        });
        return builder.build();
    }

    private static void updateModels(List<LLMManager.Model> list) {
        Map<String, LLMManager.Model> models = new HashMap<>(LLMManager.getModels());
        // 删除不包含的东西
        models.forEach((name, model) -> {
            boolean contains = false;
            for (LLMManager.Model newModel : list) {
                if (newModel.name.equals(name)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                models.remove(name);
            }
        });
        // 有就更新，没有就添加
        for (LLMManager.Model model : list) {
            if (models.containsKey(model.name)) {
                LLMManager.Model old = models.get(model.name);
                old.url = model.url;
                old.model = model.model;
                old.apiKey = model.apiKey;
            } else {
                LLMManager.addModel(model);
            }
        }
    }
}
