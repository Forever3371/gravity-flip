package gravity_flip.mixin;

import gravity_flip.GravityFlipTrigger;
import net.minecraft.advancements.AdvancementHolder;
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
 * {@code PlayerAdvancementTracker.grantCriterion}）——一个成就有多个 criterion，
 * 这个方法会被调用多次，所以必须再判断"整个成就完成了没有"，
 * 否则一个成就可能连着触发好几次翻转。
 * <p>
 * 另外：玩家上线时进度是通过私有的 {@code loadProgressMap} 直接塞进去的（不经过这里），
 * 所以不用担心"每次登录都误触发一次翻转"。这一点是查过字节码确认的。
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
        if (!self.getOrStartProgress(advancement).isDone()) {
            return; // 整个成就还没完成
        }

        // 取成就的显示名（比如"获得木头"），取不到就退回资源 id。
        // 直接用 id.getPath() 会变成 "story/root" 这种玩家看不懂的东西。
        net.minecraft.network.chat.Component title = advancement.value().display()
                .map(net.minecraft.advancements.DisplayInfo::getTitle)
                .orElse(net.minecraft.network.chat.Component.literal(advancement.id().toString()));

        GravityFlipTrigger.onAdvancementCompleted(this.player, advancement.id(), title);
    }
}
