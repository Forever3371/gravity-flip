package gravity_flip.mixin;

import gravity_flip.GravityFlipTrigger;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 成就触发点。
 * <p>
 * 注入 {@code PlayerAdvancements.award}（yarn 里的名字是
 * {@code PlayerAdvancementTracker.grantCriterion}）。
 * <p>
 * <b>每完成一个 criterion 就触发一次</b>，不等整个成就做完——
 * 所以"吃一个苹果"（均衡饮食的一部分）、"走进一个新群系"（探索的时光的一部分）
 * 都会各自触发一次重力翻转。
 * <p>
 * 另外：玩家上线时进度是通过私有的 {@code loadProgressMap} 直接塞进去的（不经过这里），
 * 所以不用担心"每次登录都误触发"。这一点是查过字节码确认的。
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void gravityFlip$onCriterionAwarded(AdvancementHolder advancement, String criterion,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return; // 这次没有新授予任何 criterion
        }
        if (this.player == null) {
            return;
        }

        PlayerAdvancements self = (PlayerAdvancements) (Object) this;
        AdvancementProgress progress = self.getOrStartProgress(advancement);

        // 取成就的显示名（比如"均衡饮食"），取不到就退回资源 id。
        Component title = advancement.value().display()
                .map(DisplayInfo::getTitle)
                .orElse(Component.literal(advancement.id().toString()));

        // 原版自带的进度文本（形如 "3/40"），比自己数更省事，而且会跟着语言变
        Component progressText = progress.getProgressText();

        GravityFlipTrigger.onCriterionCompleted(
                this.player, advancement.id(), criterion, title, progressText, progress.isDone());
    }
}
