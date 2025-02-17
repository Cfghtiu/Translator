package kgg.translator.mixin.hud;

import kgg.translator.handler.TipHandler;
import kgg.translator.handler.TranslateHelper;
import kgg.translator.option.Options;
import kgg.translator.translator.Source;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Mixin(InGameHud.class)
public abstract class InGameHudForScoreboardMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Shadow @Final private static String SCOREBOARD_JOINER;

    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private static Comparator<ScoreboardEntry> SCOREBOARD_ENTRY_COMPARATOR;

    /**
     * @author KGG_Xing_Kong
     * @reason 翻译计分板
     * AI重写的
     */
    @Overwrite
    private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective) {
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);


        // Stream operations to create an array of SidebarEntry instances.
        List<TipHandler.SidebarEntry> sidebarEntries = scoreboard.getScoreboardEntries(objective).stream()
                .filter(score -> !score.hidden())
                .sorted(SCOREBOARD_ENTRY_COMPARATOR)
                .limit(15L)
                .map(scoreboardEntry -> {
                    Team team = scoreboard.getScoreHolderTeam(scoreboardEntry.owner());
                    Text text = scoreboardEntry.name();
                    MutableText text2 = Team.decorateName(team, text);
                    MutableText text3 = scoreboardEntry.formatted(numberFormat);

                    // 翻译
                    if (Options.autoScoreboard.getValue()) {
                        text2 = (MutableText) TranslateHelper.translateNoWait(text2, Source.SCOREBOARD);
                    }
//                    text3 = (MutableText) ScreenOptions.getTranslateText(text3);

                    int scoreWidth = this.getTextRenderer().getWidth(text3);
                    return new TipHandler.SidebarEntry(text2, text3, scoreWidth);
                })
                .toList();

        Text text = objective.getDisplayName();

        // 翻译
        if (Options.autoScoreboard.getValue()) {
            text = TranslateHelper.translateNoWait(text, Source.SCOREBOARD);
        }

        int textWidth = this.getTextRenderer().getWidth(text);
        int joinerWidth = this.getTextRenderer().getWidth(SCOREBOARD_JOINER);

        // Determine the maximum width required for rendering.
        int maxWidth = this.getTextRenderer().getWidth(text);
        for (TipHandler.SidebarEntry sidebarEntry : sidebarEntries) {
            maxWidth = Math.max(maxWidth, this.getTextRenderer().getWidth(sidebarEntry.name()) + (sidebarEntry.scoreWidth() > 0 ? joinerWidth + sidebarEntry.scoreWidth() : 0));
        }

        // Rendering logic
        Text finalText = text;
        int finalMaxWidth = maxWidth;
        context.draw(() ->{
            int entryCount = sidebarEntries.size();
            Objects.requireNonNull(getTextRenderer());
            int totalHeight = entryCount * this.getTextRenderer().fontHeight;
            int startY = context.getScaledWindowHeight() / 2 + totalHeight / 3;
            int paddingX = 3;
            int xStart = context.getScaledWindowWidth() - finalMaxWidth - paddingX;
            int xEnd = context.getScaledWindowWidth() - paddingX + 2;
            int bgColorLight = this.client.options.getTextBackgroundColor(0.3f);
            int bgColorDark = this.client.options.getTextBackgroundColor(0.4f);
            int topOffset = startY - entryCount * this.getTextRenderer().fontHeight;
            Objects.requireNonNull(getTextRenderer());
            context.fill(xStart - 2, topOffset - this.getTextRenderer().fontHeight - 1, xEnd, topOffset - 1, bgColorDark);
            context.fill(xStart - 2, topOffset - 1, xEnd, startY, bgColorLight);
            context.drawText(this.getTextRenderer(), finalText, xStart + finalMaxWidth / 2 - textWidth / 2, topOffset - this.getTextRenderer().fontHeight, Colors.WHITE, false);

            for (int i = 0; i < entryCount; ++i) {
                TipHandler.SidebarEntry sidebarEntry = sidebarEntries.get(i);
                int yPosition = startY - (entryCount - i) * this.getTextRenderer().fontHeight;
                context.drawText(this.getTextRenderer(), sidebarEntry.name(), xStart, yPosition, Colors.WHITE, false);
                context.drawText(this.getTextRenderer(), sidebarEntry.score(), xEnd - sidebarEntry.scoreWidth(), yPosition, Colors.WHITE, false);
            }
        });
    }
}
