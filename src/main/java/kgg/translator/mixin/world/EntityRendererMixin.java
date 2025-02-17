package kgg.translator.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import kgg.translator.handler.TranslateHelper;
import kgg.translator.option.Options;
import kgg.translator.translator.Source;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {
    /**
     * 修改传入的text值
     */
    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private Text getText(Text text, @Local(argsOnly = true) T entity) {
        if (!Options.autoEntityName.getValue()) return text;
        if (!Options.inRange(entity.getPos())) return text;

        if (entity instanceof PlayerEntity) {
            if (Options.autoPlayerName.getValue()) {
                return TranslateHelper.translateNoWait(text, Source.PLAYER_NAME);
            } else {
                return text;
            }
        }
        return TranslateHelper.translateNoWait(text, Source.ENTITY_NAME);
    }
}
