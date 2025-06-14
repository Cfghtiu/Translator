package kgg.translator.translator;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class BaiduTranslatorImpl extends BaiduTranslator {
    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> node) {
        node.then(ClientCommandManager.argument("qps", IntegerArgumentType.integer(0))
                .executes(
                    context -> {
                        setDelayTime(1000 / IntegerArgumentType.getInteger(context, "qps"));
                        context.getSource().sendFeedback(Text.literal("OK"));
                        return 0;
                    })
                .then(ClientCommandManager.argument("appId", StringArgumentType.word())
                        .then(ClientCommandManager.argument("appKey", StringArgumentType.word()).executes(context -> {
                            setDelayTime(1000 / IntegerArgumentType.getInteger(context, "qps"));
                            setConfig(StringArgumentType.getString(context, "appId"), StringArgumentType.getString(context, "appKey"));
                            context.getSource().sendFeedback(Text.literal("OK"));
                            return 0;
                        }))));
    }
}
