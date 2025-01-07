package kgg.translator.screen;

import kgg.translator.event.OptionsScreenAddBodyEvent;
import kgg.translator.option.Options;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.text.Text;

public class OptionsScreen extends GameOptionsScreen {
    public OptionsScreen() {
        super(null, MinecraftClient.getInstance().options, Text.translatable("translator.optionscreen.title"));
    }

    @Override
    protected void addOptions() {
        this.body.addAll(
                Options.autoChat, Options.chatTip,
                Options.autoTooltip, Options.autoScoreboard,
                Options.autoBossBar, Options.autoTitle,
                Options.autoSign, Options.signCombine,
                Options.autoEntityName, Options.distance,
                Options.autoPlayerName
        );
        OptionsScreenAddBodyEvent.EVENT.invoker().add(this.body);
    }
}
