package kgg.translator.option;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import static kgg.translator.option.OptionRegistry.*;

public class Options {
    public static void init() {}

    // 聊天栏
    public static final SimpleOption<Boolean> chatTip = registerBool("chat_tip", false);
    public static final SimpleOption<Boolean> autoChat = registerBool("auto_chat", false);
    // 界面
    public static final SimpleOption<Boolean> autoTooltip = registerBoolWithTooltip("auto_tooltip", false);
    public static final SimpleOption<Boolean> autoTitle = registerBool("auto_title", false);
    public static final SimpleOption<Boolean> autoScoreboard = registerBool("auto_scoreboard", false);
    public static final SimpleOption<Boolean> autoBossBar = registerBool("auto_boss_bar", false);
    // 世界
    public static final SimpleOption<Integer> distance = register("distance", new SimpleOption<>(
            "translator.option.distance",
            SimpleOption.constantTooltip(Text.translatable("translator.option.distance.desc")),
            (p ,v) -> {
                if (v == 100) {
                    return Text.translatable("options.value.max", p, v);
                }
                return Text.translatable("options.value.block", p, v);
            },
            new SimpleOption.ValidatingIntSliderCallbacks(0, 100, true),
            30,
            v-> {}
    ));

    public static boolean inRange(Vec3d pos) {
        if (distance.getValue() == 100) {
            return true;
        }
        return MinecraftClient.getInstance().cameraEntity.getPos().distanceTo(pos) <= distance.getValue();
    }

    public static final SimpleOption<Boolean> autoEntityName = registerBool("auto_entity_name", false);
    public static final SimpleOption<Boolean> autoPlayerName = registerBool("auto_player_name", false);
    public static final SimpleOption<Boolean> autoSign = registerBool("auto_sign", false);
    public static final SimpleOption<Boolean> signCombine = registerBoolWithTooltip("sign_combine", true);

    // 内置
    // 标记翻译源，用于让llm翻译地更准确
    public static SimpleOption<Boolean> markSources = registerBoolWithTooltip("mark_sources", false);
    // 多方翻译，允许交替使用配置好的翻译器，加快翻译速度
    // todo 还没做
    public static SimpleOption<Boolean> multiTranslation = registerBoolWithTooltip("multi-translation", false);
}
