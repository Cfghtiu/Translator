package kgg.translator.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import kgg.translator.TranslatorConfig;
import kgg.translator.modmenu.ModMenuApiImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class ConfigJsonScreen extends Screen {
    private final TextFieldWidget configFieldWidget;
    private boolean needLoad = false;

    public ConfigJsonScreen() {
        super(Text.translatable("translator.configscreen.title"));
        configFieldWidget = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 200, 20, Text.translatable("translator.configscreen.edit"));
        configFieldWidget.setMaxLength(99999);
        load();
        configFieldWidget.setEditable(true);
    }

    private void load() {
        JsonObject object = new JsonObject();
        TranslatorConfig.writeConfig(object);
        String txt = object.toString();
        configFieldWidget.setText(txt);
    }

    @Override
    protected void init() {
        if (needLoad) {
            load();
            needLoad = false;
        }
        configFieldWidget.setPosition(this.width / 2 - 100, this.height / 4 + 30);
        addDrawableChild(configFieldWidget);
        addDrawableChild(ButtonWidget.builder(Text.translatable("translator.configscreen.save"), e -> save()).dimensions(this.width / 2 - 100, this.height / 4 + 96, 200, 20).build());
        ButtonWidget btn = addDrawableChild(ButtonWidget.builder(Text.translatable("translator.configscreen.modmenu"), e -> {
            needLoad = true;
            client.setScreen(ModMenuApiImpl.createScreen(this));
        })
            .dimensions(this.width / 2 - 100, this.height / 4 + 118, 200, 20).build());
        btn.active = FabricLoader.getInstance().isModLoaded("cloth-config");
    }

    private void save() {
        String str = configFieldWidget.getText();
        ClientPlayerEntity player = client.player;
        try {
            JsonObject object = JsonParser.parseString(str).getAsJsonObject();
            boolean read = TranslatorConfig.readConfig(object);
            if (player != null) {
                if (read) {
                    player.sendMessage(Text.literal("OK"), false);
                } else {
                    player.sendMessage(Text.literal("Failed to load config"), false);
                }
            }
        } catch (JsonSyntaxException e) {
            if (player != null) {
                player.sendMessage(Text.literal("Invalid json"), false);
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 16777215);
    }
}
