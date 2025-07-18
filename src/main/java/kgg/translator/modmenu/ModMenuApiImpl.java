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
    // 用于跟踪是否需要重新创建配置屏幕
    private static boolean needsRefresh = false;
    private static Screen lastParentScreen = null;
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            lastParentScreen = parent;
            return createScreen(parent);
        };
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
        
        // 记录修改前的模型数量和名称
        Map<String, LLMManager.Model> originalModels = new HashMap<>(LLMManager.getModels());
        
        // LLM 模型配置
        category.addEntry(new NestedListListEntry<LLMManager.Model, MultiElementListEntry<LLMManager.Model>>(
            Text.literal("OpenAI-compatible API"),
            Lists.newArrayList(LLMManager.getModels().values()),
            true,
            Optional::empty,
            list -> {
                updateModels(list, originalModels);
            },
            () -> Arrays.stream(LLMManager.geBuiltInModels()).toList(),  // 默认值
            entryBuilder.getResetButtonKey(),
            true,
            true,
            (model, nestedListListEntry) -> {  // 创建子组件
                if (model == null) {
                    model = new LLMManager.Model("?", "?", "?", "?");
                }
                LLMManager.Model finalModel = model;
                String originalName = model.name;
                
                MultiElementListEntry<LLMManager.Model> entry = new MultiElementListEntry<>(Text.literal(model.name), model,
                    Lists.newArrayList(
                        entryBuilder.startStrField(Text.literal("Name"), model.name)
                            .setSaveConsumer(s -> {
                                finalModel.name = s;
                                // 名称改变时需要刷新
                                if (!originalName.equals(s)) {
                                    needsRefresh = true;
                                }
                            })
                            .build(),
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
            
            // 如果需要刷新，重新打开配置屏幕
            if (needsRefresh) {
                needsRefresh = false;
                if (lastParentScreen != null) {
                    // 使用 Minecraft 的方式在下一个 tick 重新打开屏幕
                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    client.execute(() -> client.setScreen(createScreen(lastParentScreen)));
                }
            }
        });
        
        return builder.build();
    }

    private static void updateModels(List<LLMManager.Model> list, Map<String, LLMManager.Model> originalModels) {
        // 找出被删除的模型
        Set<String> currentNames = list.stream().map(m -> m.name).collect(Collectors.toSet());
        Set<String> toRemove = new HashSet<>(originalModels.keySet());
        toRemove.removeAll(currentNames);
        
        // 删除不存在的模型
        for (String name : toRemove) {
            LLMManager.removeModel(name);
            needsRefresh = true;
        }
        
        // 添加新模型或更新现有模型
        for (LLMManager.Model model : list) {
            LLMManager.Model oldModel = originalModels.get(model.name);
            if (oldModel == null) {
                // 新模型
                LLMManager.addModel(model);
                needsRefresh = true;
            } else if (!model.name.equals(oldModel.name) || 
                       !model.url.equals(oldModel.url) || 
                       !model.model.equals(oldModel.model) || 
                       !model.apiKey.equals(oldModel.apiKey)) {
                // 模型有更新
                LLMManager.addModel(model);  // addModel 会自动替换
            }
        }
    
        // 检查当前翻译器是否是 LLM 且模型是否仍存在
        Translator current = TranslatorManager.getCurrent();
        if (current instanceof LLMTranslator llm) {
            String currentName = llm.getName();
            boolean exists = list.stream().anyMatch(m -> m.name.equals(currentName));
            if (!exists) {
                // 当前 LLM 模型被删除，尝试切换到第一个仍存在的 LLM 翻译器
                Optional<Translator> fallback = TranslatorManager.getTranslators().stream()
                    .filter(t -> t instanceof LLMTranslator)
                    .filter(t -> list.stream().anyMatch(m -> m.name.equals(t.getName())))
                    .findFirst();
            
                if (fallback.isPresent()) {
                    TranslatorManager.setTranslator(fallback.get());
                } else {
                    // 如果没有LLM翻译器，切换到第一个非LLM翻译器
                    TranslatorManager.getTranslators().stream()
                        .filter(t -> !(t instanceof LLMTranslator))
                        .findFirst()
                        .ifPresent(TranslatorManager::setTranslator);
                }
            }
        }
    }
}